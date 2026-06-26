package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.utils.tr
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

/**
 * SHARED TOP APP BAR
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvexxTopAppBar(
    navController: NavController,
    notificationCount: Int = 2,
    showLogo: Boolean = true
) {
    TopAppBar(
        title = {
            if (showLogo) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = DarkCharcoal, fontWeight = FontWeight.ExtraBold)) {
                                append("INVE")
                            }
                            withStyle(style = SpanStyle(color = PrimaryGold, fontWeight = FontWeight.ExtraBold)) {
                                append("XX")
                            }
                        },
                        style = Typography.titleLarge.copy(fontSize = 24.sp, letterSpacing = 1.sp)
                    )
                }
            }
        },
        navigationIcon = {},
        actions = {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .clickable { navController.navigate("notifications") }
                    .drawBehind { drawCircle(color = ShadowColor, radius = size.width / 2f + 3f) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = DarkCharcoal,
                    modifier = Modifier.size(20.dp)
                )
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .clip(CircleShape)
                            .background(PrimaryGold)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftBackground)
    )
}

/**
 * HOME SCREEN
 * Features user gold gradient card, balances, copyable referral link, Quick Action row, Tab plan selections,
 * plan list with a purchase modal bottom sheet, and 3-stat overview.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val balance by viewModel.walletBalance.collectAsState()
    val earningBalance by viewModel.walletEarning.collectAsState()
    val bonus by viewModel.walletBonus.collectAsState()
    val recharge by viewModel.walletRecharge.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val plansState by viewModel.plansState.collectAsState()
    val buyState by viewModel.buyState.collectAsState()
    val phone by viewModel.userPhone.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val referralCode by viewModel.referralCode.collectAsState()
    val teamStats by viewModel.teamStats.collectAsState()

    var showBuyDialog by remember { mutableStateOf<PlanModel?>(null) }
    var buyAmountInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.syncWallet()
        viewModel.loadPlans(selectedTab)
        viewModel.loadTeamStats()
    }

    LaunchedEffect(buyState) {
        if (buyState is UiState.Success) {
            android.widget.Toast.makeText(context, (buyState as UiState.Success<String>).data, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetBuyState()
        } else if (buyState is UiState.Error) {
            android.widget.Toast.makeText(context, (buyState as UiState.Error).message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetBuyState()
        }
    }

    Scaffold(
        topBar = { InvexxTopAppBar(navController = navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Gold Gradient Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFF7C2), Color(0xFFFFFDF5))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar
                            AsyncImage(
                                model = "https://robohash.org/$userId?set=set4",
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(PureWhite),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.app_icon)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            // User Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = phone,
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GoldenBadge)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${tr("user_id")}: $userId",
                                        style = Typography.labelSmall.copy(
                                            color = DarkCharcoal,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = LightGrayBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Four Wallets display row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("₹${"%.2f".format(balance)}", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Deposit", style = Typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("₹${"%.2f".format(earningBalance)}", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Earnings", style = Typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("₹${"%.2f".format(bonus)}", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Bonus", style = Typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("₹${"%.2f".format(recharge)}", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Recharge", style = Typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Quick Actions Circle card row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val actions = listOf(
                        Triple(tr("deposit"), Icons.Default.Add, "deposit"),
                        Triple(tr("withdraw"), Icons.Default.Lock, "withdraw"),
                        Triple(tr("share"), Icons.Default.Share, "share"),
                        Triple(tr("online"), Icons.Default.Home, "online")
                    )
                    actions.forEach { (label, icon, route) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (route == "deposit" || route == "withdraw") {
                                        navController.navigate(route)
                                    } else if (route == "online") {
                                        navController.navigate("support")
                                    } else if (route == "share") {
                                        // Trigger clipboard copy
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Invexx Ref", referralCode)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Referral link copied successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                        viewModel.syncWallet()
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(PureWhite)
                                    .drawBehind { drawCircle(color = ShadowColor, radius = size.width / 2f + 2f) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium, color = MediumGray)
                            )
                        }
                    }
                }
            }

            // Referral Link Section
            item {
                InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                    Text(
                        text = tr("referral_desc"),
                        style = Typography.bodyMedium.copy(color = MediumGray)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftBackground)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = referralCode,
                                style = Typography.labelSmall.copy(fontSize = 14.sp, color = DarkCharcoal, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Invexx Ref", referralCode)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Referral link copied successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                viewModel.syncWallet()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(tr("copy"), style = Typography.labelLarge.copy(color = DarkCharcoal))
                        }
                    }
                }
            }

            // Stats row (Team Size, Team Rank, Total Income)
            item {
                InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatCard(icon = Icons.Default.Share, label = tr("team_size"), value = teamStats?.teamSize?.toString() ?: "0", subtitle = tr("active_team"))
                        Box(modifier = Modifier.width(1.dp).height(50.dp).background(LightGrayBorder))
                        StatCard(icon = Icons.Default.Star, label = tr("team_rank"), value = teamStats?.teamRank ?: "VIP0")
                        Box(modifier = Modifier.width(1.dp).height(50.dp).background(LightGrayBorder))
                        StatCard(icon = Icons.Default.Lock, label = tr("total_income"), value = "₹${"%.2f".format(teamStats?.totalIncome ?: 0.0f)}", subtitle = tr("total_earning"))
                    }
                }
            }

            // Fund type tab row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "fixed_fund",
                        "welfare_fund",
                        "yearly_fund"
                    )
                    tabs.forEach { type ->
                        val active = selectedTab == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (active) PrimaryGold else Color.Transparent)
                                .border(
                                    width = if (active) 0.dp else 1.dp,
                                    color = if (active) Color.Transparent else LightGrayBorder,
                                    shape = RoundedCornerShape(50.dp)
                                )
                                .clickable { viewModel.setTab(type) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr(type),
                                style = Typography.labelLarge.copy(
                                    color = if (active) DarkCharcoal else MediumGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Investment List items
            when (plansState) {
                is UiState.Loading -> {
                    item {
                        CircularProgressIndicator(
                            color = PrimaryGold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(
                            text = (plansState as UiState.Error).message,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val list = (plansState as UiState.Success<List<PlanModel>>).data
                    items(list) { plan ->
                        InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pot/Jar icon in rounded square
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PrimaryGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Plan",
                                        tint = PureWhite,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Star",
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = plan.name,
                                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Price", style = Typography.labelSmall)
                                    Text(
                                        "₹${"%.2f".format(plan.price)}",
                                        style = Typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                }

                                InvexxButton(
                                    text = "Buy Now",
                                    onClick = {
                                        viewModel.buyPlan(plan, plan.price)
                                    },
                                    modifier = Modifier.width(110.dp).height(44.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = LightGrayBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            // 3-column stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Revenue", style = Typography.labelSmall)
                                    Text("${plan.revenueDays} Days", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Daily Earnings", style = Typography.labelSmall)
                                    Text("₹${"%.2f".format(plan.dailyEarnings)}", style = Typography.labelLarge.copy(color = PrimaryGold, fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Total Revenue", style = Typography.labelSmall)
                                    Text("₹${"%.2f".format(plan.totalRevenue)}", style = Typography.labelLarge.copy(color = PrimaryGold, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

}

/**
 * TEAM SCREEN
 * Team registrations, payouts, progressions, business volume details, and ranking banner.
 */
