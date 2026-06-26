package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Common Sealed UI State to handle asynchronous flows cleanly.
 */
sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/**
 * AuthViewModel manages user login and register flows, syncing state to SharedPreferences.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ServiceLocator.getApiService(application)
    private val prefs = ServiceLocator.getPrefs(application)
    private val okHttpClient = OkHttpClient()

    private val _loginState = MutableStateFlow<UiState<UserModel>>(UiState.Idle)
    val loginState: StateFlow<UiState<UserModel>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val registerState: StateFlow<UiState<String>> = _registerState.asStateFlow()
    
    private val _forgotPassState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val forgotPassState: StateFlow<UiState<String>> = _forgotPassState.asStateFlow()

    fun login(phone: String, pass: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            try {
                val res = api.login(LoginRequest(phone, pass))
                if (res.status == "success" && res.data != null) {
                    val user = res.data
                    prefs.token = "mock_token_success_invexx"
                    prefs.phone = user.phone
                    prefs.name = user.name
                    prefs.userId = user.userId
                    prefs.balance = user.balance
                    prefs.bonus = user.bonus
                    prefs.recharge = user.recharge
                    prefs.vipLevel = user.vipLevel
                    _loginState.value = UiState.Success(user)
                } else {
                    _loginState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _loginState.value = UiState.Error(e.message ?: "Network error occurred")
            }
        }
    }

    fun register(phone: String, userId: String, pass: String, referralCode: String?) {
        viewModelScope.launch {
            _registerState.value = UiState.Loading
            try {
                val res = api.register(RegisterRequest(phone, userId, pass, referralCode))
                if (res.status == "success") {
                    _registerState.value = UiState.Success(res.message)
                } else {
                    _registerState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _registerState.value = UiState.Error(e.message ?: "Registration failed")
            }
        }
    }
    
    fun sendOtp(phone: String) {
        viewModelScope.launch {
            _forgotPassState.value = UiState.Loading
            try {
                // Check if user exists
                val checkRes = api.checkUserExists(phone)
                if (checkRes.data != true) {
                    _forgotPassState.value = UiState.Error("User with this phone number does not exist.")
                    return@launch
                }

                val json = JSONObject().apply { put("phoneNumber", phone) }
                val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://potvggygstvctvngobxl.supabase.co/functions/v1/otpsend")
                    .post(body)
                    .addHeader("x-api-key", "pk_live_5a87c99a055d45929adeec7c9dfcf37ca311e219c165ab83")
                    .build()
                
                val response = withContext(Dispatchers.IO) { okHttpClient.newCall(request).execute() }
                val respStr = withContext(Dispatchers.IO) { response.body?.string() }
                if (response.isSuccessful && respStr != null) {
                    val jsonResp = JSONObject(respStr)
                    val status = jsonResp.optInt("status")
                    if (status == 200) {
                        val data = jsonResp.getJSONObject("data")
                        val verificationToken = data.getString("verificationToken")
                        val deviceId = data.getString("deviceId")
                        _forgotPassState.value = UiState.Success("OTP_SENT|$verificationToken|$deviceId")
                    } else {
                        _forgotPassState.value = UiState.Error(jsonResp.optString("message", "Failed to send OTP"))
                    }
                } else {
                    _forgotPassState.value = UiState.Error("Failed to send OTP")
                }
            } catch (e: Exception) {
                _forgotPassState.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }
    
    fun verifyOtpAndResetPass(phone: String, otp: String, deviceId: String, verificationToken: String, newPass: String) {
        viewModelScope.launch {
            _forgotPassState.value = UiState.Loading
            try {
                val json = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("verificationToken", verificationToken)
                    put("otpCode", otp)
                }
                val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://potvggygstvctvngobxl.supabase.co/functions/v1/verifyotp")
                    .post(body)
                    .addHeader("x-api-key", "pk_live_5a87c99a055d45929adeec7c9dfcf37ca311e219c165ab83")
                    .build()
                
                val response = withContext(Dispatchers.IO) { okHttpClient.newCall(request).execute() }
                val respStr = withContext(Dispatchers.IO) { response.body?.string() }
                if (response.isSuccessful && respStr != null) {
                    val jsonResp = JSONObject(respStr)
                    val status = jsonResp.optInt("status")
                    if (status == 200) {
                        // OTP Verified, now reset password!
                        val res = api.resetPassword(phone, newPass)
                        if (res.status == "success") {
                            _forgotPassState.value = UiState.Success("PASSWORD_RESET_SUCCESS")
                        } else {
                            _forgotPassState.value = UiState.Error(res.message ?: "Failed to reset password")
                        }
                    } else {
                        _forgotPassState.value = UiState.Error(jsonResp.optString("message", "Invalid OTP"))
                    }
                } else {
                    _forgotPassState.value = UiState.Error("OTP verification failed")
                }
            } catch (e: Exception) {
                _forgotPassState.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    fun resetStates() {
        _loginState.value = UiState.Idle
        _registerState.value = UiState.Idle
        _forgotPassState.value = UiState.Idle
    }
}

/**
 * HomeViewModel handles user portfolio stats, plans filtering by tab type, and plan purchase requests.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ServiceLocator.getApiService(application)
    private val prefs = ServiceLocator.getPrefs(application)

    private val _plansState = MutableStateFlow<UiState<List<PlanModel>>>(UiState.Loading)
    val plansState: StateFlow<UiState<List<PlanModel>>> = _plansState.asStateFlow()

    private val _buyState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val buyState: StateFlow<UiState<String>> = _buyState.asStateFlow()

    private val _selectedTab = MutableStateFlow("fixed_fund")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    private val _teamStats = MutableStateFlow<TeamStatsModel?>(null)
    val teamStats: StateFlow<TeamStatsModel?> = _teamStats.asStateFlow()

    fun loadTeamStats() {
        viewModelScope.launch {
            api.getTeamStatsFlow().collect { stats ->
                _teamStats.value = stats
            }
        }
    }

    // Expose wallet parameters directly synced with prefs
    private val _walletBalance = MutableStateFlow(prefs.balance)
    val walletBalance: StateFlow<Float> = _walletBalance.asStateFlow()

    private val _walletBonus = MutableStateFlow(prefs.bonus)
    val walletBonus: StateFlow<Float> = _walletBonus.asStateFlow()

    private val _walletEarning = MutableStateFlow(prefs.earningBalance)
    val walletEarning: StateFlow<Float> = _walletEarning.asStateFlow()

    private val _walletRecharge = MutableStateFlow(prefs.recharge)
    val walletRecharge: StateFlow<Float> = _walletRecharge.asStateFlow()

    private val _userPhone = MutableStateFlow(prefs.phone)
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _userId = MutableStateFlow(prefs.userId)
    val userId: StateFlow<Int> = _userId.asStateFlow()

    private val _vipLevel = MutableStateFlow(prefs.vipLevel)
    val vipLevel: StateFlow<Int> = _vipLevel.asStateFlow()

    private val _referralCode = MutableStateFlow(prefs.referralCode)
    val referralCode: StateFlow<String> = _referralCode.asStateFlow()

    init {
        loadPlans(_selectedTab.value)
    }

    fun setTab(type: String) {
        _selectedTab.value = type
        loadPlans(type)
    }

    fun syncWallet() {
        _walletBalance.value = prefs.balance
        _walletBonus.value = prefs.bonus
        _walletEarning.value = prefs.earningBalance
        _walletRecharge.value = prefs.recharge
        _userPhone.value = prefs.phone
        _userId.value = prefs.userId
        _vipLevel.value = prefs.vipLevel
        _referralCode.value = prefs.referralCode
    }

    fun loadPlans(type: String) {
        viewModelScope.launch {
            _plansState.value = UiState.Loading
            try {
                val res = api.getPlans(type)
                if (res.status == "success" && res.data != null) {
                    _plansState.value = UiState.Success(res.data)
                } else {
                    _plansState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _plansState.value = UiState.Error(e.message ?: "Failed to load plans")
            }
        }
    }

    fun buyPlan(plan: PlanModel, investAmount: Float) {
        viewModelScope.launch {
            _buyState.value = UiState.Loading
            try {
                val res = api.buyPlan(plan, investAmount)
                if (res.status == "success") {
                    syncWallet()
                    _buyState.value = UiState.Success(res.message)
                } else {
                    _buyState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _buyState.value = UiState.Error(e.message ?: "Investment failed")
            }
        }
    }

    fun resetBuyState() {
        _buyState.value = UiState.Idle
    }
}

/**
 * TeamViewModel manages group stats and registered/active partner counts.
 */
class TeamViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ServiceLocator.getApiService(application)
    private val prefs = ServiceLocator.getPrefs(application)

    private val _teamState = MutableStateFlow<UiState<TeamStatsModel>>(UiState.Loading)
    val teamState: StateFlow<UiState<TeamStatsModel>> = _teamState.asStateFlow()

    private val _userPhone = MutableStateFlow(prefs.phone)
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _userId = MutableStateFlow(prefs.userId)
    val userId: StateFlow<Int> = _userId.asStateFlow()

    private val _referralCode = MutableStateFlow(prefs.referralCode)
    val referralCode: StateFlow<String> = _referralCode.asStateFlow()

    init {
        observeTeamStats()
    }

    fun syncUser() {
        _userPhone.value = prefs.phone
        _userId.value = prefs.userId
        _referralCode.value = prefs.referralCode
    }

    private fun observeTeamStats() {
        viewModelScope.launch {
            _teamState.value = UiState.Loading
            try {
                api.getTeamStatsFlow().collect { stats ->
                    _teamState.value = UiState.Success(stats)
                    _referralCode.value = prefs.referralCode
                }
            } catch (e: Exception) {
                _teamState.value = UiState.Error(e.message ?: "Failed to load team data")
            }
        }
    }

    fun loadTeamStats() {
        observeTeamStats()
    }
}

