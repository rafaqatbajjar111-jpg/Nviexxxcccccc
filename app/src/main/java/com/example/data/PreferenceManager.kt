package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var token: String?
        get() = prefs.getString(Constants.KEY_TOKEN, null)
        set(value) = prefs.edit().putString(Constants.KEY_TOKEN, value).apply()

    var userId: Int
        get() = prefs.getInt(Constants.KEY_USER_ID, 0)
        set(value) = prefs.edit().putInt(Constants.KEY_USER_ID, value).apply()

    var phone: String
        get() = prefs.getString(Constants.KEY_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_PHONE, value).apply()

    var name: String
        get() = prefs.getString(Constants.KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_NAME, value).apply()

    var balance: Float
        get() = prefs.getFloat(Constants.KEY_BALANCE, 0.0f)
        set(value) = prefs.edit().putFloat(Constants.KEY_BALANCE, value).apply()

    var bonus: Float
        get() = prefs.getFloat(Constants.KEY_BONUS, 0.0f)
        set(value) = prefs.edit().putFloat(Constants.KEY_BONUS, value).apply()

    var recharge: Float
        get() = prefs.getFloat(Constants.KEY_RECHARGE, 0.0f)
        set(value) = prefs.edit().putFloat(Constants.KEY_RECHARGE, value).apply()

    var earningBalance: Float
        get() = prefs.getFloat("key_earning_balance", 0.0f)
        set(value) = prefs.edit().putFloat("key_earning_balance", value).apply()

    var bonusUsed: Float
        get() = prefs.getFloat("key_bonus_used", 0.0f)
        set(value) = prefs.edit().putFloat("key_bonus_used", value).apply()

    var vipLevel: Int
        get() = prefs.getInt(Constants.KEY_VIP_LEVEL, 0)
        set(value) = prefs.edit().putInt(Constants.KEY_VIP_LEVEL, value).apply()

    var referralCode: String
        get() = prefs.getString(Constants.KEY_REFERRAL_CODE, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_REFERRAL_CODE, value).apply()

    var pushNotificationsEnabled: Boolean
        get() = prefs.getBoolean("push_notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("push_notifications_enabled", value).apply()

    var transactionPinsEnabled: Boolean
        get() = prefs.getBoolean("transaction_pins_enabled", true)
        set(value) = prefs.edit().putBoolean("transaction_pins_enabled", value).apply()

    var biometricsEnabled: Boolean
        get() = prefs.getBoolean("biometrics_enabled", false)
        set(value) = prefs.edit().putBoolean("biometrics_enabled", value).apply()

    var appLanguage: String
        get() = prefs.getString("app_language", "English") ?: "English"
        set(value) = prefs.edit().putString("app_language", value).apply()

    var accountPassword: String
        get() = prefs.getString("account_password", "") ?: ""
        set(value) = prefs.edit().putString("account_password", value).apply()

    var bankHolderName: String
        get() = prefs.getString(Constants.KEY_BANK_HOLDER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_BANK_HOLDER_NAME, value).apply()

    var bankName: String
        get() = prefs.getString(Constants.KEY_BANK_NAME, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_BANK_NAME, value).apply()

    var bankAccountNumber: String
        get() = prefs.getString(Constants.KEY_BANK_ACCOUNT_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_BANK_ACCOUNT_NUMBER, value).apply()

    var bankIfscCode: String
        get() = prefs.getString(Constants.KEY_BANK_IFSC, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_BANK_IFSC, value).apply()

    var isBankDetailsSaved: Boolean
        get() = prefs.getBoolean(Constants.KEY_BANK_DETAILS_SAVED, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_BANK_DETAILS_SAVED, value).apply()

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
