package com.example.data

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.channels.awaitClose
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Suspend extension to await standard Play Services Task
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            cont.resume(task.result)
        } else {
            cont.resumeWithException(task.exception ?: RuntimeException("Firebase task failed"))
        }
    }
}

// Suspend extension to await a single Value Event from Realtime Database Query
suspend fun Query.awaitValue(): DataSnapshot = suspendCancellableCoroutine { cont ->
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (cont.isActive) {
                cont.resume(snapshot)
            }
        }
        override fun onCancelled(error: DatabaseError) {
            if (cont.isActive) {
                cont.resumeWithException(error.toException())
            }
        }
    }
    addListenerForSingleValueEvent(listener)
    cont.invokeOnCancellation {
        removeEventListener(listener)
    }
}

/**
 * Real-time cloud synchronization service connecting Invexx to Firebase Auth and Realtime Database.
 */
class FirebaseApiService(private val context: Context) : ApiService {
    private val prefs = PreferenceManager(context)
    private val firebaseDatabaseUrl = "https://prime-khatab-default-rtdb.firebaseio.com"

    private val currentPhone: String
        get() = prefs.phone

    override suspend fun login(request: LoginRequest): ApiResponse<UserModel> {
        if (request.phone.isBlank() || request.password.isBlank()) {
            return ApiResponse("error", "Phone and password are required", null)
        }

        try {
            // Fetch profile from Realtime Database first
            val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users")
            val snapshot = dbRef.child(request.phone).awaitValue()
            var dbPasswordMatched = false

            if (snapshot.exists()) {
                val dbPassword = snapshot.child("password").getValue(String::class.java)
                if (dbPassword != null) {
                    if (dbPassword == request.password) {
                        dbPasswordMatched = true
                    } else {
                        // Password exists in DB but doesn't match
                        return ApiResponse("error", "Invalid password", null)
                    }
                }
            }

            if (!dbPasswordMatched) {
                // Fallback to Firebase Auth (mapping phone as virtual email)
                val authEmail = "${request.phone}@invexx.app"
                FirebaseAuth.getInstance().signInWithEmailAndPassword(authEmail, request.password).await()
            }

            if (snapshot.exists()) {
                val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                val phone = snapshot.child("phone").getValue(String::class.java) ?: request.phone
                val userIdStr = snapshot.child("userId").getValue(String::class.java) ?: request.phone.hashCode().toString()
                val balance = snapshot.child("balance").getValue(Float::class.java) ?: 0.0f
                val earningBalance = snapshot.child("earning_balance").getValue(Float::class.java) ?: 0.0f
                val bonus = snapshot.child("bonus").getValue(Float::class.java) ?: 0.0f
                val bonusUsed = snapshot.child("bonusUsed").getValue(Float::class.java) ?: 0.0f
                val recharge = snapshot.child("recharge").getValue(Float::class.java) ?: 0.0f
                val vipLevel = snapshot.child("vipLevel").getValue(Int::class.java) ?: 1
                val myReferralCode = snapshot.child("myReferralCode").getValue(String::class.java) ?: request.phone
                val isVerified = snapshot.child("isVerified").getValue(Boolean::class.java) ?: true

                val parsedUserId = userIdStr.toIntOrNull() ?: request.phone.hashCode().coerceAtLeast(100000)

                val bankHolderName = snapshot.child("bankHolderName").getValue(String::class.java)
                    ?: snapshot.child("bank").child("bankHolderName").getValue(String::class.java) ?: ""
                val bankName = snapshot.child("bankName").getValue(String::class.java)
                    ?: snapshot.child("bank").child("bankName").getValue(String::class.java) ?: ""
                val bankAccountNumber = snapshot.child("bankAccountNumber").getValue(String::class.java)
                    ?: snapshot.child("bank").child("bankAccountNumber").getValue(String::class.java) ?: ""
                val bankIfscCode = snapshot.child("bankIfscCode").getValue(String::class.java)
                    ?: snapshot.child("bank").child("bankIfscCode").getValue(String::class.java) ?: ""
                val isBankDetailsSaved = snapshot.child("isBankDetailsSaved").getValue(Boolean::class.java)
                    ?: snapshot.child("bank").child("isBankDetailsSaved").getValue(Boolean::class.java) ?: false

                // Sync to SharedPreferences for fast offline rendering / session caching
                val tokenResult = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()
                prefs.token = tokenResult?.token ?: "firebase_token_${request.phone}"
                prefs.phone = phone
                prefs.name = name
                prefs.userId = parsedUserId
                prefs.balance = balance
                prefs.earningBalance = earningBalance
                prefs.bonus = bonus
                prefs.bonusUsed = bonusUsed
                prefs.recharge = recharge
                prefs.vipLevel = vipLevel
                prefs.referralCode = myReferralCode
                
                prefs.bankHolderName = bankHolderName
                prefs.bankName = bankName
                prefs.bankAccountNumber = bankAccountNumber
                prefs.bankIfscCode = bankIfscCode
                prefs.isBankDetailsSaved = isBankDetailsSaved

                val passwordInDb = snapshot.child("password").getValue(String::class.java) ?: request.password
                prefs.accountPassword = passwordInDb

                val user = UserModel(
                    id = phone.hashCode(),
                    name = name,
                    phone = phone,
                    userId = parsedUserId,
                    balance = balance,
                    bonus = bonus,
                    recharge = recharge,
                    vipLevel = vipLevel,
                    referralCode = myReferralCode,
                    isVerified = isVerified
                )
                return ApiResponse("success", "Login successful", user)
            } else {
                return ApiResponse("error", "User profile not found in database", null)
            }
        } catch (e: Exception) {
            return ApiResponse("error", e.message ?: "Authentication failed", null)
        }
    }

    override suspend fun register(request: RegisterRequest): ApiResponse<String> {
        if (request.phone.isBlank() || request.userId.isBlank() || request.password.isBlank()) {
            return ApiResponse("error", "All fields are required", null)
        }

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users")

        try {
            // Check if user already exists
            val snapshot = dbRef.child(request.phone).awaitValue()
            if (snapshot.exists()) {
                return ApiResponse("error", "Phone number is already registered", null)
            }

            // Create Firebase Auth user
            val authEmail = "${request.phone}@invexx.app"
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(authEmail, request.password).await()

            // Fetch signup bonus from system settings dynamically
            val systemSettingsRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("system_settings")
            val settingsSnap = systemSettingsRef.awaitValue()
            val signupBonus = if (settingsSnap.exists()) {
                settingsSnap.child("signup_bonus").getValue(Float::class.java) ?: 46.0f
            } else {
                val defaultSettings = mapOf(
                    "minVersion" to "1.0.0",
                    "latestVersion" to "1.0.0",
                    "updateUrl" to "https://invexx.app/update",
                    "upsetScreenEnabled" to false,
                    "upsetMessage" to "We are undergoing scheduled maintenance. The app will be fully functional shortly. We apologize for the inconvenience.",
                    "telegram_url" to "https://t.me/invexx_official",
                    "whatsapp_url" to "https://wa.me/919999999999",
                    "support_email" to "support@invexx-wealth.com",
                    "upi_id" to "invexx@ybl",
                    "signup_bonus" to 46.0
                )
                systemSettingsRef.setValue(defaultSettings).await()
                46.0f
            }

            // Define initial user properties and default mocks
            val userIdVal = request.phone.hashCode().coerceAtLeast(100000)
            val initialOrders = emptyList<OrderModel>()
            val initialTransactions = listOf(
                TransactionModel(1, "bonus", signupBonus, "Success", "Just now", "Sign up bonus balance")
            )
            val initialTasks = listOf(
                TaskModel(1, "Daily Check-In", "Get daily active login rewards", 0.0f, isClaimed = false, isClaimable = false, 5.00f),
                TaskModel(2, "Invite 1 Friend", "Invite a friend to register and deposit", 0.0f, isClaimed = false, isClaimable = false, 50.00f),
                TaskModel(3, "First Investment", "Purchase your first investment plan", 0.0f, isClaimed = false, isClaimable = false, 10.00f)
            )
            val initialNotifications = listOf(
                mapOf(
                    "id" to 1,
                    "title" to "Welcome to INVEXX!",
                    "message" to "Start your investment journey with our Fixed, Welfare, and Yearly plans. Earn steady daily revenue!",
                    "date" to "Just now",
                    "isRead" to false,
                    "timestamp" to System.currentTimeMillis()
                )
            )

            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            val uniqueStr = (1..5).map { chars.random() }.joinToString("")
            val myReferralCode = "IN$uniqueStr"
            
            val generatedName = "INVEXXUSER" + (1000..9999).random()

            val userMap = mapOf(
                "id" to request.phone.hashCode(),
                "name" to generatedName,
                "phone" to request.phone,
                "userId" to userIdVal.toString(),
                "password" to request.password,
                "balance" to 0.0f,
                "earning_balance" to 0.0f,
                "bonus" to signupBonus,
                "bonusUsed" to 0.0f,
                "recharge" to 0.0f,
                "vipLevel" to 1,
                "referralCode" to (request.referralCode ?: "PHONE"),
                "myReferralCode" to myReferralCode,
                "isVerified" to true,
                "legacyCommissionSynced" to true,
                "orders" to initialOrders,
                "transactions" to initialTransactions,
                "tasks" to initialTasks,
                "notifications" to initialNotifications
            )

            dbRef.child(request.phone).setValue(userMap).await()

            // Automatically log in the registered user
            val tokenResult = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()
            prefs.token = tokenResult?.token ?: "firebase_token_${request.phone}"
            prefs.phone = request.phone
            prefs.name = generatedName
            prefs.userId = userIdVal
            prefs.balance = 0.0f
            prefs.earningBalance = 0.0f
            prefs.bonus = signupBonus
            prefs.bonusUsed = 0.0f
            prefs.recharge = 0.0f
            prefs.vipLevel = 1
            prefs.referralCode = myReferralCode // save their own code in prefs to display
            prefs.accountPassword = request.password

            // Credit referral commission to the referrer
            if (!request.referralCode.isNullOrBlank()) {
                try {
                    val allUsersSnap = dbRef.awaitValue()
                    if (allUsersSnap.exists()) {
                        for (uChild in allUsersSnap.children) {
                            val theirRef = uChild.child("myReferralCode").getValue(String::class.java)
                            if (theirRef == request.referralCode || uChild.key == request.referralCode || uChild.child("userId").getValue(String::class.java) == request.referralCode) {
                                val regComm = settingsSnap.child("register_commission").getValue(Double::class.java)?.toFloat() ?: 50.0f
                                val oldBonus = uChild.child("bonus").getValue(Float::class.java) ?: 0.0f
                                dbRef.child(uChild.key!!).child("bonus").setValue(oldBonus + regComm).await()
                                
                                // Append transaction for referrer
                                val txSnap = uChild.child("transactions")
                                val txList = mutableListOf<TransactionModel>()
                                if (txSnap.exists()) {
                                    for (tx in txSnap.children) {
                                        txList.add(TransactionModel(
                                            id = tx.child("id").getValue(Int::class.java) ?: 0,
                                            type = tx.child("type").getValue(String::class.java) ?: "",
                                            amount = tx.child("amount").getValue(Float::class.java) ?: 0.0f,
                                            status = tx.child("status").getValue(String::class.java) ?: "",
                                            date = tx.child("date").getValue(String::class.java) ?: "",
                                            description = tx.child("description").getValue(String::class.java) ?: ""
                                        ))
                                    }
                                }
                                txList.add(0, TransactionModel(
                                    id = txList.size + 1,
                                    type = "bonus",
                                    amount = regComm,
                                    status = "Success",
                                    date = "Just now",
                                    description = "Referral Bonus for inviting ${request.phone}"
                                ))
                                dbRef.child(uChild.key!!).child("transactions").setValue(txList).await()
                                break
                            }
                        }
                    }
                } catch (e: Exception) {}
            }

            return ApiResponse("success", "Account created successfully!", "Registered")
        } catch (e: Exception) {
            return ApiResponse("error", e.message ?: "Registration failed", null)
        }
    }

