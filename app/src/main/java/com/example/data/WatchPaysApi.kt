package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

// ===============================================================
// 1. SIGNATURE GENERATION
// ===============================================================
object WatchPaysSigner {
    private const val MERCHANT_ID = "100555375"
    private const val API_KEY = "de6dd0d15c1d3cf6eb846870fe9f9c8c"

    fun generateSignature(
        amount: String,
        merchantOrderNo: String,
        callbackUrl: String
    ): String {
        val params = sortedMapOf(
            "amount" to amount,
            "callback_url" to callbackUrl,
            "merchant_id" to MERCHANT_ID,
            "merchant_order_no" to merchantOrderNo
        )

        val signStr = params.entries.joinToString("") { "${it.key}=${it.value}&" } + "key=$API_KEY"

        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(signStr.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

// ===============================================================
// 2. API CALL
// ===============================================================
class WatchPaysApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    suspend fun createOrder(
        amount: Double,
        orderNo: String,
        callbackUrl: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val amountStr = DecimalFormat("0.00").format(amount)
            val signature = WatchPaysSigner.generateSignature(amountStr, orderNo, callbackUrl)

            val body = JSONObject().apply {
                put("merchant_id", "100555375")
                put("api_key", "de6dd0d15c1d3cf6eb846870fe9f9c8c")
                put("amount", amountStr)
                put("merchant_order_no", orderNo)
                put("callback_url", callbackUrl)
                put("signature", signature)
            }

            val request = Request.Builder()
                .url("https://api.watchpays.com/v1/create")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")

            if (json.optBoolean("success")) {
                json.optString("payment_url")
            } else {
                val msg = json.optString("message", json.optString("status", "unknown"))
                throw Exception("Payment failed: $msg")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
