package com.example.data

import android.content.Context
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<UserModel>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<String>

    @GET("plans")
    suspend fun getPlans(@Query("type") type: String): ApiResponse<List<PlanModel>>

    @GET("team/stats")
    suspend fun getTeamStats(): ApiResponse<TeamStatsModel>

    fun getTeamStatsFlow(): kotlinx.coroutines.flow.Flow<TeamStatsModel>

    @GET("blogs")
    suspend fun getBlogs(): ApiResponse<List<BlogModel>>

    fun getBlogsFlow(): kotlinx.coroutines.flow.Flow<List<BlogModel>>

    @POST("deposit/create")
    suspend fun createDeposit(@Body request: DepositRequest): ApiResponse<String>

    suspend fun createPendingDeposit(amount: Float, orderNo: String): ApiResponse<String>

    suspend fun processPaymentCallback(orderNo: String, status: String, amount: Float): ApiResponse<String>

    @POST("withdraw/request")
    suspend fun createWithdrawal(@Body request: WithdrawRequest): ApiResponse<String>

    @GET("orders")
    suspend fun getOrders(): ApiResponse<List<OrderModel>>

    fun getOrdersFlow(): kotlinx.coroutines.flow.Flow<List<OrderModel>>

    @GET("fund/history")
    suspend fun getTransactions(): ApiResponse<List<TransactionModel>>

    fun getTransactionsFlow(): kotlinx.coroutines.flow.Flow<List<TransactionModel>>

    @POST("gift/redeem")
    suspend fun redeemGiftCode(@Body request: RedeemRequest): ApiResponse<String>

    @GET("tasks")
    suspend fun getTasks(): ApiResponse<List<TaskModel>>

    @POST("tasks/{id}/claim")
    suspend fun claimTask(@Path("id") id: Int): ApiResponse<String>

    @POST("tasks/checkin")
    suspend fun checkIn(): ApiResponse<String>

    @GET("notifications")
    suspend fun getNotifications(): ApiResponse<List<NotificationModel>>

    @POST("notifications/read-all")
    suspend fun markNotificationsAsRead(): ApiResponse<String>

    @POST("plans/buy")
    suspend fun buyPlan(@Body plan: PlanModel, @Query("amount") amount: Float): ApiResponse<String>

    @POST("user/update-name")
    suspend fun updateProfileName(@Query("name") name: String): ApiResponse<String>

    @POST("user/save-bank")
    suspend fun saveBankDetailsInDb(
        @Query("holder") holder: String,
        @Query("bank") bank: String,
        @Query("account") account: String,
        @Query("ifsc") ifsc: String
    ): ApiResponse<String>

    suspend fun getVipBenefits(): ApiResponse<List<VipBenefitModel>>
    
    suspend fun checkUserExists(phone: String): ApiResponse<Boolean>
    suspend fun resetPassword(phone: String, newPass: String): ApiResponse<String>

    // Administrative Control APIs
    suspend fun adminGetAllUsers(): ApiResponse<List<Map<String, Any>>>
    suspend fun adminUpdateUser(phone: String, updates: Map<String, Any>): ApiResponse<String>
    suspend fun adminAddPlan(plan: PlanModel): ApiResponse<String>
    suspend fun adminUpdatePlan(plan: PlanModel): ApiResponse<String>
    suspend fun adminDeletePlan(id: Int): ApiResponse<String>
    suspend fun adminAddBlog(blog: BlogModel): ApiResponse<String>
    suspend fun adminUpdateBlog(blog: BlogModel): ApiResponse<String>
    suspend fun adminDeleteBlog(id: Int): ApiResponse<String>
    
    suspend fun adminAddTask(task: TaskModel): ApiResponse<String>
    suspend fun adminUpdateTask(task: TaskModel): ApiResponse<String>
    suspend fun adminDeleteTask(id: Int): ApiResponse<String>
    
    suspend fun adminProcessTransaction(userPhone: String, txId: Int, newStatus: String): ApiResponse<String>
    suspend fun adminTriggerMidnightReturns(): ApiResponse<String>
    suspend fun getSystemSettings(): ApiResponse<Map<String, Any>>
    suspend fun updateSystemSettings(settings: Map<String, Any>): ApiResponse<String>
    fun getUserProfileFlow(): kotlinx.coroutines.flow.Flow<Map<String, Any>>
}