    override suspend fun getPlans(type: String): ApiResponse<List<PlanModel>> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("plans")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<PlanModel>()

        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val plan = PlanModel(
                    id = child.child("id").getValue(Int::class.java) ?: 0,
                    name = child.child("name").getValue(String::class.java) ?: "",
                    price = child.child("price").getValue(Float::class.java) ?: 0.0f,
                    revenueDays = child.child("revenueDays").getValue(Int::class.java) ?: 0,
                    dailyEarnings = child.child("dailyEarnings").getValue(Float::class.java) ?: 0.0f,
                    totalRevenue = child.child("totalRevenue").getValue(Float::class.java) ?: 0.0f,
                    type = child.child("type").getValue(String::class.java) ?: ""
                )
                if (type == "all" || type.isEmpty() || plan.type == type) {
                    list.add(plan)
                }
            }
        }
        return ApiResponse("success", "Plans loaded", list)
    }

    override suspend fun getTeamStats(): ApiResponse<TeamStatsModel> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val sysRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("system_settings")
        val sysSnap = sysRef.awaitValue()
        val regComm = sysSnap.child("register_commission").getValue(Double::class.java)?.toFloat() ?: 50.0f
        val bizComm = sysSnap.child("business_commission").getValue(Double::class.java)?.toFloat() ?: 2.0f

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users")
        val ourSnapshot = dbRef.child(phone).awaitValue()
        val allUsersSnapshot = dbRef.awaitValue()

        val bonus = ourSnapshot.child("bonus").getValue(Float::class.java) ?: 0.0f
        val vipLevel = ourSnapshot.child("vipLevel").getValue(Int::class.java) ?: 1
        val ourReferralCode = ourSnapshot.child("referralCode").getValue(String::class.java) ?: phone
        val ourPhone = ourSnapshot.child("phone").getValue(String::class.java) ?: phone
        val ourUserId = ourSnapshot.child("userId").getValue(String::class.java) ?: ""
        val ourMyRef = ourSnapshot.child("myReferralCode").getValue(String::class.java) ?: phone

        var teamSize = 0
        var activeCount = 0
        var totalBizAmount = 0.0f
        var referralIncome = 0.00f
        val referredList = mutableListOf<ReferredUserModel>()

        if (allUsersSnapshot.exists()) {
            for (userChild in allUsersSnapshot.children) {
                val theirPhone = userChild.child("phone").getValue(String::class.java) ?: ""
                if (theirPhone == phone || theirPhone.isBlank()) continue

                val refUsed = userChild.child("referralCode").getValue(String::class.java) ?: ""
                if (refUsed.isNotBlank() && (refUsed == ourPhone || refUsed == ourUserId || refUsed == ourMyRef)) {
                    teamSize++
                    val theirRecharge = userChild.child("recharge").getValue(Float::class.java) ?: 0.0f
                    if (theirRecharge > 0.0f) {
                        activeCount++
                        totalBizAmount += theirRecharge
                    }
                    referralIncome += regComm

                    val theirName = userChild.child("name").getValue(String::class.java) ?: "Unnamed User"
                    val theirUserIdStr = userChild.child("userId").getValue(String::class.java) ?: ""
                    val theirVip = userChild.child("vipLevel").getValue(Int::class.java) ?: 1
                    val theirBalance = userChild.child("balance").getValue(Float::class.java) ?: 0.0f
                    val theirIsVerified = userChild.child("isVerified").getValue(Boolean::class.java)
                        ?: userChild.child("verified").getValue(Boolean::class.java)
                        ?: false

                    referredList.add(
                        ReferredUserModel(
                            phone = theirPhone,
                            name = theirName,
                            userId = theirUserIdStr,
                            vipLevel = theirVip,
                            recharge = theirRecharge,
                            balance = theirBalance,
                            isVerified = theirIsVerified
                        )
                    )
                }
            }
        }

        val stats = TeamStatsModel(
            teamSize = teamSize,
            teamRank = "VIP$vipLevel",
            totalIncome = bonus + referralIncome,
            registerTotal = teamSize,
            registerActive = activeCount,
            businessTotal = totalBizAmount.toInt(),
            businessActive = activeCount,
            registerCommission = regComm,
            registerIncome = referralIncome,
            businessCommission = bizComm,
            businessIncome = 0.00f,
            referredUsers = referredList
        )
        return ApiResponse("success", "Team stats loaded", stats)
    }

    override fun getTeamStatsFlow(): kotlinx.coroutines.flow.Flow<TeamStatsModel> = 
        kotlinx.coroutines.flow.callbackFlow {
        val phone = currentPhone
        if (phone.isBlank()) {
            awaitClose {}
            return@callbackFlow
        }
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                
                val ourSnapshot = snapshot.child(phone)
                if (!ourSnapshot.exists()) return

                // Fetch System Settings directly from Firebase for commissions
                FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("system_settings")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(settingsSnap: DataSnapshot) {
                            val regComm = settingsSnap.child("register_commission").getValue(Double::class.java)?.toFloat() ?: 50.0f
                            val bizComm = settingsSnap.child("business_commission").getValue(Double::class.java)?.toFloat() ?: 2.0f
                            
                            val bonus = ourSnapshot.child("bonus").getValue(Float::class.java) ?: 0.0f
                            val vipLevel = ourSnapshot.child("vipLevel").getValue(Int::class.java) ?: 1
                            val ourReferralCode = ourSnapshot.child("referralCode").getValue(String::class.java) ?: phone
                            val ourPhone = ourSnapshot.child("phone").getValue(String::class.java) ?: phone
                            val ourUserId = ourSnapshot.child("userId").getValue(String::class.java) ?: ""
                            val ourMyRef = ourSnapshot.child("myReferralCode").getValue(String::class.java) ?: phone
                            
                            val bonusUsed = ourSnapshot.child("bonusUsed").getValue(Float::class.java) ?: 0.0f
                            val legacySynced = ourSnapshot.child("legacyCommissionSynced").getValue(Boolean::class.java) ?: false
                            
                            var currentBonus = bonus
                            
                            var teamSize = 0
                            var activeCount = 0
                            var totalBizAmount = 0.0f
                            var referralIncome = 0.0f
                            var businessIncome = 0.0f
                            val referredList = mutableListOf<ReferredUserModel>()
                            
                            for (userChild in snapshot.children) {
                                val theirPhone = userChild.child("phone").getValue(String::class.java) ?: ""
                                if (theirPhone == phone || theirPhone.isBlank()) continue
                                
                                val refUsed = userChild.child("referralCode").getValue(String::class.java) ?: ""
                                if (refUsed.isNotBlank() && (refUsed == ourPhone || refUsed == ourUserId || refUsed == ourMyRef)) {
                                    teamSize++
                                    val theirRecharge = userChild.child("recharge").getValue(Float::class.java) ?: 0.0f
                                    if (theirRecharge > 0.0f) {
                                        activeCount++
                                        totalBizAmount += theirRecharge
                                        businessIncome += theirRecharge * (bizComm / 100.0f)
                                    }
                                    referralIncome += regComm // use dynamically fetched commission
                                    
                                    referredList.add(ReferredUserModel(
                                        phone = theirPhone,
                                        name = userChild.child("name").getValue(String::class.java) ?: "User",
                                        userId = userChild.child("userId").getValue(String::class.java) ?: "",
                                        vipLevel = userChild.child("vipLevel").getValue(Int::class.java) ?: 1,
                                        recharge = theirRecharge,
                                        balance = userChild.child("balance").getValue(Float::class.java) ?: 0.0f,
                                        isVerified = userChild.child("isVerified").getValue(Boolean::class.java) ?: false
                                    ))
                                }
                            }
                            
                            val stats = TeamStatsModel(
                                teamSize = teamSize,
                                teamRank = "VIP$vipLevel",
                                totalIncome = currentBonus + bonusUsed, // True total lifetime income
                                registerTotal = teamSize,
                                registerActive = activeCount,
                                businessTotal = totalBizAmount.toInt(),
                                businessActive = activeCount,
                                registerCommission = regComm,
                                registerIncome = referralIncome,
                                businessCommission = bizComm,
                                businessIncome = businessIncome,
                                referredUsers = referredList
                            )
                            trySend(stats)
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.addValueEventListener(listener)
        awaitClose { dbRef.removeEventListener(listener) }
    }

    override suspend fun getBlogs(): ApiResponse<List<BlogModel>> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("blogs")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<BlogModel>()

        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val blog = BlogModel(
                    id = child.child("id").getValue(Int::class.java) ?: 0,
                    title = child.child("title").getValue(String::class.java) ?: "",
                    excerpt = child.child("excerpt").getValue(String::class.java) ?: "",
                    content = child.child("content").getValue(String::class.java) ?: "",
                    imageUrl = child.child("imageUrl").getValue(String::class.java) ?: "",
                    date = child.child("date").getValue(String::class.java) ?: ""
                )
                list.add(blog)
            }
        }
        return ApiResponse("success", "Blogs loaded", list)
    }

    override fun getBlogsFlow(): kotlinx.coroutines.flow.Flow<List<BlogModel>> = kotlinx.coroutines.flow.callbackFlow {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("blogs")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BlogModel>()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val blog = BlogModel(
                            id = child.child("id").getValue(Int::class.java) ?: 0,
                            title = child.child("title").getValue(String::class.java) ?: "",
                            excerpt = child.child("excerpt").getValue(String::class.java) ?: "",
                            content = child.child("content").getValue(String::class.java) ?: "",
                            imageUrl = child.child("imageUrl").getValue(String::class.java) ?: "",
                            date = child.child("date").getValue(String::class.java) ?: ""
                        )
                        list.add(blog)
                    }
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignore or handle
            }
        }
        dbRef.addValueEventListener(listener)
        awaitClose {
            dbRef.removeEventListener(listener)
        }
    }

    override suspend fun createPendingDeposit(amount: Float, orderNo: String): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
        
        val newTx = mapOf(
            "id" to (System.currentTimeMillis() / 1000).toInt(),
            "type" to "deposit",
            "amount" to amount,
            "status" to "Pending",
            "date" to "Just now",
            "description" to "Deposit request",
            "orderNo" to orderNo
        )
        // Set under transactions/orderNo so webhook can update it or add new
        dbRef.child("transactions").child(orderNo).setValue(newTx).await()

        val globalFundRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("fund_history")
        val newFundKey = globalFundRef.push().key ?: java.util.UUID.randomUUID().toString()
        val fundMap = mapOf(
            "userId" to phone,
            "type" to "deposit",
            "amount" to amount,
            "status" to "Pending",
            "date" to System.currentTimeMillis().toString(),
            "description" to "Deposit request",
            "orderNo" to orderNo
        )
        globalFundRef.child(newFundKey).setValue(fundMap).await()

        return ApiResponse("success", "Pending deposit created", null)
    }

    override suspend fun processPaymentCallback(orderNo: String, status: String, amount: Float): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val db = FirebaseDatabase.getInstance(firebaseDatabaseUrl)
        val userRef = db.getReference("users").child(phone)
        val txRef = userRef.child("transactions").child(orderNo)

        val txSnapshot = txRef.awaitValue()
        if (!txSnapshot.exists()) {
            return ApiResponse("error", "Transaction not found", null)
        }

        val currentStatus = txSnapshot.child("status").getValue(String::class.java) ?: "Pending"
        if (currentStatus.equals("Success", ignoreCase = true)) {
            return ApiResponse("success", "Transaction already processed successfully", null)
        }

        if (status.equals("success", ignoreCase = true) || status.equals("pay_success", ignoreCase = true)) {
            // Update transaction status to Success
            txRef.child("status").setValue("Success").await()

            // Update user balance and recharge
            val userSnapshot = userRef.awaitValue()
            val oldBalance = userSnapshot.child("balance").getValue(Float::class.java) ?: 0.0f
            val oldRecharge = userSnapshot.child("recharge").getValue(Float::class.java) ?: 0.0f

            val newBalance = oldBalance + amount
            val newRecharge = oldRecharge + amount

            userRef.child("balance").setValue(newBalance).await()
            userRef.child("recharge").setValue(newRecharge).await()

            // Sync local pref cache immediately
            prefs.balance = newBalance
            prefs.recharge = newRecharge

            // Update global fund history
            try {
                val globalFundRef = db.getReference("fund_history")
                val fundSnapshot = globalFundRef.awaitValue()
                if (fundSnapshot.exists()) {
                    for (child in fundSnapshot.children) {
                        val fundOrderNo = child.child("orderNo").getValue(String::class.java)
                        if (fundOrderNo == orderNo) {
                            globalFundRef.child(child.key!!).child("status").setValue("Success").await()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore silent failure of fund_history update
            }

            return ApiResponse("success", "Payment processed and credited successfully", null)
        } else {
            txRef.child("status").setValue("Failed").await()
            try {
                val globalFundRef = db.getReference("fund_history")
                val fundSnapshot = globalFundRef.awaitValue()
                if (fundSnapshot.exists()) {
                    for (child in fundSnapshot.children) {
                        val fundOrderNo = child.child("orderNo").getValue(String::class.java)
                        if (fundOrderNo == orderNo) {
                            globalFundRef.child(child.key!!).child("status").setValue("Failed").await()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore silent failure
            }
            return ApiResponse("success", "Payment status updated to Failed", null)
        }
    }

    override suspend fun createDeposit(request: DepositRequest): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
        val snapshot = dbRef.awaitValue()

        val oldBalance = snapshot.child("balance").getValue(Float::class.java) ?: 0.0f
        val oldRecharge = snapshot.child("recharge").getValue(Float::class.java) ?: 0.0f

        val newBalance = oldBalance + request.amount
        val newRecharge = oldRecharge + request.amount

        // Save to Firebase
        dbRef.child("balance").setValue(newBalance).await()
        dbRef.child("recharge").setValue(newRecharge).await()

        // Sync local pref cache immediately
        prefs.balance = newBalance
        prefs.recharge = newRecharge

        // Append to transactions list
        val txSnapshot = dbRef.child("transactions").awaitValue()
        val transactionsList = mutableListOf<TransactionModel>()
        if (txSnapshot.exists()) {
            for (child in txSnapshot.children) {
                val tx = TransactionModel(
                    id = child.child("id").getValue(Int::class.java) ?: 0,
                    type = child.child("type").getValue(String::class.java) ?: "",
                    amount = child.child("amount").getValue(Float::class.java) ?: 0.0f,
                    status = child.child("status").getValue(String::class.java) ?: "",
                    date = child.child("date").getValue(String::class.java) ?: "",
                    description = child.child("description").getValue(String::class.java) ?: ""
                )
                transactionsList.add(tx)
            }
        }

        val newTx = TransactionModel(
            id = transactionsList.size + 1,
            type = "deposit",
            amount = request.amount,
            status = "Success",
            date = "Just now",
            description = "Deposit successful via UPI"
        )
        transactionsList.add(0, newTx)
        dbRef.child("transactions").setValue(transactionsList).await()

        val globalFundRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("fund_history")
        val newFundKey = globalFundRef.push().key ?: java.util.UUID.randomUUID().toString()
        val fundMap = mapOf(
            "userId" to phone,
            "type" to "deposit",
            "amount" to request.amount,
            "status" to "Success",
            "date" to System.currentTimeMillis().toString(),
            "description" to "Deposit successful via UPI"
        )
        globalFundRef.child(newFundKey).setValue(fundMap).await()

        // Fetch merchants upi_id from system settings
        val systemSettingsRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("system_settings")
        val settingsSnap = systemSettingsRef.awaitValue()
        val upiId = if (settingsSnap.exists()) {
            settingsSnap.child("upi_id").getValue(String::class.java) ?: "invexx@ybl"
        } else {
            "invexx@ybl"
        }

        try {
            addNotificationInDb("Deposit Successful! 💳", "Successfully deposited ₹${request.amount} to your INVEXX wallet via secure UPI gateway.")
        } catch (e: Exception) {}
        
        // Business commission distribution
        try {
            val myReferralCode = snapshot.child("referralCode").getValue(String::class.java) ?: ""
            if (myReferralCode.isNotBlank() && myReferralCode != phone && myReferralCode != snapshot.child("userId").getValue(String::class.java)) {
                val allUsersRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users")
                val allUsersSnap = allUsersRef.awaitValue()
                if (allUsersSnap.exists()) {
                    val bizCommPercent = settingsSnap.child("business_commission").getValue(Double::class.java)?.toFloat() ?: 2.0f
                    val commissionAmount = request.amount * (bizCommPercent / 100.0f)
                    for (uChild in allUsersSnap.children) {
                        val theirRef = uChild.child("myReferralCode").getValue(String::class.java)
                        if (theirRef == myReferralCode || uChild.key == myReferralCode || uChild.child("userId").getValue(String::class.java) == myReferralCode) {
                            val oldBonus = uChild.child("bonus").getValue(Float::class.java) ?: 0.0f
                            allUsersRef.child(uChild.key!!).child("bonus").setValue(oldBonus + commissionAmount).await()
                            
                            val txSnap = uChild.child("transactions")
                            val txList = mutableListOf<TransactionModel>()
                            if (txSnap.exists()) {
                                for (tx in txSnap.children) {
                                    txList.add(TransactionModel(
                                        id = tx.child("id").getValue(Int::class.java) ?: 0,
                                        type = tx.child("type").getValue(String::class.java) ?: "",
                                        amount = tx.child("amount").getValue(Float::class.java) ?: 0.0f,
                                        status = tx.child("status").getValue(String::class.java) ?: "",
                                        date = tx.child("date").getValue(String::class.java) ?: "",
                                        description = tx.child("description").getValue(String::class.java) ?: ""
                                    ))
                                }
                            }
                            txList.add(0, TransactionModel(
                                id = txList.size + 1,
                                type = "bonus",
                                amount = commissionAmount,
                                status = "Success",
                                date = "Just now",
                                description = "Business Commission from team deposit"
                            ))
                            allUsersRef.child(uChild.key!!).child("transactions").setValue(txList).await()
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {}

        val upiDeepLink = "upi://pay?pa=$upiId&pn=Invexx%20Wealth&am=${request.amount}&cu=INR&tn=Invexx%20Deposit"
        return ApiResponse("success", "Payment initiated", upiDeepLink)
    }

    override suspend fun createWithdrawal(request: WithdrawRequest): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
        val snapshot = dbRef.awaitValue()

        val earningBalance = snapshot.child("earning_balance").getValue(Float::class.java) ?: 0.0f
        if (request.amount > earningBalance) {
            return ApiResponse("error", "Insufficient earning balance. You can only withdraw your plan earnings.", null)
        }

        val newEarningBalance = earningBalance - request.amount
        dbRef.child("earning_balance").setValue(newEarningBalance).await()
        prefs.earningBalance = newEarningBalance

        val userId = snapshot.child("userId").getValue(String::class.java) ?: phone
        val orderNo = "WD_${userId}_${System.currentTimeMillis()}"
        
        val newTx = mapOf(
            "id" to (System.currentTimeMillis() / 1000).toInt(),
            "type" to "withdrawal",
            "amount" to request.amount,
            "status" to "Pending",
            "date" to "Just now",
            "description" to "Withdraw to bank: ${request.bankName} (${request.accountNumber})",
            "orderNo" to orderNo
        )
        dbRef.child("transactions").child(orderNo).setValue(newTx).await()

        val globalFundRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("fund_history")
        val newFundKey = globalFundRef.push().key ?: java.util.UUID.randomUUID().toString()
        val fundMap = mapOf(
            "userId" to phone,
            "type" to "withdrawal",
            "amount" to request.amount,
            "status" to "Pending",
            "date" to System.currentTimeMillis().toString(),
            "description" to "Withdraw to bank: ${request.bankName} (${request.accountNumber})",
            "orderNo" to orderNo
        )
        globalFundRef.child(newFundKey).setValue(fundMap).await()

        try {
            addNotificationInDb("Withdrawal Submitted! 💸", "Your withdrawal request of ₹${request.amount} to bank ${request.bankName} is pending verification.")
        } catch (e: Exception) {}

        return ApiResponse("success", "Withdrawal request submitted successfully", "Pending")
    }

    override suspend fun getOrders(): ApiResponse<List<OrderModel>> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone).child("orders")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<OrderModel>()

        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val order = OrderModel(
                    id = child.child("id").getValue(Int::class.java) ?: 0,
                    planName = child.child("planName").getValue(String::class.java) ?: "",
                    investAmount = child.child("investAmount").getValue(Float::class.java) ?: 0.0f,
                    dailyEarnings = child.child("dailyEarnings").getValue(Float::class.java) ?: 0.0f,
                    totalReturn = child.child("totalReturn").getValue(Float::class.java) ?: 0.0f,
                    startDate = child.child("startDate").getValue(String::class.java) ?: "",
                    endDate = child.child("endDate").getValue(String::class.java) ?: "",
                    status = child.child("status").getValue(String::class.java) ?: ""
                )
                list.add(order)
            }
        }
        return ApiResponse("success", "Orders loaded", list)
    }

    override fun getOrdersFlow(): kotlinx.coroutines.flow.Flow<List<OrderModel>> = kotlinx.coroutines.flow.callbackFlow {
        val phone = currentPhone
        if (phone.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl)
            .getReference("users").child(phone).child("orders")
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<OrderModel>()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val order = OrderModel(
                            id = child.child("id").getValue(Int::class.java) ?: 0,
                            planName = child.child("planName").getValue(String::class.java) ?: "",
                            investAmount = child.child("investAmount").getValue(Float::class.java) ?: 0f,
                            dailyEarnings = child.child("dailyEarnings").getValue(Float::class.java) ?: 0f,
                            totalReturn = child.child("totalReturn").getValue(Float::class.java) ?: 0f,
                            startDate = child.child("startDate").getValue(String::class.java) ?: "",
                            endDate = child.child("endDate").getValue(String::class.java) ?: "",
                            status = child.child("status").getValue(String::class.java) ?: ""
                        )
                        list.add(order)
                    }
                }
                trySend(list)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        dbRef.addValueEventListener(listener)
        awaitClose { dbRef.removeEventListener(listener) }
    }

    override suspend fun getTransactions(): ApiResponse<List<TransactionModel>> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone).child("transactions")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<TransactionModel>()

        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val tx = TransactionModel(
                    id = child.child("id").getValue(Int::class.java) ?: 0,
                    type = child.child("type").getValue(String::class.java) ?: "",
                    amount = child.child("amount").getValue(Float::class.java) ?: 0.0f,
                    status = child.child("status").getValue(String::class.java) ?: "",
                    date = child.child("date").getValue(String::class.java) ?: "",
                    description = child.child("description").getValue(String::class.java) ?: "",
                    orderNo = child.child("orderNo").getValue(String::class.java)
                )
                list.add(tx)
            }
        }
        list.sortByDescending { it.id }
        return ApiResponse("success", "Transactions loaded", list)
    }

    override fun getTransactionsFlow(): kotlinx.coroutines.flow.Flow<List<TransactionModel>> = 
        kotlinx.coroutines.flow.callbackFlow {
        val phone = currentPhone
        if (phone.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl)
            .getReference("users").child(phone).child("transactions")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<TransactionModel>()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        list.add(TransactionModel(
                            id = child.child("id").getValue(Int::class.java) ?: 0,
                            type = child.child("type").getValue(String::class.java) ?: "",
                            amount = child.child("amount").getValue(Float::class.java) ?: 0f,
                            status = child.child("status").getValue(String::class.java) ?: "",
                            date = child.child("date").getValue(String::class.java) ?: "",
                            description = child.child("description").getValue(String::class.java) ?: "",
                            orderNo = child.child("orderNo").getValue(String::class.java)
                        ))
                    }
                }
                list.sortByDescending { it.id }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.addValueEventListener(listener)
        awaitClose { dbRef.removeEventListener(listener) }
    }

    override fun getUserProfileFlow(): kotlinx.coroutines.flow.Flow<Map<String, Any>> = 
        kotlinx.coroutines.flow.callbackFlow {
        val phone = currentPhone
        if (phone.isBlank()) {
            trySend(emptyMap())
            awaitClose {}
            return@callbackFlow
        }
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl)
            .getReference("users").child(phone)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val map = mutableMapOf<String, Any>()
                
                snapshot.child("balance").getValue(Float::class.java)?.let { map["balance"] = it }
                snapshot.child("earning_balance").getValue(Float::class.java)?.let { map["earning_balance"] = it }
                snapshot.child("bonus").getValue(Float::class.java)?.let { map["bonus"] = it }
                snapshot.child("bonusUsed").getValue(Float::class.java)?.let { map["bonusUsed"] = it }
                snapshot.child("recharge").getValue(Float::class.java)?.let { map["recharge"] = it }
                snapshot.child("vipLevel").getValue(Int::class.java)?.let { map["vipLevel"] = it }
                snapshot.child("name").getValue(String::class.java)?.let { map["name"] = it }
                
                val holder = snapshot.child("bankHolderName").getValue(String::class.java)
                    ?: snapshot.child("bank").child("bankHolderName").getValue(String::class.java)
                val bName = snapshot.child("bankName").getValue(String::class.java)
                    ?: snapshot.child("bank").child("bankName").getValue(String::class.java)
                val accNum = snapshot.child("bankAccountNumber").getValue(String::class.java)
                    ?: snapshot.child("bank").child("bankAccountNumber").getValue(String::class.java)
                val ifsc = snapshot.child("bankIfscCode").getValue(String::class.java)
                    ?: snapshot.child("bank").child("bankIfscCode").getValue(String::class.java)
                val saved = snapshot.child("isBankDetailsSaved").getValue(Boolean::class.java)
                    ?: snapshot.child("bank").child("isBankDetailsSaved").getValue(Boolean::class.java)

                holder?.let { map["bankHolderName"] = it }
                bName?.let { map["bankName"] = it }
                accNum?.let { map["bankAccountNumber"] = it }
                ifsc?.let { map["bankIfscCode"] = it }
                saved?.let { map["isBankDetailsSaved"] = it }

                val myRef = snapshot.child("myReferralCode").getValue(String::class.java) ?: phone
                map["referralCode"] = myRef
                
                // Sync to prefs for offline cache
                (map["balance"] as? Float)?.let { prefs.balance = it }
                (map["earning_balance"] as? Float)?.let { prefs.earningBalance = it }
                (map["bonus"] as? Float)?.let { prefs.bonus = it }
                (map["bonusUsed"] as? Float)?.let { prefs.bonusUsed = it }
                (map["recharge"] as? Float)?.let { prefs.recharge = it }
                (map["vipLevel"] as? Int)?.let { prefs.vipLevel = it }
                (map["name"] as? String)?.let { prefs.name = it }
                (map["bankHolderName"] as? String)?.let { prefs.bankHolderName = it }
                (map["bankName"] as? String)?.let { prefs.bankName = it }
                (map["bankAccountNumber"] as? String)?.let { prefs.bankAccountNumber = it }
                (map["bankIfscCode"] as? String)?.let { prefs.bankIfscCode = it }
                (map["isBankDetailsSaved"] as? Boolean)?.let { prefs.isBankDetailsSaved = it }
                (map["referralCode"] as? String)?.let { prefs.referralCode = it }
                
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.addValueEventListener(listener)
        awaitClose { dbRef.removeEventListener(listener) }
    }

    override suspend fun redeemGiftCode(request: RedeemRequest): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val codeUpper = request.code.uppercase().trim()
        val giftCodesRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("gift_codes")
        var codeSnap = giftCodesRef.child(codeUpper).awaitValue()

        // If the entire gift_codes node is empty, seed "INVEXX100" as a default dynamic gift code
        val parentSnap = giftCodesRef.awaitValue()
        if (!parentSnap.exists()) {
            val defaultCodes = mapOf(
                "INVEXX100" to mapOf(
                    "amount" to 100.0f,
                    "active" to true,
                    "max_uses" to 1000,
                    "used_by" to mapOf<String, Boolean>()
                ),
                "TEST50" to mapOf(
                    "amount" to 50.0f,
                    "active" to true,
                    "max_uses" to 1,
                    "expiry" to "2026-06-26",
                    "used_by" to mapOf<String, Boolean>()
                )
            )
            giftCodesRef.setValue(defaultCodes).await()
            codeSnap = giftCodesRef.child(codeUpper).awaitValue()
        }

        if (!codeSnap.exists()) {
            return ApiResponse("error", "Invalid or expired gift code", null)
        }

        val active = codeSnap.child("active").getValue(Boolean::class.java) ?: true
        if (!active) {
            return ApiResponse("error", "This gift code is no longer active", null)
        }

        val amount = codeSnap.child("amount").getValue(Float::class.java) ?: 0.0f

        // Check if user already used this code
        val usedBySnap = codeSnap.child("used_by")
        if (usedBySnap.exists()) {
            for (child in usedBySnap.children) {
                if (child.key == phone || child.getValue(String::class.java) == phone) {
                    return ApiResponse("error", "You have already redeemed this gift code", null)
                }
            }
        }

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
        val snapshot = dbRef.awaitValue()

        val oldBonus = snapshot.child("bonus").getValue(Float::class.java) ?: 0.0f
        val newBonus = oldBonus + amount
        dbRef.child("bonus").setValue(newBonus).await()
        prefs.bonus = newBonus

        // Append transaction
        val txSnapshot = dbRef.child("transactions").awaitValue()
        val transactionsList = mutableListOf<TransactionModel>()
        if (txSnapshot.exists()) {
            for (child in txSnapshot.children) {
                val tx = TransactionModel(
                    id = child.child("id").getValue(Int::class.java) ?: 0,
                    type = child.child("type").getValue(String::class.java) ?: "",
                    amount = child.child("amount").getValue(Float::class.java) ?: 0.0f,
                    status = child.child("status").getValue(String::class.java) ?: "",
                    date = child.child("date").getValue(String::class.java) ?: "",
                    description = child.child("description").getValue(String::class.java) ?: ""
                )
                transactionsList.add(tx)
            }
        }
        val newTx = TransactionModel(
            id = transactionsList.size + 1,
            type = "bonus",
            amount = amount,
            status = "Success",
            date = "Just now",
            description = "Gift code $codeUpper redeemed"
        )
        transactionsList.add(0, newTx)
        dbRef.child("transactions").setValue(transactionsList).await()

        // Mark code as used by this user
        giftCodesRef.child(codeUpper).child("used_by").child(phone).setValue(true).await()

        val globalGiftRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("gift_redeem_history")
        val newGiftKey = globalGiftRef.push().key ?: java.util.UUID.randomUUID().toString()
        val giftMap = mapOf(
            "userId" to phone,
            "code" to codeUpper,
            "amount" to amount,
            "date" to System.currentTimeMillis().toString()
        )
        globalGiftRef.child(newGiftKey).setValue(giftMap).await()

        try {
            addNotificationInDb("Gift Code Redeemed! 🎁", "Successfully redeemed code: $codeUpper. ₹$amount has been added to your bonus balance.")
        } catch (e: Exception) {}

        return ApiResponse("success", "Code redeemed successfully! ₹$amount added to bonus.", "Success")
    }

    override suspend fun getVipBenefits(): ApiResponse<List<VipBenefitModel>> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("vip_benefits")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<VipBenefitModel>()

        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val benefit = VipBenefitModel(
                    level = child.child("level").getValue(String::class.java) ?: "",
                    requirement = child.child("requirement").getValue(String::class.java) ?: "",
                    benefitDesc = child.child("benefitDesc").getValue(String::class.java) ?: ""
                )
                list.add(benefit)
            }
        }
        return ApiResponse("success", "VIP benefits loaded", list)
    }

    override suspend fun getTasks(): ApiResponse<List<TaskModel>> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val globalDbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("tasks")
        val globalSnapshot = globalDbRef.awaitValue()
        val globalTasks = mutableListOf<TaskModel>()
        
        if (globalSnapshot.exists()) {
            for (child in globalSnapshot.children) {
                val tid = child.child("id").getValue(Int::class.java) ?: 0
                val title = child.child("title").getValue(String::class.java) ?: ""
                val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                val rewardAmount = child.child("rewardAmount").getValue(Float::class.java) ?: 0.0f
                globalTasks.add(TaskModel(tid, title, subtitle, 0f, false, false, rewardAmount))
            }
        }

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone).child("tasks")
        val snapshot = dbRef.awaitValue()
        val userTasksMap = mutableMapOf<Int, TaskModel>()

        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val tid = child.child("id").getValue(Int::class.java) ?: 0
                val progress = child.child("progress").getValue(Float::class.java) ?: 0.0f
                val isClaimed = child.child("claimed").getValue(Boolean::class.java) ?: child.child("isClaimed").getValue(Boolean::class.java) ?: false
                val isClaimable = child.child("claimable").getValue(Boolean::class.java) ?: child.child("isClaimable").getValue(Boolean::class.java) ?: false
                userTasksMap[tid] = TaskModel(tid, "", "", progress, isClaimed, isClaimable, 0f)
            }
        }
        
        val list = mutableListOf<TaskModel>()
        
        for (gTask in globalTasks) {
            val uTask = userTasksMap[gTask.id]
            if (uTask != null) {
                list.add(gTask.copy(progress = uTask.progress, isClaimed = uTask.isClaimed, isClaimable = uTask.isClaimable))
            } else {
                list.add(gTask)
            }
        }

        return ApiResponse("success", "Tasks loaded", list)
    }

    override suspend fun checkIn(): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
        val snapshot = dbRef.awaitValue()

        val tasksSnapshot = snapshot.child("tasks")
        val tasksList = mutableListOf<TaskModel>()
        var foundCheckIn = false

        if (tasksSnapshot.exists()) {
            for (child in tasksSnapshot.children) {
                val tid = child.child("id").getValue(Int::class.java) ?: 0
                val title = child.child("title").getValue(String::class.java) ?: ""
                val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                val progress = child.child("progress").getValue(Float::class.java) ?: 0.0f
                val isClaimed = child.child("claimed").getValue(Boolean::class.java) ?: child.child("isClaimed").getValue(Boolean::class.java) ?: false
                val isClaimable = child.child("claimable").getValue(Boolean::class.java) ?: child.child("isClaimable").getValue(Boolean::class.java) ?: false
                val rewardAmount = child.child("rewardAmount").getValue(Float::class.java) ?: 0.0f

                if (tid == 1 && !isClaimed && !isClaimable) {
                    tasksList.add(TaskModel(tid, title, subtitle, 1.0f, isClaimed, true, rewardAmount))
                    foundCheckIn = true
                } else {
                    tasksList.add(TaskModel(tid, title, subtitle, progress, isClaimed, isClaimable, rewardAmount))
                }
            }
        }

        if (foundCheckIn) {
            dbRef.child("tasks").setValue(tasksList).await()
            try {
                addNotificationInDb("Check-In Successful! 📅", "You have successfully checked in today. Your reward of ₹5.00 is ready to be claimed!")
            } catch (e: Exception) {}
            return ApiResponse("success", "Successfully checked in! Now you can claim your daily reward.", "Success")
        } else {
            return ApiResponse("error", "Check-in task already completed or claimed today.", null)
        }
    }

    override suspend fun claimTask(id: Int): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
        val snapshot = dbRef.awaitValue()

        val tasksSnapshot = snapshot.child("tasks")
        val tasksList = mutableListOf<TaskModel>()
        var reward = 0f
        var taskTitle = ""
        var claimedSome = false

        if (tasksSnapshot.exists()) {
            for (child in tasksSnapshot.children) {
                val tid = child.child("id").getValue(Int::class.java) ?: 0
                val title = child.child("title").getValue(String::class.java) ?: ""
                val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                val prog = child.child("progress").getValue(Float::class.java) ?: 0.0f
                var isCl = child.child("claimed").getValue(Boolean::class.java) ?: child.child("isClaimed").getValue(Boolean::class.java) ?: false
                var isCla = child.child("claimable").getValue(Boolean::class.java) ?: child.child("isClaimable").getValue(Boolean::class.java) ?: false
                val rwd = child.child("rewardAmount").getValue(Float::class.java) ?: 0.0f

                if (tid == id && isCla && !isCl) {
                    isCl = true
                    isCla = false
                    reward = rwd
                    taskTitle = title
                    claimedSome = true
                }
                tasksList.add(TaskModel(tid, title, subtitle, prog, isCl, isCla, rwd))
            }
        }

        if (claimedSome) {
            dbRef.child("tasks").setValue(tasksList).await()
            val oldBonus = snapshot.child("bonus").getValue(Float::class.java) ?: 0.0f
            val newBonus = oldBonus + reward
            dbRef.child("bonus").setValue(newBonus).await()
            prefs.bonus = newBonus

            // Append transaction
            val txSnapshot = dbRef.child("transactions").awaitValue()
            val transactionsList = mutableListOf<TransactionModel>()
            if (txSnapshot.exists()) {
                for (child in txSnapshot.children) {
                    val tx = TransactionModel(
                        id = child.child("id").getValue(Int::class.java) ?: 0,
                        type = child.child("type").getValue(String::class.java) ?: "",
                        amount = child.child("amount").getValue(Float::class.java) ?: 0.0f,
                        status = child.child("status").getValue(String::class.java) ?: "",
                        date = child.child("date").getValue(String::class.java) ?: "",
                        description = child.child("description").getValue(String::class.java) ?: ""
                    )
                    transactionsList.add(tx)
                }
            }
            val newTx = TransactionModel(
                id = transactionsList.size + 1,
                type = "bonus",
                amount = reward,
                status = "Success",
                date = "Just now",
                description = "Claimed task reward: $taskTitle"
            )
            transactionsList.add(0, newTx)
            dbRef.child("transactions").setValue(transactionsList).await()

            try {
                addNotificationInDb("Reward Claimed! 🎁", "Successfully claimed task reward of ₹$reward for completing: $taskTitle!")
            } catch (e: Exception) {}

            return ApiResponse("success", "Claimed ₹$reward successfully!", "Success")
        }

        return ApiResponse("error", "Task is not claimable or already claimed", null)
    }

    override suspend fun getNotifications(): ApiResponse<List<NotificationModel>> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone).child("notifications")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<NotificationModel>()

        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val notif = NotificationModel(
                    id = child.child("id").getValue(Int::class.java) ?: 0,
                    title = child.child("title").getValue(String::class.java) ?: "",
                    message = child.child("message").getValue(String::class.java) ?: "",
                    date = child.child("date").getValue(String::class.java) ?: "",
                    isRead = child.child("read").getValue(Boolean::class.java) ?: child.child("isRead").getValue(Boolean::class.java) ?: false
                )
                list.add(notif)
            }
        }
        return ApiResponse("success", "Notifications loaded", list)
    }

    override suspend fun markNotificationsAsRead(): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone).child("notifications")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<NotificationModel>()

        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val notif = NotificationModel(
                    id = child.child("id").getValue(Int::class.java) ?: 0,
                    title = child.child("title").getValue(String::class.java) ?: "",
                    message = child.child("message").getValue(String::class.java) ?: "",
                    date = child.child("date").getValue(String::class.java) ?: "",
                    isRead = true
                )
                list.add(notif)
            }
            dbRef.setValue(list).await()
        }
        return ApiResponse("success", "All notifications marked as read", "Success")
    }

    override suspend fun buyPlan(plan: PlanModel, amount: Float): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
        val snapshot = dbRef.awaitValue()

        val depositBalance = snapshot.child("balance").getValue(Float::class.java) ?: 0.0f
        val earningBalance = snapshot.child("earning_balance").getValue(Float::class.java) ?: 0.0f
        val bonus = snapshot.child("bonus").getValue(Float::class.java) ?: 0.0f
        val recharge = snapshot.child("recharge").getValue(Float::class.java) ?: 0.0f
        val bonusUsed = snapshot.child("bonusUsed").getValue(Float::class.java) ?: 0.0f

        val usableBonusTotal = recharge - bonusUsed
        val availableBonusForThis = if (usableBonusTotal > 0) minOf(bonus, usableBonusTotal) else 0.0f
        val bonusToUse = minOf(amount, availableBonusForThis)
        val remainingAmount = amount - bonusToUse

        if (depositBalance + earningBalance < remainingAmount) {
            return ApiResponse("error", "Insufficient balance. Available deposit & earnings: ₹${String.format("%.2f", depositBalance + earningBalance)}, Usable bonus: ₹${String.format("%.2f", bonusToUse)}", null)
        }

        var newDepositBalance = depositBalance
        var newEarningBalance = earningBalance
        var newBonus = bonus
        var newBonusUsed = bonusUsed

        if (bonusToUse > 0) {
            newBonus -= bonusToUse
            newBonusUsed += bonusToUse
        }

        if (remainingAmount > 0) {
            if (newDepositBalance >= remainingAmount) {
                newDepositBalance -= remainingAmount
            } else {
                val leftToPay = remainingAmount - newDepositBalance
                newDepositBalance = 0.0f
                newEarningBalance -= leftToPay
            }
        }

        dbRef.child("balance").setValue(newDepositBalance).await()
        dbRef.child("earning_balance").setValue(newEarningBalance).await()
        dbRef.child("bonus").setValue(newBonus).await()
        dbRef.child("bonusUsed").setValue(newBonusUsed).await()

        prefs.balance = newDepositBalance
        prefs.earningBalance = newEarningBalance
        prefs.bonus = newBonus
        prefs.bonusUsed = newBonusUsed

        // Append to orders
        val ordersSnapshot = dbRef.child("orders").awaitValue()
        val ordersList = mutableListOf<OrderModel>()
        if (ordersSnapshot.exists()) {
            for (child in ordersSnapshot.children) {
                val ord = OrderModel(
                    id = child.child("id").getValue(Int::class.java) ?: 0,
                    planName = child.child("planName").getValue(String::class.java) ?: "",
                    investAmount = child.child("investAmount").getValue(Float::class.java) ?: 0.0f,
                    dailyEarnings = child.child("dailyEarnings").getValue(Float::class.java) ?: 0.0f,
                    totalReturn = child.child("totalReturn").getValue(Float::class.java) ?: 0.0f,
                    startDate = child.child("startDate").getValue(String::class.java) ?: "",
                    endDate = child.child("endDate").getValue(String::class.java) ?: "",
                    status = child.child("status").getValue(String::class.java) ?: ""
                )
                ordersList.add(ord)
            }
        }
        val newOrder = OrderModel(
            id = ordersList.size + 101,
            planName = plan.name,
            investAmount = plan.price,
            dailyEarnings = plan.dailyEarnings,
            totalReturn = plan.totalRevenue,
            startDate = "Just now",
            endDate = "In ${plan.revenueDays} days",
            status = "Active"
        )
        ordersList.add(0, newOrder)
        dbRef.child("orders").setValue(ordersList).await()

        // Append transaction
        val txSnapshot = dbRef.child("transactions").awaitValue()
        val transactionsList = mutableListOf<TransactionModel>()
        if (txSnapshot.exists()) {
            for (child in txSnapshot.children) {
                val tx = TransactionModel(
                    id = child.child("id").getValue(Int::class.java) ?: 0,
                    type = child.child("type").getValue(String::class.java) ?: "",
                    amount = child.child("amount").getValue(Float::class.java) ?: 0.0f,
                    status = child.child("status").getValue(String::class.java) ?: "",
                    date = child.child("date").getValue(String::class.java) ?: "",
                    description = child.child("description").getValue(String::class.java) ?: ""
                )
                transactionsList.add(tx)
            }
        }
        val newTx = TransactionModel(
            id = transactionsList.size + 1,
            type = "investment",
            amount = amount,
            status = "Success",
            date = "Just now",
            description = "Invested in plan ${plan.name}"
        )
        transactionsList.add(0, newTx)
        dbRef.child("transactions").setValue(transactionsList).await()

        val globalOrderRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("order_history")
        val newOrderHistoryKey = globalOrderRef.push().key ?: java.util.UUID.randomUUID().toString()
        val orderMap = mapOf(
            "userId" to phone,
            "planId" to plan.id,
            "planName" to plan.name,
            "investAmount" to amount,
            "dailyEarnings" to plan.dailyEarnings,
            "totalReturn" to plan.totalRevenue,
            "date" to System.currentTimeMillis().toString()
        )
        globalOrderRef.child(newOrderHistoryKey).setValue(orderMap).await()

        try {
            addNotificationInDb("Investment Active! 💰", "Successfully invested ₹$amount in plan: ${plan.name}. Start tracking your daily returns!")
        } catch (e: Exception) {}

        return ApiResponse("success", "Successfully invested in plan ${plan.name}!", "Success")
    }

    override suspend fun updateProfileName(name: String): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
        dbRef.child("name").setValue(name).await()
        prefs.name = name

        try {
            addNotificationInDb("Profile Updated! 👤", "Your profile nickname has been updated to: $name.")
        } catch (e: Exception) {}

        return ApiResponse("success", "Successfully updated profile name!", "Success")
    }

    override suspend fun saveBankDetailsInDb(holder: String, bank: String, account: String, ifsc: String): ApiResponse<String> {
        val phone = currentPhone
        if (phone.isBlank()) return ApiResponse("error", "User not logged in", null)

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
        dbRef.child("bankHolderName").setValue(holder).await()
        dbRef.child("bankName").setValue(bank).await()
        dbRef.child("bankAccountNumber").setValue(account).await()
        dbRef.child("bankIfscCode").setValue(ifsc).await()
        dbRef.child("isBankDetailsSaved").setValue(true).await()

        // Also save nested under "bank" table/object inside the user's data
        val bankMap = mapOf(
            "bankHolderName" to holder,
            "bankName" to bank,
            "bankAccountNumber" to account,
            "bankIfscCode" to ifsc,
            "isBankDetailsSaved" to true
        )
        dbRef.child("bank").setValue(bankMap).await()

        try {
            addNotificationInDb("Bank Details Saved 🔒", "Your bank details (Bank: $bank, Acc: $account) have been locked and saved securely in cloud database.")
        } catch (e: Exception) {}

        return ApiResponse("success", "Bank details successfully saved to cloud database!", "Success")
    }

    private suspend fun addNotificationInDb(title: String, message: String) {
        val phone = currentPhone
        if (phone.isBlank()) return

        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone).child("notifications")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<Map<String, Any>>()
        
        var nextId = 1
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val nid = child.child("id").getValue(Int::class.java) ?: nextId
                val nTitle = child.child("title").getValue(String::class.java) ?: ""
                val nMessage = child.child("message").getValue(String::class.java) ?: ""
                val nDate = child.child("date").getValue(String::class.java) ?: ""
                val nRead = child.child("read").getValue(Boolean::class.java) ?: child.child("isRead").getValue(Boolean::class.java) ?: false
                val nTimestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                
                list.add(mapOf(
                    "id" to nid,
                    "title" to nTitle,
                    "message" to nMessage,
                    "date" to nDate,
                    "isRead" to nRead,
                    "timestamp" to nTimestamp
                ))
                if (nid >= nextId) nextId = nid + 1
            }
        }

        val newNotif = mapOf(
            "id" to nextId,
            "title" to title,
            "message" to message,
            "date" to "Just now",
            "isRead" to false,
            "timestamp" to System.currentTimeMillis()
        )
        list.add(0, newNotif) // Prepend at index 0 for newest notifications first
        dbRef.setValue(list).await()
    }

    override suspend fun checkUserExists(phone: String): ApiResponse<Boolean> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users")
        val snapshot = dbRef.child(phone).awaitValue()
        return ApiResponse("success", "User check", snapshot.exists())
    }

    override suspend fun resetPassword(phone: String, newPass: String): ApiResponse<String> {
        return try {
            val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
            val snapshot = dbRef.awaitValue()
            if (!snapshot.exists()) {
                return ApiResponse("error", "User not found", null)
            }
            
            val oldPassword = snapshot.child("password").getValue(String::class.java)
            if (oldPassword != null) {
                val authEmail = "${phone}@invexx.app"
                try {
                    // Authenticate with old password
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(authEmail, oldPassword).await()
                    // Update to new password
                    FirebaseAuth.getInstance().currentUser?.updatePassword(newPass)?.await()
                } catch (e: Exception) {
                    // Ignore auth exceptions, we must update RTDB anyway
                }
            }
            
            // Update RTDB
            dbRef.child("password").setValue(newPass).await()
            if (prefs.phone == phone) {
                prefs.accountPassword = newPass
            }
            
            ApiResponse("success", "Password reset successfully", "Success")
        } catch (e: Exception) {
            ApiResponse("error", "Failed to reset password: ${e.message}", null)
        }
    }

    override suspend fun adminGetAllUsers(): ApiResponse<List<Map<String, Any>>> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<Map<String, Any>>()
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val userVal = child.value as? Map<String, Any>
                if (userVal != null) {
                    list.add(userVal)
                }
            }
        }
        return ApiResponse("success", "Users loaded successfully", list)
    }

    override suspend fun adminUpdateUser(phone: String, updates: Map<String, Any>): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(phone)
        dbRef.updateChildren(updates).await()
        return ApiResponse("success", "User updated successfully", "Success")
    }

    override suspend fun adminAddPlan(plan: PlanModel): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("plans")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<PlanModel>()
        var maxId = 0
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val pid = child.child("id").getValue(Int::class.java) ?: 0
                val pName = child.child("name").getValue(String::class.java) ?: ""
                val pPrice = child.child("price").getValue(Float::class.java) ?: 0f
                val pRev = child.child("revenueDays").getValue(Int::class.java) ?: 0
                val pDaily = child.child("dailyEarnings").getValue(Float::class.java) ?: 0f
                val pTot = child.child("totalRevenue").getValue(Float::class.java) ?: 0f
                val pType = child.child("type").getValue(String::class.java) ?: ""
                val pIcon = child.child("iconUrl").getValue(String::class.java)
                list.add(PlanModel(pid, pName, pPrice, pRev, pDaily, pTot, pType, pIcon))
                if (pid > maxId) maxId = pid
            }
        }
        val newPlan = plan.copy(id = maxId + 1)
        list.add(newPlan)
        dbRef.setValue(list).await()
        return ApiResponse("success", "Plan added successfully", "Success")
    }

    override suspend fun adminUpdatePlan(plan: PlanModel): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("plans")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<PlanModel>()
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val pid = child.child("id").getValue(Int::class.java) ?: 0
                if (pid == plan.id) {
                    list.add(plan)
                } else {
                    val pName = child.child("name").getValue(String::class.java) ?: ""
                    val pPrice = child.child("price").getValue(Float::class.java) ?: 0f
                    val pRev = child.child("revenueDays").getValue(Int::class.java) ?: 0
                    val pDaily = child.child("dailyEarnings").getValue(Float::class.java) ?: 0f
                    val pTot = child.child("totalRevenue").getValue(Float::class.java) ?: 0f
                    val pType = child.child("type").getValue(String::class.java) ?: ""
                    val pIcon = child.child("iconUrl").getValue(String::class.java)
                    list.add(PlanModel(pid, pName, pPrice, pRev, pDaily, pTot, pType, pIcon))
                }
            }
        }
        dbRef.setValue(list).await()
        return ApiResponse("success", "Plan updated successfully", "Success")
    }

    override suspend fun adminDeletePlan(id: Int): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("plans")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<PlanModel>()
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val pid = child.child("id").getValue(Int::class.java) ?: 0
                if (pid != id) {
                    val pName = child.child("name").getValue(String::class.java) ?: ""
                    val pPrice = child.child("price").getValue(Float::class.java) ?: 0f
                    val pRev = child.child("revenueDays").getValue(Int::class.java) ?: 0
                    val pDaily = child.child("dailyEarnings").getValue(Float::class.java) ?: 0f
                    val pTot = child.child("totalRevenue").getValue(Float::class.java) ?: 0f
                    val pType = child.child("type").getValue(String::class.java) ?: ""
                    val pIcon = child.child("iconUrl").getValue(String::class.java)
                    list.add(PlanModel(pid, pName, pPrice, pRev, pDaily, pTot, pType, pIcon))
                }
            }
        }
        dbRef.setValue(list).await()
        return ApiResponse("success", "Plan deleted successfully", "Success")
    }

    override suspend fun adminAddBlog(blog: BlogModel): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("blogs")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<BlogModel>()
        var maxId = 0
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val bid = child.child("id").getValue(Int::class.java) ?: 0
                val bTitle = child.child("title").getValue(String::class.java) ?: ""
                val bExc = child.child("excerpt").getValue(String::class.java) ?: ""
                val bCont = child.child("content").getValue(String::class.java) ?: ""
                val bImg = child.child("imageUrl").getValue(String::class.java) ?: ""
                val bDate = child.child("date").getValue(String::class.java) ?: ""
                list.add(BlogModel(bid, bTitle, bExc, bCont, bImg, bDate))
                if (bid > maxId) maxId = bid
            }
        }
        val newBlog = blog.copy(id = maxId + 1)
        list.add(newBlog)
        dbRef.setValue(list).await()
        return ApiResponse("success", "Blog added successfully", "Success")
    }

    override suspend fun adminUpdateBlog(blog: BlogModel): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("blogs")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<BlogModel>()
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val bid = child.child("id").getValue(Int::class.java) ?: 0
                if (bid == blog.id) {
                    list.add(blog)
                } else {
                    val bTitle = child.child("title").getValue(String::class.java) ?: ""
                    val bExc = child.child("excerpt").getValue(String::class.java) ?: ""
                    val bCont = child.child("content").getValue(String::class.java) ?: ""
                    val bImg = child.child("imageUrl").getValue(String::class.java) ?: ""
                    val bDate = child.child("date").getValue(String::class.java) ?: ""
                    list.add(BlogModel(bid, bTitle, bExc, bCont, bImg, bDate))
                }
            }
        }
        dbRef.setValue(list).await()
        return ApiResponse("success", "Blog updated successfully", "Success")
    }

    override suspend fun adminDeleteBlog(id: Int): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("blogs")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<BlogModel>()
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val bid = child.child("id").getValue(Int::class.java) ?: 0
                if (bid != id) {
                    val bTitle = child.child("title").getValue(String::class.java) ?: ""
                    val bExc = child.child("excerpt").getValue(String::class.java) ?: ""
                    val bCont = child.child("content").getValue(String::class.java) ?: ""
                    val bImg = child.child("imageUrl").getValue(String::class.java) ?: ""
                    val bDate = child.child("date").getValue(String::class.java) ?: ""
                    list.add(BlogModel(bid, bTitle, bExc, bCont, bImg, bDate))
                }
            }
        }
        dbRef.setValue(list).await()
        return ApiResponse("success", "Blog deleted successfully", "Success")
    }

    override suspend fun adminAddTask(task: TaskModel): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("tasks")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<TaskModel>()
        var maxId = 0
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val tid = child.child("id").getValue(Int::class.java) ?: 0
                val title = child.child("title").getValue(String::class.java) ?: ""
                val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                val progress = child.child("progress").getValue(Float::class.java) ?: 0.0f
                val isClaimed = child.child("claimed").getValue(Boolean::class.java) ?: child.child("isClaimed").getValue(Boolean::class.java) ?: false
                val isClaimable = child.child("claimable").getValue(Boolean::class.java) ?: child.child("isClaimable").getValue(Boolean::class.java) ?: false
                val rewardAmount = child.child("rewardAmount").getValue(Float::class.java) ?: 0.0f
                list.add(TaskModel(tid, title, subtitle, progress, isClaimed, isClaimable, rewardAmount))
                if (tid > maxId) maxId = tid
            }
        }
        val newTask = task.copy(id = maxId + 1)
        list.add(newTask)
        dbRef.setValue(list).await()
        return ApiResponse("success", "Task added successfully", "Success")
    }

    override suspend fun adminUpdateTask(task: TaskModel): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("tasks")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<TaskModel>()
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val tid = child.child("id").getValue(Int::class.java) ?: 0
                if (tid == task.id) {
                    list.add(task)
                } else {
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                    val progress = child.child("progress").getValue(Float::class.java) ?: 0.0f
                    val isClaimed = child.child("claimed").getValue(Boolean::class.java) ?: child.child("isClaimed").getValue(Boolean::class.java) ?: false
                    val isClaimable = child.child("claimable").getValue(Boolean::class.java) ?: child.child("isClaimable").getValue(Boolean::class.java) ?: false
                    val rewardAmount = child.child("rewardAmount").getValue(Float::class.java) ?: 0.0f
                    list.add(TaskModel(tid, title, subtitle, progress, isClaimed, isClaimable, rewardAmount))
                }
            }
        }
        dbRef.setValue(list).await()
        return ApiResponse("success", "Task updated successfully", "Success")
    }

    override suspend fun adminDeleteTask(id: Int): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("tasks")
        val snapshot = dbRef.awaitValue()
        val list = mutableListOf<TaskModel>()
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val tid = child.child("id").getValue(Int::class.java) ?: 0
                if (tid != id) {
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                    val progress = child.child("progress").getValue(Float::class.java) ?: 0.0f
                    val isClaimed = child.child("claimed").getValue(Boolean::class.java) ?: child.child("isClaimed").getValue(Boolean::class.java) ?: false
                    val isClaimable = child.child("claimable").getValue(Boolean::class.java) ?: child.child("isClaimable").getValue(Boolean::class.java) ?: false
                    val rewardAmount = child.child("rewardAmount").getValue(Float::class.java) ?: 0.0f
                    list.add(TaskModel(tid, title, subtitle, progress, isClaimed, isClaimable, rewardAmount))
                }
            }
        }
        dbRef.setValue(list).await()
        return ApiResponse("success", "Task deleted successfully", "Success")
    }

    override suspend fun adminProcessTransaction(userPhone: String, txId: Int, newStatus: String): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users").child(userPhone)
        val snapshot = dbRef.awaitValue()
        if (!snapshot.exists()) return ApiResponse("error", "User not found", null)

        val txSnapshot = snapshot.child("transactions")
        val txList = mutableListOf<TransactionModel>()
        var refundAmount = 0f
        var transactionType = ""

        if (txSnapshot.exists()) {
            for (child in txSnapshot.children) {
                val tid = child.child("id").getValue(Int::class.java) ?: 0
                val type = child.child("type").getValue(String::class.java) ?: ""
                val amount = child.child("amount").getValue(Float::class.java) ?: 0f
                val status = child.child("status").getValue(String::class.java) ?: ""
                val date = child.child("date").getValue(String::class.java) ?: ""
                val description = child.child("description").getValue(String::class.java) ?: ""

                if (tid == txId) {
                    transactionType = type
                    txList.add(TransactionModel(tid, type, amount, newStatus, date, description))
                    if (type == "withdrawal" && newStatus == "Failed" && status == "Pending") {
                        refundAmount = amount
                    }
                } else {
                    txList.add(TransactionModel(tid, type, amount, status, date, description))
                }
            }
        }

        dbRef.child("transactions").setValue(txList).await()

        if (refundAmount > 0f) {
            val oldBalance = snapshot.child("balance").getValue(Float::class.java) ?: 0f
            val newBalance = oldBalance + refundAmount
            dbRef.child("balance").setValue(newBalance).await()
        }

        try {
            val notifTitle = if (newStatus == "Success") "Transaction Approved! ✅" else "Transaction Rejected! ❌"
            val notifMessage = "Your $transactionType request of ID #$txId has been processed as: $newStatus."
            val userNotifRef = dbRef.child("notifications")
            val userNotifsSnapshot = userNotifRef.awaitValue()
            val list = mutableListOf<Map<String, Any>>()
            var nextId = 1
            if (userNotifsSnapshot.exists()) {
                for (child in userNotifsSnapshot.children) {
                    val nid = child.child("id").getValue(Int::class.java) ?: nextId
                    list.add(mapOf(
                        "id" to nid,
                        "title" to (child.child("title").getValue(String::class.java) ?: ""),
                        "message" to (child.child("message").getValue(String::class.java) ?: ""),
                        "date" to (child.child("date").getValue(String::class.java) ?: ""),
                        "isRead" to (child.child("isRead").getValue(Boolean::class.java) ?: false),
                        "timestamp" to (child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis())
                    ))
                    if (nid >= nextId) nextId = nid + 1
                }
            }
            list.add(0, mapOf(
                "id" to nextId,
                "title" to notifTitle,
                "message" to notifMessage,
                "date" to "Just now",
                "isRead" to false,
                "timestamp" to System.currentTimeMillis()
            ))
            userNotifRef.setValue(list).await()
        } catch (e: Exception) {}

        return ApiResponse("success", "Transaction processed successfully", "Success")
    }

    override suspend fun adminTriggerMidnightReturns(): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("users")
        val usersSnapshot = dbRef.awaitValue()
        var updatedUsersCount = 0

        if (usersSnapshot.exists()) {
            for (userChild in usersSnapshot.children) {
                val phone = userChild.child("phone").getValue(String::class.java) ?: continue
                val ordersSnapshot = userChild.child("orders")
                var totalEarningsToAdd = 0f
                val ordersList = mutableListOf<OrderModel>()
                var updatedOrders = false

                if (ordersSnapshot.exists()) {
                    for (orderChild in ordersSnapshot.children) {
                        val oid = orderChild.child("id").getValue(Int::class.java) ?: 0
                        val planName = orderChild.child("planName").getValue(String::class.java) ?: ""
                        val investAmount = orderChild.child("investAmount").getValue(Float::class.java) ?: 0f
                        val dailyEarnings = orderChild.child("dailyEarnings").getValue(Float::class.java) ?: 0f
                        val totalReturn = orderChild.child("totalReturn").getValue(Float::class.java) ?: 0f
                        val startDate = orderChild.child("startDate").getValue(String::class.java) ?: ""
                        val endDate = orderChild.child("endDate").getValue(String::class.java) ?: ""
                        val status = orderChild.child("status").getValue(String::class.java) ?: ""

                        if (status == "Active") {
                            totalEarningsToAdd += dailyEarnings
                            ordersList.add(OrderModel(oid, planName, investAmount, dailyEarnings, totalReturn + dailyEarnings, startDate, endDate, status))
                            updatedOrders = true
                        } else {
                            ordersList.add(OrderModel(oid, planName, investAmount, dailyEarnings, totalReturn, startDate, endDate, status))
                        }
                    }
                }

                if (totalEarningsToAdd > 0f) {
                    val userRef = dbRef.child(phone)
                    val currentEarningBalance = userChild.child("earning_balance").getValue(Float::class.java) ?: 0f
                    val newEarningBalance = currentEarningBalance + totalEarningsToAdd
                    userRef.child("earning_balance").setValue(newEarningBalance).await()

                    if (updatedOrders) {
                        userRef.child("orders").setValue(ordersList).await()
                    }

                    val txSnapshot = userChild.child("transactions")
                    val txList = mutableListOf<TransactionModel>()
                    if (txSnapshot.exists()) {
                        for (txChild in txSnapshot.children) {
                            txList.add(TransactionModel(
                                id = txChild.child("id").getValue(Int::class.java) ?: 0,
                                type = txChild.child("type").getValue(String::class.java) ?: "",
                                amount = txChild.child("amount").getValue(Float::class.java) ?: 0f,
                                status = txChild.child("status").getValue(String::class.java) ?: "",
                                date = txChild.child("date").getValue(String::class.java) ?: "",
                                description = txChild.child("description").getValue(String::class.java) ?: ""
                            ))
                        }
                    }
                    val nextTxId = txList.size + 1
                    txList.add(0, TransactionModel(
                        id = nextTxId,
                        type = "earnings",
                        amount = totalEarningsToAdd,
                        status = "Success",
                        date = "Midnight Daily Return",
                        description = "Daily earnings added for your active investments"
                    ))
                    userRef.child("transactions").setValue(txList).await()

                    val userNotifRef = userRef.child("notifications")
                    val userNotifsSnapshot = userNotifRef.awaitValue()
                    val notifList = mutableListOf<Map<String, Any>>()
                    var nextNotifId = 1
                    if (userNotifsSnapshot.exists()) {
                        for (notifChild in userNotifsSnapshot.children) {
                            val nid = notifChild.child("id").getValue(Int::class.java) ?: nextNotifId
                            notifList.add(mapOf(
                                "id" to nid,
                                "title" to (notifChild.child("title").getValue(String::class.java) ?: ""),
                                "message" to (notifChild.child("message").getValue(String::class.java) ?: ""),
                                "date" to (notifChild.child("date").getValue(String::class.java) ?: ""),
                                "isRead" to (notifChild.child("isRead").getValue(Boolean::class.java) ?: false),
                                "timestamp" to (notifChild.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis())
                            ))
                            if (nid >= nextNotifId) nextNotifId = nid + 1
                        }
                    }
                    notifList.add(0, mapOf(
                        "id" to nextNotifId,
                        "title" to "Daily Return Added! 💰",
                        "message" to "Congratulations! ₹${totalEarningsToAdd} daily return has been successfully credited to your wallet.",
                        "date" to "Just now",
                        "isRead" to false,
                        "timestamp" to System.currentTimeMillis()
                    ))
                    userNotifRef.setValue(notifList).await()

                    updatedUsersCount++
                }
            }
        }
        return ApiResponse("success", "Midnight returns calculated and added for $updatedUsersCount users successfully!", "Success")
    }

    override suspend fun getSystemSettings(): ApiResponse<Map<String, Any>> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("system_settings")
        val snapshot = dbRef.awaitValue()
        val map = mutableMapOf<String, Any>()
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val key = child.key ?: continue
                val value = child.value ?: continue
                map[key] = value
            }
        } else {
            val defaultSettings = mapOf(
                "minVersion" to "1.0.0",
                "latestVersion" to "1.0.0",
                "updateUrl" to "https://invexx.app/update",
                "upsetScreenEnabled" to false,
                "upsetMessage" to "We are undergoing scheduled maintenance. The app will be fully functional shortly. We apologize for the inconvenience.",
                "telegram_url" to "https://t.me/invexx_official",
                "whatsapp_url" to "https://wa.me/919999999999",
                "support_email" to "support@invexx-wealth.com",
                "upi_id" to "invexx@ybl",
                "signup_bonus" to 46.0,
                "register_commission" to 50.0,
                "business_commission" to 2.0
            )
            dbRef.setValue(defaultSettings).await()
            return ApiResponse("success", "System settings loaded with defaults", defaultSettings)
        }
        return ApiResponse("success", "System settings loaded successfully", map)
    }

    override suspend fun updateSystemSettings(settings: Map<String, Any>): ApiResponse<String> {
        val dbRef = FirebaseDatabase.getInstance(firebaseDatabaseUrl).getReference("system_settings")
        dbRef.setValue(settings).await()
        return ApiResponse("success", "System settings updated successfully", "Success")
    }
}