/**
 * BlogViewModel manages articles, posts, and news feeds.
 */
class BlogViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ServiceLocator.getApiService(application)

    private val _blogsState = MutableStateFlow<UiState<List<BlogModel>>>(UiState.Loading)
    val blogsState: StateFlow<UiState<List<BlogModel>>> = _blogsState.asStateFlow()

    init {
        observeBlogs()
    }

    private fun observeBlogs() {
        viewModelScope.launch {
            _blogsState.value = UiState.Loading
            try {
                api.getBlogsFlow().collect { list ->
                    _blogsState.value = UiState.Success(list)
                }
            } catch (e: Exception) {
                _blogsState.value = UiState.Error(e.message ?: "Failed to fetch blogs")
            }
        }
    }

    fun loadBlogs() {
        observeBlogs()
    }
}

/**
 * MineViewModel manages user wallet balances and history syncs.
 */
class MineViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ServiceLocator.getApiService(application)
    private val prefs = ServiceLocator.getPrefs(application)

    private val _userName = MutableStateFlow(prefs.name)
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userPhone = MutableStateFlow(prefs.phone)
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _userId = MutableStateFlow(prefs.userId)
    val userId: StateFlow<Int> = _userId.asStateFlow()

    private val _vipLevel = MutableStateFlow(prefs.vipLevel)
    val vipLevel: StateFlow<Int> = _vipLevel.asStateFlow()

    private val _balance = MutableStateFlow(prefs.balance)
    val balance: StateFlow<Float> = _balance.asStateFlow()

    private val _bonus = MutableStateFlow(prefs.bonus)
    val bonus: StateFlow<Float> = _bonus.asStateFlow()

    private val _earningBalance = MutableStateFlow(prefs.earningBalance)
    val earningBalance: StateFlow<Float> = _earningBalance.asStateFlow()

    private val _recharge = MutableStateFlow(prefs.recharge)
    val recharge: StateFlow<Float> = _recharge.asStateFlow()

    private val _referralCode = MutableStateFlow(prefs.referralCode)
    val referralCode: StateFlow<String> = _referralCode.asStateFlow()

    init {
        observeUserProfile()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            try {
                api.getUserProfileFlow().collect { profile ->
                    (profile["balance"] as? Float)?.let { _balance.value = it }
                    (profile["bonus"] as? Float)?.let { _bonus.value = it }
                    (profile["earning_balance"] as? Float)?.let { _earningBalance.value = it }
                    (profile["recharge"] as? Float)?.let { _recharge.value = it }
                    (profile["vipLevel"] as? Int)?.let { _vipLevel.value = it }
                    (profile["name"] as? String)?.let { _userName.value = it }
                    (profile["referralCode"] as? String)?.let { _referralCode.value = it }
                }
            } catch (e: Exception) { /* use prefs fallback */ }
        }
    }

    fun refreshProfile() {
        _userName.value = prefs.name
        _userPhone.value = prefs.phone
        _userId.value = prefs.userId
        _vipLevel.value = prefs.vipLevel
        _balance.value = prefs.balance
        _bonus.value = prefs.bonus
        _earningBalance.value = prefs.earningBalance
        _recharge.value = prefs.recharge
        _referralCode.value = prefs.referralCode
    }

    fun triggerMidnightEarnings(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = api.adminTriggerMidnightReturns()
                refreshProfile()
                onResult(res.message)
            } catch (e: Exception) {
                onResult(e.message ?: "Failed")
            }
        }
    }

    fun updateName(newName: String) {
        viewModelScope.launch {
            try {
                api.updateProfileName(newName)
                _userName.value = prefs.name
            } catch (e: Exception) {
                // local fallback if network is slow
                prefs.name = newName
                _userName.value = newName
            }
        }
    }

    fun logout() {
        prefs.clearSession()
    }
}