/**
 * High-fidelity in-memory Mock API implementation to provide offline-first support.
 * This guarantees the user has a 100% interactive, reliable demo showing exact visual states
 * requested in their mockup, and handles investments, withdrawals, task claiming, etc.
 */
class MockApiService(private val context: Context) : ApiService {
    private val prefs = PreferenceManager(context)

    // In-memory state
    private val plans = mutableListOf(
        PlanModel(1, "Copper", 159.00f, 19, 24.00f, 456.00f, "fixed_fund"),
        PlanModel(2, "GOLD", 200.00f, 10, 25.00f, 250.00f, "fixed_fund"),
        PlanModel(3, "Silver", 200.00f, 35, 29.00f, 1015.00f, "fixed_fund"),
        PlanModel(4, "Platinum", 500.00f, 40, 75.00f, 3000.00f, "fixed_fund"),
        
        PlanModel(5, "Welfare Tier 1", 100.00f, 5, 25.00f, 125.00f, "welfare_fund"),
        PlanModel(6, "Welfare Tier 2", 300.00f, 7, 85.00f, 595.00f, "welfare_fund"),
        
        PlanModel(7, "Yearly Premium", 1200.00f, 365, 10.00f, 3650.00f, "yearly_fund"),
        PlanModel(8, "Yearly Ultra", 2500.00f, 365, 25.00f, 9125.00f, "yearly_fund")
    )

    private val orders = mutableListOf<OrderModel>(
        OrderModel(101, "Copper", 159.00f, 24.00f, 456.00f, "2026-06-20", "2026-07-09", "Active")
    )

    private val transactions = mutableListOf(
        TransactionModel(1, "deposit", 100.00f, "Success", "2026-06-23 14:30", "Recharge wallet"),
        TransactionModel(2, "commission", 2.00f, "Success", "2026-06-23 18:45", "Referral Level 1 commission"),
        TransactionModel(3, "bonus", 46.00f, "Success", "2026-06-22 09:00", "Sign up bonus balance")
    )

    private val tasks = mutableListOf(
        TaskModel(1, "Daily Check-In", "Get daily active login rewards", 1.0f, isClaimed = false, isClaimable = true, 5.00f),
        TaskModel(2, "Invite 1 Friend", "Invite a friend to register and deposit", 0.5f, isClaimed = false, isClaimable = false, 50.00f),
        TaskModel(3, "First Investment", "Purchase your first investment plan", 1.0f, isClaimed = true, isClaimable = false, 10.00f)
    )

    private val notifications = mutableListOf(
        NotificationModel(1, "Welcome to INVEXX!", "Start your investment journey with our Fixed, Welfare, and Yearly plans. Earn steady daily revenue!", "June 24, 2026", isRead = false),
        NotificationModel(2, "Referral Bonus Credited", "You received a Level 1 referral commission of ₹2.00 in your balance.", "June 23, 2026", isRead = false),
        NotificationModel(3, "Secure Account Check", "Your security shield is active. Keep your credentials safe.", "June 22, 2026", isRead = true)
    )

