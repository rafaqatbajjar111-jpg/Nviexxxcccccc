package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserModel(
    val id: Int,
    val name: String,
    val phone: String,
    val userId: Int,
    val balance: Float,
    val bonus: Float,
    val recharge: Float,
    val vipLevel: Int,
    val referralCode: String,
    val isVerified: Boolean
)

@JsonClass(generateAdapter = true)
data class PlanModel(
    val id: Int,
    val name: String,
    val price: Float,
    val revenueDays: Int,
    val dailyEarnings: Float,
    val totalRevenue: Float,
    val type: String, // "fixed_fund", "welfare_fund", "yearly_fund"
    val iconUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ReferredUserModel(
    val phone: String,
    val name: String,
    val userId: String,
    val vipLevel: Int,
    val recharge: Float,
    val balance: Float,
    val isVerified: Boolean
)

@JsonClass(generateAdapter = true)
data class TeamStatsModel(
    val teamSize: Int,
    val teamRank: String, // e.g. "VIP0"
    val totalIncome: Float,
    val registerTotal: Int,
    val registerActive: Int,
    val businessTotal: Int,
    val businessActive: Int,
    val registerCommission: Float,
    val registerIncome: Float,
    val businessCommission: Float,
    val businessIncome: Float,
    val referredUsers: List<ReferredUserModel>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class BlogModel(
    val id: Int,
    val title: String,
    val excerpt: String,
    val content: String,
    val imageUrl: String,
    val date: String
)

@JsonClass(generateAdapter = true)
data class TransactionModel(
    val id: Int,
    val type: String, // "deposit", "withdrawal", "commission", "bonus"
    val amount: Float,
    val status: String, // "Success", "Pending", "Failed"
    val date: String,
    val description: String,
    val orderNo: String? = null
)

@JsonClass(generateAdapter = true)
data class OrderModel(
    val id: Int,
    val planName: String,
    val investAmount: Float,
    val dailyEarnings: Float,
    val totalReturn: Float,
    val startDate: String,
    val endDate: String,
    val status: String // "Active", "Completed", "Expired"
)

@JsonClass(generateAdapter = true)
data class NotificationModel(
    val id: Int,
    val title: String,
    val message: String,
    val date: String,
    val isRead: Boolean
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val status: String, // "success" or "error"
    val message: String,
    val data: T?
)

// Helper DTOs for Request Payloads
data class LoginRequest(val phone: String, val password: String)
data class RegisterRequest(val phone: String, val userId: String, val password: String, val referralCode: String? = null)
data class DepositRequest(val amount: Float)
data class WithdrawRequest(val name: String, val bankName: String, val accountNumber: String, val ifscCode: String, val amount: Float)
data class RedeemRequest(val code: String)

@JsonClass(generateAdapter = true)
data class TaskModel(
    val id: Int,
    val title: String,
    val subtitle: String,
    val progress: Float, // 0f to 1f
    val isClaimed: Boolean,
    val isClaimable: Boolean,
    val rewardAmount: Float
)

@JsonClass(generateAdapter = true)
data class VipBenefitModel(
    val level: String, // "VIP0", "VIP1"
    val requirement: String,
    val benefitDesc: String
)