/**
 * DepositViewModel processes UPI deposit requests and custom amount presets.
 */
class DepositViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ServiceLocator.getApiService(application)
    private val prefs = ServiceLocator.getPrefs(application)

    private val _depositState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val depositState: StateFlow<UiState<String>> = _depositState.asStateFlow()

    // Preset chips
    val presetAmounts = listOf(200f, 500f, 1000f, 2000f, 5000f, 19990f)

    private val _selectedAmount = MutableStateFlow(200f)
    val selectedAmount: StateFlow<Float> = _selectedAmount.asStateFlow()

    private val _customAmount = MutableStateFlow("")
    val customAmount: StateFlow<String> = _customAmount.asStateFlow()

    // Direct wallet values for stats card
    private val _balance = MutableStateFlow(prefs.balance)
    val balance: StateFlow<Float> = _balance.asStateFlow()

    private val _bonus = MutableStateFlow(prefs.bonus)
    val bonus: StateFlow<Float> = _bonus.asStateFlow()

    private val _recharge = MutableStateFlow(prefs.recharge)
    val recharge: StateFlow<Float> = _recharge.asStateFlow()

    // Fields to track the last initiated transaction
    var lastInitiatedOrderNo: String? = null
    var lastInitiatedAmount: Float = 0f

    fun selectPreset(amount: Float) {
        _selectedAmount.value = amount
        _customAmount.value = ""
    }

    fun setCustomAmount(value: String) {
        _customAmount.value = value
        val parsed = value.toFloatOrNull()
        if (parsed != null) {
            _selectedAmount.value = parsed
        }
    }

    fun initiateDeposit() {
        viewModelScope.launch {
            _depositState.value = UiState.Loading
            try {
                // Automatically add ₹10 to user's selected or custom amount
                val amount = _selectedAmount.value + 10f
                if (amount < 210f) {
                    _depositState.value = UiState.Error("Minimum deposit is ₹210 (including ₹10 auto-addition).")
                    return@launch
                }
                if (amount > 20000f) {
                    _depositState.value = UiState.Error("Maximum deposit is ₹20,000.")
                    return@launch
                }
                
                val userId = prefs.userId.toString()
                val orderNo = "${userId}_${System.currentTimeMillis()}"
                val callbackUrl = "https://prime-khatab-default-rtdb.firebaseio.com/payment_callbacks.json"
                
                // Track initiated order
                lastInitiatedOrderNo = orderNo
                lastInitiatedAmount = amount
                
                // Save pending transaction to DB
                api.createPendingDeposit(amount, orderNo)
                
                val watchPaysApi = WatchPaysApi()
                val paymentUrl = watchPaysApi.createOrder(amount.toDouble(), orderNo, callbackUrl)
                
                _depositState.value = UiState.Success(paymentUrl)
            } catch (e: Exception) {
                _depositState.value = UiState.Error(e.message ?: "Failed to initiate payment")
            }
        }
    }

    fun processPaymentCallback(orderNo: String?, status: String?, amount: Float?) {
        viewModelScope.launch {
            try {
                val resolvedOrderNo = orderNo ?: lastInitiatedOrderNo
                val resolvedAmount = amount ?: lastInitiatedAmount
                val resolvedStatus = status ?: "success"

                if (resolvedOrderNo != null && resolvedAmount > 0f) {
                    val result = api.processPaymentCallback(resolvedOrderNo, resolvedStatus, resolvedAmount)
                    if (result.status == "success") {
                        // Refresh local state flows immediately
                        _balance.value = prefs.balance
                        _recharge.value = prefs.recharge
                    }
                }
            } catch (e: Exception) {
                // Ignore or handle
            }
        }
    }

    fun resetDepositState() {
        _depositState.value = UiState.Idle
    }
}