    private val blogs = listOf(
        BlogModel(
            id = 1,
            title = "Invexx Platform Official Launch",
            excerpt = "We are excited to announce the official launch of Invexx platform. A new journey of investment, growth and earnings begins now.",
            content = "We are absolutely thrilled to officially launch the INVEXX Investment Platform. Built on a foundation of security, transparency, and sustainable growth, INVEXX offers a premium suite of digital funds tailored for investors worldwide. With Fixed Funds, Welfare options, and robust Yearly Growth structures, INVEXX bridges the gap between everyday wealth and professional-grade yield generation. Thank you for joining us on Day 1—this is where your financial freedom matures. Invest, Earn, and Grow with INVEXX!",
            imageUrl = "https://picsum.photos/seed/invexx1/400/300",
            date = "April 24, 2025"
        ),
        BlogModel(
            id = 2,
            title = "Higher Returns with Fixed Funds",
            excerpt = "Our Fixed Fund plans are designed to give you stable daily earnings and long term financial growth.",
            content = "For investors seeking stability and lock-in guarantees, the Fixed Funds remain our flagship product. Our plans like Copper, Silver, and Gold provide immediate, structured, daily payouts with clear contract periods ranging from 10 to 35 days. By locking in strategic high-frequency transactions, we secure yield margins that are returned directly to your digital wallets every 24 hours. Learn how you can maximize compounding with minimal risk today.",
            imageUrl = "https://picsum.photos/seed/invexx2/400/300",
            date = "April 20, 2025"
        ),
        BlogModel(
            id = 3,
            title = "Security is Our Top Priority",
            excerpt = "At Invexx, the security of your data and transactions is our top priority. We use advanced encryption and secure systems.",
            content = "At INVEXX, we prioritize safety above all else. Your financial data is protected by industry-standard 256-bit SSL encryption and our secure backend ledger network. Additionally, our SharedPreferences local storage keeps your credentials locked safely on-device. With features like immediate withdrawal auditing, automated security checkups, and 100% verified KYC guidelines, we make sure that your funds are accessible only to you, at all times.",
            imageUrl = "https://picsum.photos/seed/invexx3/400/300",
            date = "April 15, 2025"
        ),
        BlogModel(
            id = 4,
            title = "Invite Friends & Earn More",
            excerpt = "Share your referral link, invite your friends and earn exciting commission on their investments.",
            content = "Unlock exponential income with the INVEXX multi-tier affiliate program. By sharing your custom referral link, you can invite friends and earn immediate rewards. From Level 1 commissions starting at 50% down to team volume incentives, we reward team leaders with rank advancements up to VIP10. Empower your network to invest, and grow a sustainable, recurring team salary directly from team volume.",
            imageUrl = "https://picsum.photos/seed/invexx4/400/300",
            date = "April 10, 2025"
        ),
        BlogModel(
            id = 5,
            title = "New Investment Plans Coming Soon",
            excerpt = "We are working on new and better investment plans to help you earn more and grow faster.",
            content = "We are constantly evaluating and designing new fund portfolios to offer diverse risk-reward profiles. In the coming quarter, we are planning to introduce eco-friendly energy portfolios and micro-infrastructure funding. These additions will be hosted under our Welfare and Yearly sections to offer even higher annual yields and special VIP bonuses. Stay tuned to our blogs for early-bird reservation opportunities!",
            imageUrl = "https://picsum.photos/seed/invexx5/400/300",
            date = "April 05, 2025"
        )
    )

    override suspend fun login(request: LoginRequest): ApiResponse<UserModel> {
        delay(1000)
        if (request.phone.isBlank() || request.password.isBlank()) {
            return ApiResponse("error", "Phone and password are required", null)
        }
        prefs.token = "mock_auth_token_xyz_invexx"
        prefs.phone = request.phone
        prefs.name = "User_" + request.phone.takeLast(4)
        prefs.userId = 7
        
        val user = UserModel(
            id = 7,
            name = prefs.name,
            phone = prefs.phone,
            userId = prefs.userId,
            balance = prefs.balance,
            bonus = prefs.bonus,
            recharge = prefs.recharge,
            vipLevel = prefs.vipLevel,
            referralCode = "PHONE",
            isVerified = true
        )
        return ApiResponse("success", "Login successful", user)
    }

    override suspend fun register(request: RegisterRequest): ApiResponse<String> {
        delay(1000)
        if (request.phone.isBlank() || request.userId.isBlank() || request.password.isBlank()) {
            return ApiResponse("error", "All fields are required", null)
        }
        val genId = request.phone.hashCode().coerceAtLeast(100000)
        prefs.token = "mock_auth_token_xyz_invexx"
        prefs.phone = request.phone
        prefs.name = request.userId
        prefs.userId = genId
        prefs.balance = 0.0f
        prefs.bonus = 46.00f
        prefs.recharge = 0.0f
        prefs.vipLevel = 0
        prefs.referralCode = request.referralCode ?: "PHONE"
        prefs.accountPassword = request.password
        return ApiResponse("success", "Account created successfully!", "Registered")
    }