@Composable
fun TeamScreen(
    navController: NavController,
    viewModel: TeamViewModel,
    modifier: Modifier = Modifier
) {
    val teamState by viewModel.teamState.collectAsState()
    val phone by viewModel.userPhone.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val referralCode by viewModel.referralCode.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.syncUser()
        viewModel.loadTeamStats()
    }

    val stats = when (val state = teamState) {
        is UiState.Success -> state.data
        else -> null
    }

    Scaffold(
        topBar = { InvexxTopAppBar(navController = navController) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Side-by-Side Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left stat card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PureWhite)
                            .border(1.dp, PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Referral", tint = PrimaryGold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tr("total_registered"), style = Typography.labelSmall)
                            Text("${stats?.registerTotal ?: 0}", style = Typography.titleLarge.copy(color = PrimaryGold, fontWeight = FontWeight.ExtraBold))
                        }
                    }
                    // Right stat card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PureWhite)
                            .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Earning", tint = PrimaryGold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tr("total_earnings"), style = Typography.labelSmall)
                            Text("₹${String.format("%.2f", stats?.totalIncome ?: 0f)}", style = Typography.titleLarge.copy(color = PrimaryGold, fontWeight = FontWeight.ExtraBold))
                        }
                    }
                }
            }

            // User Info header context
            item {
                InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InvexxLogo(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(phone, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("${tr("user_id")}: $userId", style = Typography.labelSmall.copy(color = PrimaryGold, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Referral card
            item {
                InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                    Text(
                        text = tr("referral_desc"),
                        style = Typography.bodyMedium.copy(color = MediumGray)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftBackground)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = referralCode,
                                style = Typography.labelSmall.copy(fontSize = 14.sp, color = DarkCharcoal, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Invexx Ref", referralCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(tr("copy"), style = Typography.labelLarge.copy(color = DarkCharcoal))
                        }
                    }
                }
            }

            // Register/Active and Business Volume Side-by-Side card metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Register/Active
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PureWhite)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Shield bar", tint = PrimaryGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("register_active"), style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${stats?.registerActive ?: 0} / ${stats?.registerTotal ?: 0}", style = Typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                            Text(tr("total_active"), style = Typography.labelSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            // Progress bar
                            val regProgress = if ((stats?.registerTotal ?: 0) > 0) (stats?.registerActive ?: 0).toFloat() / (stats?.registerTotal ?: 0) else 0f
                            GoldenProgressBar(progress = regProgress)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tr("commission"), style = Typography.labelSmall)
                            Text("${stats?.registerCommission ?: 50.0f}%", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            Text(tr("income"), style = Typography.labelSmall)
                            Text("₹${String.format("%.2f", stats?.registerIncome ?: 0f)}", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            InvexxButton(
                                text = tr("details"),
                                onClick = {
                                    Toast.makeText(context, "Check registered partner status below", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            )
                        }
                    }

                    // Card 2: Business Volume
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PureWhite)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = "Shield bar", tint = PrimaryGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("business_volume"), style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("₹${stats?.businessTotal ?: 0}", style = Typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                            Text(tr("total_active"), style = Typography.labelSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            // Progress bar
                            val bizProgress = if ((stats?.registerTotal ?: 0) > 0) (stats?.businessActive ?: 0).toFloat() / (stats?.registerTotal ?: 0) else 0f
                            GoldenProgressBar(progress = bizProgress)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tr("commission"), style = Typography.labelSmall)
                            Text("${stats?.businessCommission ?: 2.0f}%", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            Text(tr("income"), style = Typography.labelSmall)
                            Text("₹${String.format("%.2f", stats?.businessIncome ?: 0f)}", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            InvexxButton(
                                text = tr("details"),
                                onClick = {
                                    Toast.makeText(context, "Business volume represents total recharges of referred partners", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            )
                        }
                    }
                }
            }

            // Upgrade benefits banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFFDF0))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Upgrade", tint = PrimaryGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(tr("upgrade_rank_1"), style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                Text(tr("upgrade_rank_2"), style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        InvexxButton(
                            text = tr("view_rank_benefits"),
                            onClick = { navController.navigate("rankBenefits") },
                            modifier = Modifier.height(38.dp)
                        )
                    }
                }
            }

            // Referred Partners Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Referred Partners (Team)",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryGold.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${stats?.referredUsers?.size ?: 0} partners",
                            style = Typography.labelSmall.copy(color = PrimaryGold, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Referred Partners List
            val referredUsers = stats?.referredUsers ?: emptyList()
            if (referredUsers.isEmpty()) {
                item {
                    InvexxCard(borderRadius = 16.dp, padding = 20.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MediumGray.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No team partners referred yet",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MediumGray)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Share your referral link above to invite friends and earn ₹10.00 registration bonus + commissions!",
                                style = Typography.labelSmall.copy(color = MediumGray.copy(alpha = 0.7f)),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(referredUsers) { partner ->
                    InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Avatar placeholder with VIP badge
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGold.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = PrimaryGold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = partner.name,
                                            style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal)
                                        )
                                        if (partner.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Verified Partner",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // VIP Badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PrimaryGold)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "VIP${partner.vipLevel}",
                                                style = Typography.labelSmall.copy(
                                                    color = DarkCharcoal,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Phone contact & User ID
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Phone",
                                            tint = MediumGray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = partner.phone,
                                            style = Typography.labelSmall.copy(color = DarkCharcoal.copy(alpha = 0.8f))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "ID: ${partner.userId}",
                                            style = Typography.labelSmall.copy(color = MediumGray)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Investment / Balance stats
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recharge: ₹${String.format("%.1f", partner.recharge)}",
                                            style = Typography.labelSmall.copy(
                                                color = if (partner.recharge > 0f) Color(0xFF4CAF50) else MediumGray,
                                                fontWeight = if (partner.recharge > 0f) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                        Text(
                                            text = "Balance: ₹${String.format("%.1f", partner.balance)}",
                                            style = Typography.labelSmall.copy(color = MediumGray)
                                        )
                                    }
                                }
                            }

                            // Contact Action
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Contact Phone", partner.phone)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Phone copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SoftBackground)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Contact",
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * BLOG SCREEN
 * Shows general articles, announcements, news, and details transition on item click.
 */
@Composable
fun BlogScreen(
    navController: NavController,
    viewModel: BlogViewModel,
    modifier: Modifier = Modifier
) {
    val blogsState by viewModel.blogsState.collectAsState()

    Scaffold(
        topBar = { InvexxTopAppBar(navController = navController, showLogo = false) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFF4B2), Color(0xFFFFFDF5))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PureWhite),
                                    contentAlignment = Alignment.Center
                                ) {
                                    InvexxLogo(modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Blog & News", style = Typography.titleLarge.copy(fontSize = 22.sp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Stay updated with the latest news and important updates.",
                                style = Typography.bodyMedium.copy(fontSize = 13.sp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.news_illustration),
                            contentDescription = "Newspaper",
                            modifier = Modifier.size(75.dp)
                        )
                    }
                }
            }

            // Scrollable Blog Cards list
            when (blogsState) {
                is UiState.Loading -> {
                    item {
                        CircularProgressIndicator(
                            color = PrimaryGold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(text = "Failed to load blogs")
                    }
                }
                is UiState.Success -> {
                    val blogs = (blogsState as UiState.Success<List<BlogModel>>).data
                    items(blogs) { blog ->
                        InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("blogDetail/${blog.id}") },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Square image via Coil
                                AsyncImage(
                                    model = blog.imageUrl,
                                    contentDescription = blog.title,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LightGrayBorder),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = R.drawable.app_icon) // local gold placeholder fallback
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Home, // Calendar icon fallback
                                            contentDescription = "Date",
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(blog.date, style = Typography.labelSmall)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = blog.title,
                                        style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = blog.excerpt,
                                        style = Typography.bodyMedium.copy(fontSize = 12.sp, color = MediumGray),
                                        maxLines = 2
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Small gold circle with white right arrow
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock, // right arrow fallback
                                        contentDescription = "Detail",
                                        tint = PureWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/**
 * MINE SCREEN
 * Holds personal profiles, balance details, quick action options, and support navigations.
 */
@Composable
fun MineScreen(
    navController: NavController,
    viewModel: MineViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val balance by viewModel.balance.collectAsState()
    val earningBalance by viewModel.earningBalance.collectAsState()
    val bonus by viewModel.bonus.collectAsState()
    val recharge by viewModel.recharge.collectAsState()
    val name by viewModel.userName.collectAsState()
    val phone by viewModel.userPhone.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val vipLevel by viewModel.vipLevel.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }

    Scaffold(
        topBar = { InvexxTopAppBar(navController = navController, showLogo = false) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile top card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFF4B2), Color(0xFFFFFDF5))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar
                            AsyncImage(
                                model = "https://robohash.org/$userId?set=set4",
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(PureWhite),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.app_icon)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            // Info
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        name,
                                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit, // Edit pencil icon
                                        contentDescription = "Edit name",
                                        tint = PrimaryGold,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { 
                                                newNameInput = name
                                                showEditNameDialog = true
                                            }
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(phone, style = Typography.bodyMedium.copy(fontSize = 13.sp, color = MediumGray))
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GoldenBadge)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "${tr("user_id")}: $userId",
                                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal)
                                    )
                                }
                            }

                            // Crown and VIP badge below avatar
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryGold)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "VIP$vipLevel",
                                    color = DarkCharcoal,
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Rounded white pill wallet balance
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50.dp))
                                .background(PureWhite)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("₹${"%.2f".format(balance)}", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Deposit", style = Typography.labelSmall)
                                }
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(LightGrayBorder))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("₹${"%.2f".format(earningBalance)}", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Earnings", style = Typography.labelSmall)
                                }
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(LightGrayBorder))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("₹${"%.2f".format(bonus)}", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Bonus", style = Typography.labelSmall)
                                }
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(LightGrayBorder))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("₹${"%.2f".format(recharge)}", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Recharge", style = Typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Menu Options in List Form
            val menus = listOf(
                Pair("bank_card", "withdraw"),
                Pair("order_history", "orderHistory"),
                Pair("fund_history", "fundHistory"),
                Pair("gift_code", "giftCode"),
                Pair("task", "task"),
                Pair("payment_callbacks", "paymentCallbacks"),
                Pair("support", "support"),
                Pair("settings", "settings"),
                Pair("about_invexx", "about")
            )

            items(menus) { (key, route) ->
                val icon = when (key) {
                    "bank_card" -> Icons.Default.CreditCard
                    "order_history" -> Icons.Default.History
                    "fund_history" -> Icons.Default.ReceiptLong
                    "gift_code" -> Icons.Default.CardGiftcard
                    "task" -> Icons.Default.Assignment
                    "payment_callbacks" -> Icons.Default.ReceiptLong
                    "support" -> Icons.Default.Headset
                    "settings" -> Icons.Default.Settings
                    else -> Icons.Default.Info
                }

                MenuItemRow(
                    icon = icon,
                    title = tr(key),
                    onClick = {
                        navController.navigate(route)
                    }
                )
            }

            // Logout Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                InvexxButton(
                    text = tr("logout"),
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = DarkCharcoal)
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Profile Name", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                InvexxTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    hintText = "Enter your name",
                    leadingIcon = Icons.Default.Person
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newNameInput.isNotBlank()) {
                        viewModel.updateName(newNameInput)
                    }
                    showEditNameDialog = false
                }) {
                    Text("Save", color = PrimaryGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = MediumGray)
                }
            }
        )
    }

    // Logout confirm Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = tr("logout"),
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = { Text(tr("logout_confirm")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        showLogoutDialog = false
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                ) {
                    Text(tr("logout"), color = DarkCharcoal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(tr("cancel"), color = MediumGray)
                }
            }
        )
    }
}
