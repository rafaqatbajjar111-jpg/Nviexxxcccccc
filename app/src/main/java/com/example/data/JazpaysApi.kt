package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

// ===============================================================
// 1. JAZPAYS SIGNATURE GENERATION
// ===============================================================
object JazpaysSigner {
    private const val MERCHANT_ID = "100222099"
    private const val API_KEY = "25aa23a6200008a506628fa5f971fc1d"

    fun generateSignature(
        amount: String,
        merchantOrderNo: String,
        callbackUrl: String
    ): String {
        // Collect parameters
        val params = sortedMapOf(
            "amount" to amount,
            "callback_url" to callbackUrl,
            "merchant_id" to MERCHANT_ID,
            "merchant_order_no" to merchantOrderNo
        )

        // Sort alphabetically by key (ascending) and construct: key1=value1&key2=value2&...
        // The last element also has an '&' before appending key=YOUR_API_KEY
        val signStr = params.entries.joinToString("") { "${it.key}=${it.value}&" } + "key=$API_KEY"

        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(signStr.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

// ===============================================================
// 2. JAZPAYS API CALL
// ===============================================================
class JazpaysApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    suspend fun createOrder(
        amount: Double,
        orderNo: String,
        callbackUrl: String
    ): String = withContext(Dispatchers.IO) {
        val amountStr = String.format(java.util.Locale.US, "%.2f", amount)
        val signature = JazpaysSigner.generateSignature(amountStr, orderNo, callbackUrl)

        val body = JSONObject().apply {
            put("merchant_id", "100222099")
            put("api_key", "25aa23a6200008a506628fa5f971fc1d")
            put("amount", amountStr)
            put("merchant_order_no", orderNo)
            put("callback_url", callbackUrl)
            put("signature", signature)
        }

        val request = Request.Builder()
            .url("https://api.jazpays.com/v1/create")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBodyStr = response.body?.string() ?: "{}"
        
        if (!response.isSuccessful) {
            throw Exception("HTTP Error ${response.code}: $responseBodyStr")
        }

        val json = JSONObject(responseBodyStr)

        // Support standard success boolean, "success" status string, or simply having a valid payment_url
        val isSuccess = json.optBoolean("success", false) || 
                        json.optString("status").lowercase() == "success" || 
                        json.optString("success").lowercase() == "true" ||
                        json.has("payment_url")

        if (isSuccess) {
            val payUrl = json.optString("payment_url")
            if (payUrl.isNullOrBlank()) {
                throw Exception("Payment URL is empty in response")
            }
            payUrl
        } else {
            val msg = json.optString("message", json.optString("status", "unknown"))
            throw Exception("Payment initiation failed: $msg")
        }
    }
}