    override suspend fun getPlans(type: String): ApiResponse<List<PlanModel>> {
        delay(500)
        val filtered = if (type == "all" || type.isEmpty()) plans else plans.filter { it.type == type }
        return ApiResponse("success", "Plans loaded", filtered)
    }

    override suspend fun getTeamStats(): ApiResponse<TeamStatsModel> {
        delay(500)
        val stats = TeamStatsModel(
            teamSize = 1,
            teamRank = "VIP${prefs.vipLevel}",
            totalIncome = prefs.bonus + 2.00f, // Match UI mockup nicely
            registerTotal = 1,
            registerActive = 1,
            businessTotal = 0,
            businessActive = 0,
            registerCommission = 50.0f,
            registerIncome = 2.00f,
            businessCommission = 2.0f,
            businessIncome = 0.00f,
            referredUsers = listOf(
                ReferredUserModel(
                    phone = "9876543210",
                    name = "Mock Partner",
                    userId = "88772",
                    vipLevel = 2,
                    recharge = 500.0f,
                    balance = 120.0f,
                    isVerified = true
                )
            )
        )
        return ApiResponse("success", "Team stats loaded", stats)
    }

    override fun getTeamStatsFlow(): kotlinx.coroutines.flow.Flow<TeamStatsModel> {
        return kotlinx.coroutines.flow.flow {
            val stats = TeamStatsModel(
                teamSize = 1,
                teamRank = "VIP${prefs.vipLevel}",
                totalIncome = prefs.bonus + 2.00f,
                registerTotal = 1,
                registerActive = 1,
                businessTotal = 0,
                businessActive = 0,
                registerCommission = 50.0f,
                registerIncome = 2.00f,
                businessCommission = 2.0f,
                businessIncome = 0.00f,
                referredUsers = listOf(
                    ReferredUserModel(
                        phone = "9876543210",
                        name = "Mock Partner",
                        userId = "88772",
                        vipLevel = 2,
                        recharge = 500.0f,
                        balance = 120.0f,
                        isVerified = true
                    )
                )
            )
            emit(stats)
        }
    }

    override suspend fun getBlogs(): ApiResponse<List<BlogModel>> {
        delay(500)
        return ApiResponse("success", "Blogs loaded", blogs)
    }

    override fun getBlogsFlow(): kotlinx.coroutines.flow.Flow<List<BlogModel>> {
        return kotlinx.coroutines.flow.flow {
            emit(blogs)
        }
    }

    override suspend fun createDeposit(request: DepositRequest): ApiResponse<String> {
        delay(1000)
        val newRecharge = prefs.recharge + request.amount
        prefs.recharge = newRecharge
        prefs.balance += request.amount
        transactions.add(0, TransactionModel(
            id = transactions.size + 1,
            type = "deposit",
            amount = request.amount,
            status = "Success",
            date = "Just now",
            description = "Deposit successful via UPI"
        ))
        return ApiResponse("success", "Payment initiated", "https://api.invexx.web.app/pay/upi?amount=${request.amount}")
    }

    override suspend fun createPendingDeposit(amount: Float, orderNo: String): ApiResponse<String> {
        delay(1000)
        transactions.add(0, TransactionModel(
            id = transactions.size + 1,
            type = "deposit",
            amount = amount,
            status = "Pending",
            date = "Just now",
            description = "Deposit request",
            orderNo = orderNo
        ))
        return ApiResponse("success", "Pending deposit created", null)
    }