/**
 * WithdrawViewModel handles bank card setups and payouts.
 */
class WithdrawViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ServiceLocator.getApiService(application)
    private val prefs = ServiceLocator.getPrefs(application)

    private val _withdrawState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val withdrawState: StateFlow<UiState<String>> = _withdrawState.asStateFlow()

    private val _balance = MutableStateFlow(prefs.earningBalance)
    val balance: StateFlow<Float> = _balance.asStateFlow()

    private val _isBankSaved = MutableStateFlow(prefs.isBankDetailsSaved)
    val isBankSaved: StateFlow<Boolean> = _isBankSaved.asStateFlow()

    // Form inputs
    private val _holderName = MutableStateFlow("")
    val holderName: StateFlow<String> = _holderName.asStateFlow()

    private val _bankName = MutableStateFlow("")
    val bankName: StateFlow<String> = _bankName.asStateFlow()

    private val _accountNumber = MutableStateFlow("")
    val accountNumber: StateFlow<String> = _accountNumber.asStateFlow()

    private val _ifscCode = MutableStateFlow("")
    val ifscCode: StateFlow<String> = _ifscCode.asStateFlow()

    init {
        loadBankDetails()
    }

    private fun loadBankDetails() {
        val hasOfflineBank = prefs.bankHolderName.isNotBlank() && prefs.bankAccountNumber.isNotBlank()
        if (prefs.isBankDetailsSaved || hasOfflineBank) {
            _isBankSaved.value = true
            _holderName.value = prefs.bankHolderName
            _bankName.value = prefs.bankName
            _accountNumber.value = prefs.bankAccountNumber
            _ifscCode.value = prefs.bankIfscCode
        }
        viewModelScope.launch {
            try {
                api.getUserProfileFlow().collect { profile ->
                    val isBankSavedInDb = profile["isBankDetailsSaved"] as? Boolean == true
                    val dbHolder = profile["bankHolderName"] as? String ?: ""
                    val dbAcc = profile["bankAccountNumber"] as? String ?: ""
                    
                    if (isBankSavedInDb || (dbHolder.isNotBlank() && dbAcc.isNotBlank())) {
                        _isBankSaved.value = true
                        if (dbHolder.isNotBlank()) _holderName.value = dbHolder
                        (profile["bankName"] as? String)?.let { _bankName.value = it }
                        if (dbAcc.isNotBlank()) _accountNumber.value = dbAcc
                        (profile["bankIfscCode"] as? String)?.let { _ifscCode.value = it }
                        
                        // Sync to prefs
                        prefs.bankHolderName = _holderName.value
                        prefs.bankName = _bankName.value
                        prefs.bankAccountNumber = _accountNumber.value
                        prefs.bankIfscCode = _ifscCode.value
                        prefs.isBankDetailsSaved = true
                    }
                    (profile["earning_balance"] as? Float)?.let { _balance.value = it }
                }
            } catch (e: Exception) { /* use prefs fallback */ }
        }
    }

    fun saveBankDetails(holder: String, bank: String, acc: String, ifsc: String) {
        if (holder.isBlank() || bank.isBlank() || acc.isBlank() || ifsc.isBlank()) {
            _withdrawState.value = UiState.Error("Please fill out all bank credentials to save.")
            return
        }
        viewModelScope.launch {
            _withdrawState.value = UiState.Loading
            try {
                val res = api.saveBankDetailsInDb(holder, bank, acc, ifsc)
                if (res.status == "success") {
                    prefs.bankHolderName = holder
                    prefs.bankName = bank
                    prefs.bankAccountNumber = acc
                    prefs.bankIfscCode = ifsc
                    prefs.isBankDetailsSaved = true
                    _isBankSaved.value = true
                    
                    _holderName.value = holder
                    _bankName.value = bank
                    _accountNumber.value = acc
                    _ifscCode.value = ifsc
                    _withdrawState.value = UiState.Success("Bank details locked and saved securely in cloud database!")
                } else {
                    _withdrawState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _withdrawState.value = UiState.Error(e.message ?: "Failed to save bank details online")
            }
        }
    }

    fun enableBankEdit() {
        _isBankSaved.value = false
    }

    private val _withdrawAmount = MutableStateFlow("")
    val withdrawAmount: StateFlow<String> = _withdrawAmount.asStateFlow()

    val indianBanks = listOf(
        "SBI", "HDFC", "ICICI", "Axis", "Kotak", "PNB", "Bank of Baroda", "Canara",
        "Union Bank", "Yes Bank", "IDFC First", "IndusInd", "Federal Bank", "Karnataka Bank",
        "South Indian Bank", "UCO Bank", "Bank of India", "Central Bank of India"
    )

    fun updateHolderName(value: String) { _holderName.value = value }
    fun updateBankName(value: String) { _bankName.value = value }
    fun updateAccountNumber(value: String) { _accountNumber.value = value }
    fun updateIfscCode(value: String) { _ifscCode.value = value }
    fun updateWithdrawAmount(value: String) { _withdrawAmount.value = value }

    fun refreshBalance() {
        _balance.value = prefs.earningBalance
    }

    fun requestWithdrawal() {
        viewModelScope.launch {
            _withdrawState.value = UiState.Loading
            try {
                val amt = _withdrawAmount.value.toFloatOrNull()
                if (amt == null || amt < 100f) {
                    _withdrawState.value = UiState.Error("Minimum withdrawal amount is ₹100.00")
                    return@launch
                }
                if (amt > prefs.earningBalance) {
                    _withdrawState.value = UiState.Error("Insufficient earning balance. You can only withdraw plan earnings.")
                    return@launch
                }
                val nameVal = if (prefs.isBankDetailsSaved) prefs.bankHolderName else _holderName.value
                val bankVal = if (prefs.isBankDetailsSaved) prefs.bankName else _bankName.value
                val accVal = if (prefs.isBankDetailsSaved) prefs.bankAccountNumber else _accountNumber.value
                val ifscVal = if (prefs.isBankDetailsSaved) prefs.bankIfscCode else _ifscCode.value

                if (nameVal.isBlank() || bankVal.isBlank() || accVal.isBlank() || ifscVal.isBlank()) {
                    _withdrawState.value = UiState.Error("Please fill out and save all bank credentials first.")
                    return@launch
                }

                val res = api.createWithdrawal(
                    WithdrawRequest(
                        name = nameVal,
                        bankName = bankVal,
                        accountNumber = accVal,
                        ifscCode = ifscVal,
                        amount = amt
                    )
                )

                if (res.status == "success") {
                    refreshBalance()
                    _withdrawState.value = UiState.Success("Withdrawal of ₹$amt requested successfully! Processed within 24 hours.")
                } else {
                    _withdrawState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _withdrawState.value = UiState.Error(e.message ?: "Failed to request withdrawal")
            }
        }
    }

    fun resetWithdrawState() {
        _withdrawState.value = UiState.Idle
    }
}

/**
 * Simple ViewModel for list views.
 */
class ListsViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ServiceLocator.getApiService(application)

    private val _orders = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Loading)
    val orders: StateFlow<UiState<List<OrderModel>>> = _orders.asStateFlow()

    private val _transactions = MutableStateFlow<UiState<List<TransactionModel>>>(UiState.Loading)
    val transactions: StateFlow<UiState<List<TransactionModel>>> = _transactions.asStateFlow()

    private val _tasks = MutableStateFlow<UiState<List<TaskModel>>>(UiState.Loading)
    val tasks: StateFlow<UiState<List<TaskModel>>> = _tasks.asStateFlow()

    private val _claimingTaskId = MutableStateFlow<Int?>(null)
    val claimingTaskId: StateFlow<Int?> = _claimingTaskId.asStateFlow()

    private val _notifications = MutableStateFlow<UiState<List<NotificationModel>>>(UiState.Loading)
    val notifications: StateFlow<UiState<List<NotificationModel>>> = _notifications.asStateFlow()

    private val _redeemState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val redeemState: StateFlow<UiState<String>> = _redeemState.asStateFlow()

    fun loadOrders() {
        viewModelScope.launch {
            _orders.value = UiState.Loading
            try {
                api.getOrdersFlow().collect { list ->
                    _orders.value = UiState.Success(list)
                }
            } catch (e: Exception) {
                _orders.value = UiState.Error(e.message ?: "Failed to fetch investments")
            }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _transactions.value = UiState.Loading
            try {
                api.getTransactionsFlow().collect { list ->
                    _transactions.value = UiState.Success(list)
                }
            } catch (e: Exception) {
                _transactions.value = UiState.Error(e.message ?: "Failed to load transactions")
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = UiState.Loading
            try {
                val res = api.getTasks()
                if (res.status == "success" && res.data != null) {
                    _tasks.value = UiState.Success(res.data)
                } else {
                    _tasks.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _tasks.value = UiState.Error(e.message ?: "Failed to load tasks")
            }
        }
    }

    fun claimTaskReward(task: TaskModel) {
        viewModelScope.launch {
            _claimingTaskId.value = task.id
            try {
                val res = api.claimTask(task.id)
                if (res.status == "success") {
                    loadTasks() // reload
                }
            } catch (e: Exception) {
                // ignore or handle
            } finally {
                _claimingTaskId.value = null
            }
        }
    }

    fun checkInUser() {
        viewModelScope.launch {
            try {
                val res = api.checkIn()
                if (res.status == "success") {
                    loadTasks() // reload tasks
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _notifications.value = UiState.Loading
            try {
                val res = api.getNotifications()
                if (res.status == "success" && res.data != null) {
                    _notifications.value = UiState.Success(res.data)
                } else {
                    _notifications.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _notifications.value = UiState.Error(e.message ?: "Failed to load notifications")
            }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            try {
                val res = api.markNotificationsAsRead()
                if (res.status == "success") {
                    loadNotifications()
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun redeemGiftCode(code: String) {
        viewModelScope.launch {
            _redeemState.value = UiState.Loading
            try {
                if (code.isBlank()) {
                    _redeemState.value = UiState.Error("Please enter a gift code.")
                    return@launch
                }
                val res = api.redeemGiftCode(RedeemRequest(code))
                if (res.status == "success" && res.data != null) {
                    _redeemState.value = UiState.Success(res.message)
                } else {
                    _redeemState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _redeemState.value = UiState.Error(e.message ?: "Failed to redeem code")
            }
        }
    }

    fun resetRedeemState() {
        _redeemState.value = UiState.Idle
    }
}

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ServiceLocator.getApiService(application)

    private val _users = MutableStateFlow<UiState<List<Map<String, Any>>>>(UiState.Idle)
    val users: StateFlow<UiState<List<Map<String, Any>>>> = _users

    private val _plans = MutableStateFlow<UiState<List<PlanModel>>>(UiState.Idle)
    val plans: StateFlow<UiState<List<PlanModel>>> = _plans

    private val _blogs = MutableStateFlow<UiState<List<BlogModel>>>(UiState.Idle)
    val blogs: StateFlow<UiState<List<BlogModel>>> = _blogs

    private val _tasks = MutableStateFlow<UiState<List<TaskModel>>>(UiState.Idle)
    val tasks: StateFlow<UiState<List<TaskModel>>> = _tasks

    private val _systemSettings = MutableStateFlow<UiState<Map<String, Any>>>(UiState.Idle)
    val systemSettings: StateFlow<UiState<Map<String, Any>>> = _systemSettings

    private val _operationState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val operationState: StateFlow<UiState<String>> = _operationState

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadUsers()
        loadPlans()
        loadBlogs()
        loadTasks()
        loadSystemSettings()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _users.value = UiState.Loading
            try {
                val res = api.adminGetAllUsers()
                if (res.status == "success" && res.data != null) {
                    _users.value = UiState.Success(res.data)
                } else {
                    _users.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _users.value = UiState.Error(e.message ?: "Failed to load users")
            }
        }
    }

    fun loadPlans() {
        viewModelScope.launch {
            _plans.value = UiState.Loading
            try {
                val res = api.getPlans("all")
                if (res.status == "success" && res.data != null) {
                    _plans.value = UiState.Success(res.data)
                } else {
                    _plans.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _plans.value = UiState.Error(e.message ?: "Failed to load plans")
            }
        }
    }

    fun loadBlogs() {
        viewModelScope.launch {
            _blogs.value = UiState.Loading
            try {
                api.getBlogsFlow().collect { list ->
                    _blogs.value = UiState.Success(list)
                }
            } catch (e: Exception) {
                _blogs.value = UiState.Error(e.message ?: "Failed to load blogs")
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = UiState.Loading
            try {
                val response = api.getTasks()
                if (response.status == "success") {
                    _tasks.value = UiState.Success(response.data ?: emptyList())
                } else {
                    _tasks.value = UiState.Error(response.message ?: "Failed to load tasks")
                }
            } catch (e: Exception) {
                _tasks.value = UiState.Error(e.message ?: "Failed to load tasks")
            }
        }
    }

    fun loadSystemSettings() {
        viewModelScope.launch {
            _systemSettings.value = UiState.Loading
            try {
                val res = api.getSystemSettings()
                if (res.status == "success" && res.data != null) {
                    _systemSettings.value = UiState.Success(res.data)
                } else {
                    _systemSettings.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _systemSettings.value = UiState.Error(e.message ?: "Failed to load settings")
            }
        }
    }

    fun updateUser(phone: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminUpdateUser(phone, updates)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("User updated successfully!")
                    loadUsers()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Update user failed")
            }
        }
    }

    fun processTransaction(phone: String, txId: Int, newStatus: String) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminProcessTransaction(phone, txId, newStatus)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("Transaction processed as $newStatus")
                    loadUsers()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Transaction update failed")
            }
        }
    }

    fun addPlan(plan: PlanModel) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminAddPlan(plan)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("Plan added successfully")
                    loadPlans()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Add plan failed")
            }
        }
    }

    fun updatePlan(plan: PlanModel) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminUpdatePlan(plan)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("Plan updated successfully")
                    loadPlans()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Update plan failed")
            }
        }
    }

    fun deletePlan(id: Int) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminDeletePlan(id)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("Plan deleted successfully")
                    loadPlans()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Delete plan failed")
            }
        }
    }

    fun addBlog(blog: BlogModel) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminAddBlog(blog)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("Blog post published successfully")
                    loadBlogs()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Post blog failed")
            }
        }
    }

    fun updateBlog(blog: BlogModel) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminUpdateBlog(blog)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("Blog updated successfully")
                    loadBlogs()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Update blog failed")
            }
        }
    }

    fun deleteBlog(id: Int) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminDeleteBlog(id)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("Blog post deleted successfully")
                    loadBlogs()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Delete blog failed")
            }
        }
    }

    fun addTask(task: TaskModel) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminAddTask(task)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("Task added successfully")
                    loadTasks()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Add task failed")
            }
        }
    }

    fun updateTask(task: TaskModel) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminUpdateTask(task)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("Task updated successfully")
                    loadTasks()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Update task failed")
            }
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminDeleteTask(id)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("Task deleted successfully")
                    loadTasks()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Delete task failed")
            }
        }
    }

    fun triggerMidnightEarnings() {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.adminTriggerMidnightReturns()
                if (res.status == "success") {
                    _operationState.value = UiState.Success(res.message)
                    loadUsers()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Midnight returns run failed")
            }
        }
    }

    fun saveSystemSettings(settings: Map<String, Any>) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            try {
                val res = api.updateSystemSettings(settings)
                if (res.status == "success") {
                    _operationState.value = UiState.Success("System settings saved successfully")
                    loadSystemSettings()
                } else {
                    _operationState.value = UiState.Error(res.message)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error(e.message ?: "Save system settings failed")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = UiState.Idle
    }
}