    override suspend fun processPaymentCallback(orderNo: String, status: String, amount: Float): ApiResponse<String> {
        delay(1000)
        val tx = transactions.find { it.orderNo == orderNo }
        if (tx != null) {
            if (status.equals("success", ignoreCase = true)) {
                val updatedTx = tx.copy(status = "Success")
                transactions.remove(tx)
                transactions.add(0, updatedTx)
                prefs.balance += amount
                prefs.recharge += amount
                return ApiResponse("success", "Processed successfully", null)
            } else {
                val updatedTx = tx.copy(status = "Failed")
                transactions.remove(tx)
                transactions.add(0, updatedTx)
                return ApiResponse("success", "Processed as failed", null)
            }
        }
        return ApiResponse("error", "Transaction not found", null)
    }

    override suspend fun createWithdrawal(request: WithdrawRequest): ApiResponse<String> {
        delay(1200)
        if (request.amount > prefs.balance) {
            return ApiResponse("error", "Insufficient balance", null)
        }
        prefs.balance -= request.amount
        transactions.add(0, TransactionModel(
            id = transactions.size + 1,
            type = "withdrawal",
            amount = request.amount,
            status = "Pending",
            date = "Just now",
            description = "Withdraw to bank: ${request.bankName} (${request.accountNumber})"
        ))
        return ApiResponse("success", "Withdrawal request submitted successfully", "Pending")
    }

    override suspend fun getOrders(): ApiResponse<List<OrderModel>> {
        delay(500)
        return ApiResponse("success", "Orders loaded", orders)
    }

    override fun getOrdersFlow(): kotlinx.coroutines.flow.Flow<List<OrderModel>> {
        return kotlinx.coroutines.flow.flow {
            emit(orders)
        }
    }

    override suspend fun getTransactions(): ApiResponse<List<TransactionModel>> {
        delay(500)
        return ApiResponse("success", "Transactions loaded", transactions)
    }

    override fun getTransactionsFlow(): kotlinx.coroutines.flow.Flow<List<TransactionModel>> {
        return kotlinx.coroutines.flow.flow {
            emit(transactions)
        }
    }

    override suspend fun redeemGiftCode(request: RedeemRequest): ApiResponse<String> {
        delay(1000)
        if (request.code.uppercase() == "INVEXX100") {
            prefs.bonus += 100f
            transactions.add(0, TransactionModel(
                id = transactions.size + 1,
                type = "bonus",
                amount = 100.00f,
                status = "Success",
                date = "Just now",
                description = "Gift code INVEXX100 redeemed"
            ))
            return ApiResponse("success", "Code redeemed successfully! ₹100.00 added to bonus.", "Success")
        }
        return ApiResponse("error", "Invalid or expired gift code", null)
    }

    override suspend fun getVipBenefits(): ApiResponse<List<VipBenefitModel>> {
        delay(300)
        return ApiResponse("success", "Success", emptyList())
    }

    override suspend fun getTasks(): ApiResponse<List<TaskModel>> {
        delay(300)
        return ApiResponse("success", "Tasks loaded", tasks)
    }

    override suspend fun claimTask(id: Int): ApiResponse<String> {
        delay(800)
        val taskIndex = tasks.indexOfFirst { it.id == id }
        if (taskIndex != -1) {
            val task = tasks[taskIndex]
            if (task.isClaimable && !task.isClaimed) {
                tasks[taskIndex] = task.copy(isClaimed = true, isClaimable = false)
                prefs.bonus += task.rewardAmount
                transactions.add(0, TransactionModel(
                    id = transactions.size + 1,
                    type = "bonus",
                    amount = task.rewardAmount,
                    status = "Success",
                    date = "Just now",
                    description = "Claimed task reward: ${task.title}"
                ))
                return ApiResponse("success", "Claimed ₹${task.rewardAmount} successfully!", "Success")
            }
        }
        return ApiResponse("error", "Task is not claimable or already claimed", null)
    }

    override suspend fun checkIn(): ApiResponse<String> {
        delay(500)
        val taskIndex = tasks.indexOfFirst { it.id == 1 }
        if (taskIndex != -1) {
            val task = tasks[taskIndex]
            if (!task.isClaimed && !task.isClaimable) {
                tasks[taskIndex] = task.copy(progress = 1.0f, isClaimable = true)
                return ApiResponse("success", "Checked in successfully! Now claim your reward.", "Success")
            }
        }
        return ApiResponse("error", "Already checked in or claimed today", null)
    }

    override suspend fun getNotifications(): ApiResponse<List<NotificationModel>> {
        delay(300)
        return ApiResponse("success", "Notifications loaded", notifications)
    }

    override suspend fun markNotificationsAsRead(): ApiResponse<String> {
        delay(300)
        for (i in notifications.indices) {
            notifications[i] = notifications[i].copy(isRead = true)
        }
        return ApiResponse("success", "All notifications marked as read", "Success")
    }

    override suspend fun buyPlan(plan: PlanModel, amount: Float): ApiResponse<String> {
        delay(1000)
        if (prefs.balance < amount) {
            return ApiResponse("error", "Insufficient balance", null)
        }
        prefs.balance -= amount
        
        orders.add(0, OrderModel(
            id = orders.size + 101,
            planName = plan.name,
            investAmount = plan.price,
            dailyEarnings = plan.dailyEarnings,
            totalReturn = plan.totalRevenue,
            startDate = "Just now",
            endDate = "In ${plan.revenueDays} days",
            status = "Active"
        ))
        
        transactions.add(0, TransactionModel(
            id = transactions.size + 1,
            type = "investment",
            amount = amount,
            status = "Success",
            date = "Just now",
            description = "Invested in plan ${plan.name}"
        ))
        
        return ApiResponse("success", "Successfully invested in plan ${plan.name}!", "Success")
    }

    override suspend fun updateProfileName(name: String): ApiResponse<String> {
        delay(300)
        prefs.name = name
        return ApiResponse("success", "Profile name updated to $name locally", "Success")
    }

    override suspend fun saveBankDetailsInDb(holder: String, bank: String, account: String, ifsc: String): ApiResponse<String> {
        delay(500)
        prefs.bankHolderName = holder
        prefs.bankName = bank
        prefs.bankAccountNumber = account
        prefs.bankIfscCode = ifsc
        prefs.isBankDetailsSaved = true
        return ApiResponse("success", "Bank details successfully saved to Mock Database", "Success")
    }
    
    override suspend fun checkUserExists(phone: String): ApiResponse<Boolean> {
        delay(300)
        return ApiResponse("success", "User check", phone == prefs.phone)
    }

    override suspend fun resetPassword(phone: String, newPass: String): ApiResponse<String> {
        delay(300)
        if (phone == prefs.phone) {
            prefs.accountPassword = newPass
            return ApiResponse("success", "Password reset successfully", "Success")
        }
        return ApiResponse("error", "User not found", null)
    }

    override suspend fun adminGetAllUsers(): ApiResponse<List<Map<String, Any>>> {
        delay(300)
        val mockUser = mapOf(
            "name" to prefs.name,
            "phone" to (prefs.phone ?: "1234567890"),
            "balance" to prefs.balance,
            "bonus" to prefs.bonus,
            "recharge" to prefs.recharge,
            "vipLevel" to prefs.vipLevel,
            "transactions" to listOf(
                mapOf("id" to 1, "type" to "deposit", "amount" to 500f, "status" to "Success", "date" to "Just now", "description" to "Mock Deposit"),
                mapOf("id" to 2, "type" to "withdrawal", "amount" to 200f, "status" to "Pending", "date" to "Just now", "description" to "Mock Withdrawal")
            )
        )
        return ApiResponse("success", "Loaded users", listOf(mockUser))
    }

    override suspend fun adminUpdateUser(phone: String, updates: Map<String, Any>): ApiResponse<String> {
        delay(300)
        return ApiResponse("success", "User updated", "Success")
    }

    override suspend fun adminAddPlan(plan: PlanModel): ApiResponse<String> {
        delay(300)
        plans.add(plan)
        return ApiResponse("success", "Plan added", "Success")
    }

    override suspend fun adminUpdatePlan(plan: PlanModel): ApiResponse<String> {
        delay(300)
        val index = plans.indexOfFirst { it.id == plan.id }
        if (index != -1) {
            plans[index] = plan
        }
        return ApiResponse("success", "Plan updated", "Success")
    }

    override suspend fun adminDeletePlan(id: Int): ApiResponse<String> {
        delay(300)
        plans.removeAll { it.id == id }
        return ApiResponse("success", "Plan deleted", "Success")
    }

    override suspend fun adminAddBlog(blog: BlogModel): ApiResponse<String> {
        delay(300)
        return ApiResponse("success", "Blog added", "Success")
    }

    override suspend fun adminUpdateBlog(blog: BlogModel): ApiResponse<String> {
        delay(300)
        return ApiResponse("success", "Blog updated", "Success")
    }

    override suspend fun adminDeleteBlog(id: Int): ApiResponse<String> {
        delay(300)
        return ApiResponse("success", "Blog deleted", "Success")
    }

    override suspend fun adminAddTask(task: TaskModel): ApiResponse<String> {
        delay(300)
        return ApiResponse("success", "Task added", "Success")
    }

    override suspend fun adminUpdateTask(task: TaskModel): ApiResponse<String> {
        delay(300)
        return ApiResponse("success", "Task updated", "Success")
    }

    override suspend fun adminDeleteTask(id: Int): ApiResponse<String> {
        delay(300)
        return ApiResponse("success", "Task deleted", "Success")
    }

    override suspend fun adminProcessTransaction(userPhone: String, txId: Int, newStatus: String): ApiResponse<String> {
        delay(300)
        return ApiResponse("success", "Transaction processed", "Success")
    }

    override suspend fun adminTriggerMidnightReturns(): ApiResponse<String> {
        delay(300)
        return ApiResponse("success", "Midnight returns triggered successfully", "Success")
    }

    override suspend fun getSystemSettings(): ApiResponse<Map<String, Any>> {
        delay(300)
        return ApiResponse("success", "Settings loaded", mapOf(
            "minVersion" to "1.0.0", 
            "latestVersion" to "1.0.0", 
            "updateUrl" to "https://invexx.app",
            "telegram_url" to "https://t.me/invexx_official",
            "whatsapp_url" to "https://wa.me/919999999999",
            "support_email" to "support@invexx-wealth.com"
        ))
    }

    override suspend fun updateSystemSettings(settings: Map<String, Any>): ApiResponse<String> {
        delay(300)
        return ApiResponse("success", "Settings updated", "Success")
    }

    override fun getUserProfileFlow(): kotlinx.coroutines.flow.Flow<Map<String, Any>> {
        return kotlinx.coroutines.flow.flow {
            val map = mapOf<String, Any>(
                "balance" to prefs.balance,
                "bonus" to prefs.bonus,
                "recharge" to prefs.recharge,
                "vipLevel" to prefs.vipLevel,
                "name" to prefs.name,
                "bankHolderName" to prefs.bankHolderName,
                "bankName" to prefs.bankName,
                "bankAccountNumber" to prefs.bankAccountNumber,
                "bankIfscCode" to prefs.bankIfscCode,
                "isBankDetailsSaved" to prefs.isBankDetailsSaved,
                "referralCode" to prefs.referralCode
            )
            emit(map)
        }
    }
}

/**
 * Singleton dependency manager (ServiceLocator pattern).
 * Keeps dependencies isolated, lightweight, and compile-time safe,
 * perfectly supporting incremental hot-reloads and lightning-fast compilation times.
 */
object ServiceLocator {
    private var apiServiceInstance: ApiService? = null
    private var prefsInstance: PreferenceManager? = null

    fun getPrefs(context: Context): PreferenceManager {
        if (prefsInstance == null) {
            prefsInstance = PreferenceManager(context.applicationContext)
        }
        return prefsInstance!!
    }

    fun getApiService(context: Context): ApiService {
        if (apiServiceInstance == null) {
            // Use live Firebase-connected API service by default
            apiServiceInstance = FirebaseApiService(context.applicationContext)
        }
        return apiServiceInstance!!
    }

    // Provision of real Retrofit client if needed
    fun getRealRetrofitApiService(context: Context, tokenProvider: () -> String?): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            tokenProvider()?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
