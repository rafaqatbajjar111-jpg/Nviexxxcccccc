package com.example.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

/**
 * SHARED FEATURE APP BAR
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureTopAppBar(
    title: String,
    navController: NavController
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = Typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .clickable { navController.popBackStack() }
                    .drawBehind { drawCircle(color = ShadowColor, radius = size.width / 2f + 3f) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DarkCharcoal,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftBackground)
    )
}

/**
 * DEPOSIT SCREEN
 */
@Composable
fun DepositScreen(
    navController: NavController,
    viewModel: DepositViewModel,
    modifier: Modifier = Modifier
) {
    val balance by viewModel.balance.collectAsState()
    val bonus by viewModel.bonus.collectAsState()
    val recharge by viewModel.recharge.collectAsState()
    
    val selectedAmount by viewModel.selectedAmount.collectAsState()
    val customAmount by viewModel.customAmount.collectAsState()
    val depositState by viewModel.depositState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(depositState) {
        if (depositState is UiState.Success) {
            val paymentUrl = (depositState as UiState.Success<String>).data
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(paymentUrl))
                context.startActivity(intent)
                android.widget.Toast.makeText(context, "Opening secure payment gateway...", android.widget.Toast.LENGTH_SHORT).show()
                navController.popBackStack() // Go back as payment will continue in browser
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Could not open browser", android.widget.Toast.LENGTH_SHORT).show()
            }
            viewModel.resetDepositState()
        } else if (depositState is UiState.Error) {
            android.widget.Toast.makeText(context, (depositState as UiState.Error).message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetDepositState()
        }
    }

    Scaffold(
        topBar = { FeatureTopAppBar("Deposit / Recharge", navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Wallets display row
            item {
                InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("₹${"%.2f".format(balance)}", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            Text("Balance", style = Typography.labelSmall)
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

            // Quick Chips
            item {
                Text("Select Recharge Amount", style = Typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.presetAmounts.take(3).forEach { amt ->
                        val active = selectedAmount == amt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) PrimaryGold else PureWhite)
                                .border(1.dp, if (active) Color.Transparent else LightGrayBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectPreset(amt) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "₹${amt.toInt()}",
                                style = Typography.labelLarge.copy(
                                    color = if (active) DarkCharcoal else MediumGray,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.presetAmounts.drop(3).forEach { amt ->
                        val active = selectedAmount == amt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) PrimaryGold else PureWhite)
                                .border(1.dp, if (active) Color.Transparent else LightGrayBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectPreset(amt) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "₹${amt.toInt()}",
                                style = Typography.labelLarge.copy(
                                    color = if (active) DarkCharcoal else MediumGray,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Custom Amount Input
            item {
                InvexxTextField(
                    value = customAmount,
                    onValueChange = { viewModel.setCustomAmount(it) },
                    hintText = "Enter custom amount (₹)",
                    leadingIcon = Icons.Default.Lock, // Rupee indicator fallback
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    testTag = "deposit_amount_input"
                )
            }

            // UPI Payment option card
            item {
                Text("Payment Method", style = Typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GoldenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "UPI", tint = PrimaryGold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("UPI Gateway", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                Text("GPay, PhonePe, Paytm, BHIM", style = Typography.labelSmall)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(PrimaryGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = DarkCharcoal, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Pay Now
            item {
                Spacer(modifier = Modifier.height(12.dp))
                InvexxButton(
                    text = "Pay Now",
                    onClick = { viewModel.initiateDeposit() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = depositState !is UiState.Loading,
                    isLoading = depositState is UiState.Loading,
                    testTag = "pay_now_button"
                )
            }
        }
    }
}

/**
 * WITHDRAW SCREEN
 */
@Composable
fun WithdrawScreen(
    navController: NavController,
    viewModel: WithdrawViewModel,
    modifier: Modifier = Modifier
) {
    val balance by viewModel.balance.collectAsState()
    val holderName by viewModel.holderName.collectAsState()
    val bankName by viewModel.bankName.collectAsState()
    val accountNumber by viewModel.accountNumber.collectAsState()
    val ifscCode by viewModel.ifscCode.collectAsState()
    val withdrawAmount by viewModel.withdrawAmount.collectAsState()
    val withdrawState by viewModel.withdrawState.collectAsState()
    val isBankSaved by viewModel.isBankSaved.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshBalance()
    }

    LaunchedEffect(withdrawState) {
        if (withdrawState is UiState.Success) {
            android.widget.Toast.makeText(context, (withdrawState as UiState.Success<String>).data, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetWithdrawState()
            viewModel.updateWithdrawAmount("")
        } else if (withdrawState is UiState.Error) {
            android.widget.Toast.makeText(context, (withdrawState as UiState.Error).message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetWithdrawState()
        }
    }

    Scaffold(
        topBar = { FeatureTopAppBar("Withdrawal", navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Available Balance
            item {
                InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Available Balance", style = Typography.bodyMedium.copy(color = MediumGray))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹${"%.2f".format(balance)}",
                            style = Typography.displayLarge.copy(fontSize = 32.sp, color = PrimaryGold)
                        )
                    }
                }
            }

            // Bank credentials Form or Locked Card
            item {
                if (isBankSaved) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Linked Bank Card", style = Typography.titleMedium)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Saved",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Your account is saved",
                                    style = Typography.labelSmall.copy(color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "EDIT",
                                    style = Typography.labelSmall.copy(color = PrimaryGold, fontWeight = FontWeight.ExtraBold),
                                    modifier = Modifier.clickable { viewModel.enableBankEdit() }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Golden Luxury Debit Card representation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF3E2D12), // Deep premium dark gold
                                            Color(0xFF1E1708)  // Dark carbon slate
                                        )
                                    )
                                )
                                .border(1.5.dp, Brush.horizontalGradient(listOf(PrimaryGold, Color(0xFFFFF7C2))), RoundedCornerShape(20.dp))
                                .padding(20.dp)
                        ) {
                            // Chip and Card Brand row
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Golden Card Chip representation
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp, 30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(Color(0xFFFFF4B2), PrimaryGold)
                                                )
                                            )
                                            .border(0.5.dp, PureWhite, RoundedCornerShape(6.dp))
                                    )
                                    // Bank Name Display
                                    Text(
                                        text = bankName.uppercase(),
                                        style = Typography.titleMedium.copy(
                                            color = PrimaryGold,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.5.sp
                                        )
                                    )
                                }
                                
                                // Account Number representation masked elegantly
                                val maskedAcc = remember(accountNumber) {
                                    if (accountNumber.length > 4) {
                                        "••••  ••••  ••••  " + accountNumber.takeLast(4)
                                    } else {
                                        "••••  ••••  ••••  $accountNumber"
                                    }
                                }
                                Text(
                                    text = maskedAcc,
                                    style = Typography.headlineMedium.copy(
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        fontSize = 20.sp
                                    ),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )

                                // Footer row (Holder Name & IFSC)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text("CARD HOLDER", style = Typography.labelSmall.copy(color = MediumGray, fontSize = 9.sp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = holderName.uppercase(),
                                            style = Typography.bodyMedium.copy(color = PureWhite, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("IFSC CODE", style = Typography.labelSmall.copy(color = MediumGray, fontSize = 9.sp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = ifscCode.uppercase(),
                                            style = Typography.bodyMedium.copy(color = PrimaryGold, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Enter Bank Account Details", style = Typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Account holder name
                            InvexxTextField(
                                value = holderName,
                                onValueChange = { viewModel.updateHolderName(it) },
                                hintText = "Account Holder Name",
                                leadingIcon = Icons.Default.Lock,
                                testTag = "withdraw_holder_input"
                            )

                            // Bank Name (Manually Added, No selection / dropdown picker)
                            InvexxTextField(
                                value = bankName,
                                onValueChange = { viewModel.updateBankName(it) },
                                hintText = "Enter Bank Name (e.g. HDFC Bank, SBI)",
                                leadingIcon = Icons.Default.Home,
                                testTag = "withdraw_bank_input"
                            )

                            // Account Number
                            InvexxTextField(
                                value = accountNumber,
                                onValueChange = { viewModel.updateAccountNumber(it) },
                                hintText = "Bank Account Number",
                                leadingIcon = Icons.Default.Lock,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                testTag = "withdraw_acc_input"
                            )

                            // IFSC Code
                            InvexxTextField(
                                value = ifscCode,
                                onValueChange = { viewModel.updateIfscCode(it.uppercase()) },
                                hintText = "IFSC Code (11 alphanumeric digits)",
                                leadingIcon = Icons.Default.Lock,
                                testTag = "withdraw_ifsc_input"
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // One-time save button
                            InvexxButton(
                                text = "Lock & Save Bank Account",
                                onClick = {
                                    viewModel.saveBankDetails(holderName, bankName, accountNumber, ifscCode)
                                },
                                isLoading = withdrawState is UiState.Loading,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(18.dp), tint = DarkCharcoal)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            )
                        }
                    }
                }
            }

            // Amount Input
            item {
                Text("Withdrawal Amount", style = Typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                InvexxTextField(
                    value = withdrawAmount,
                    onValueChange = { viewModel.updateWithdrawAmount(it) },
                    hintText = "Minimum withdrawal ₹100",
                    leadingIcon = Icons.Default.Lock,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    testTag = "withdraw_amount_input"
                )

                // Quick presets
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(200f, 500f, 1000f)
                    presets.forEach { amt ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PureWhite)
                                .border(1.dp, LightGrayBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateWithdrawAmount(amt.toInt().toString()) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("₹${amt.toInt()}", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Info box instructions
            item {
                InvexxCard(borderRadius = 12.dp, padding = 12.dp, backgroundColor = GoldenLight) {
                    Text(
                        "📌 Withdrawal Instructions:\n" +
                        "1. Processing hours: 9 AM to 6 PM daily.\n" +
                        "2. Minimum payout limit: ₹100.00.\n" +
                        "3. Withdrawals are processed instantly but can take up to 24 hours depending on bank settlement speeds.",
                        style = Typography.bodyMedium.copy(fontSize = 11.sp, color = DarkCharcoal, lineHeight = 16.sp)
                    )
                }
            }

            // Withdraw Button
            item {
                InvexxButton(
                    text = "Withdraw Now",
                    onClick = { viewModel.requestWithdrawal() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = withdrawState !is UiState.Loading,
                    isLoading = withdrawState is UiState.Loading,
                    testTag = "withdraw_submit_button"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * GIFT CODE SCREEN
 */
@Composable
fun GiftCodeScreen(
    navController: NavController,
    viewModel: ListsViewModel,
    modifier: Modifier = Modifier
) {
    var giftCodeInput by remember { mutableStateOf("") }
    val redeemState by viewModel.redeemState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(redeemState) {
        if (redeemState is UiState.Success) {
            android.widget.Toast.makeText(context, (redeemState as UiState.Success<String>).data, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetRedeemState()
            giftCodeInput = ""
        } else if (redeemState is UiState.Error) {
            android.widget.Toast.makeText(context, (redeemState as UiState.Error).message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetRedeemState()
        }
    }

    Scaffold(
        topBar = { FeatureTopAppBar("Gift Code Redeem", navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Elegant Vector Gift Box Illustration
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(GoldenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock, // Giftbox indicator fallback
                    contentDescription = "Gift Box",
                    tint = PrimaryGold,
                    modifier = Modifier.size(64.dp)
                )
            }

            Text(
                "Have a promo/gift code?\nRedeem it instantly and add funds to your bonus wallet!",
                style = Typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            InvexxTextField(
                value = giftCodeInput,
                onValueChange = { giftCodeInput = it },
                hintText = "Enter promo code (e.g. INVEXX100)",
                leadingIcon = Icons.Default.Lock,
                testTag = "gift_code_input"
            )

            InvexxButton(
                text = "Redeem Reward",
                onClick = { viewModel.redeemGiftCode(giftCodeInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = redeemState !is UiState.Loading,
                isLoading = redeemState is UiState.Loading,
                testTag = "gift_redeem_button"
            )

            // list of redeemed history
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tip: Try code \"INVEXX100\" to instantly claim ₹100.00!", style = Typography.labelSmall.copy(color = PrimaryGold, fontWeight = FontWeight.Bold))
        }
    }
}

/**
 * TASK SCREEN
 */
@Composable
fun TaskScreen(
    navController: NavController,
    viewModel: ListsViewModel,
    modifier: Modifier = Modifier
) {
    val tasksState by viewModel.tasks.collectAsState()
    val claimingTaskId by viewModel.claimingTaskId.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTasks()
    }

    Scaffold(
        topBar = { FeatureTopAppBar("Daily Tasks", navController) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Complete daily activities to unlock bonus rewards!",
                    style = Typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            when (tasksState) {
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
                    item { Text("Failed to load tasks") }
                }
                is UiState.Success -> {
                    val tasks = (tasksState as UiState.Success<List<TaskModel>>).data
                    items(tasks) { task ->
                        InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Task Icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(GoldenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Check Icon",
                                        tint = PrimaryGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        task.title,
                                        style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        task.subtitle,
                                        style = Typography.bodyMedium.copy(fontSize = 12.sp, color = MediumGray)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Right Claim Button
                                if (task.isClaimed) {
                                    Box(
                                        modifier = Modifier
                                            .width(90.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(LightGrayBorder),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Claimed", style = Typography.labelSmall.copy(color = MediumGray, fontWeight = FontWeight.Bold))
                                    }
                                } else if (task.id == 1 && !task.isClaimable) {
                                    InvexxButton(
                                        text = "Check-In",
                                        onClick = { viewModel.checkInUser() },
                                        enabled = claimingTaskId == null,
                                        modifier = Modifier
                                            .width(90.dp)
                                            .height(36.dp)
                                    )
                                } else {
                                    InvexxButton(
                                        text = "Claim",
                                        onClick = { viewModel.claimTaskReward(task) },
                                        enabled = task.isClaimable && claimingTaskId == null,
                                        isLoading = claimingTaskId == task.id,
                                        modifier = Modifier
                                            .width(90.dp)
                                            .height(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            // Progress bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Progress", style = Typography.labelSmall)
                                Text("${(task.progress * 100).toInt()}%", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            GoldenProgressBar(progress = task.progress)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Reward: +₹${"%.2f".format(task.rewardAmount)}", style = Typography.labelSmall.copy(color = PrimaryGold, fontWeight = FontWeight.Bold))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/**
 * NOTIFICATION SCREEN
 */
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: ListsViewModel,
    modifier: Modifier = Modifier
) {
    val notifsState by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Scaffold(
        topBar = { FeatureTopAppBar("Notifications", navController) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                        Text("Mark all as read", color = PrimaryGold, fontWeight = FontWeight.Bold)
                    }
                }
            }

            when (notifsState) {
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
                is UiState.Error -> { item { Text("No notifications available") } }
                is UiState.Success -> {
                    val list = (notifsState as UiState.Success<List<NotificationModel>>).data
                    items(list) { notif ->
                        InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Unread Indicator dot
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (notif.isRead) Color.Transparent else PrimaryGold)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        notif.title,
                                        style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        notif.message,
                                        style = Typography.bodyMedium.copy(color = MediumGray, fontSize = 13.sp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        notif.date,
                                        style = Typography.labelSmall.copy(fontSize = 10.sp)
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
 * RANK / VIP BENEFITS SCREEN
 */
@Composable
fun RankBenefitsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    val userVipLevel = prefs.vipLevel

    val api = remember { ServiceLocator.getApiService(context) }
    var vipBenefits by remember { mutableStateOf<List<VipBenefitModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        val res = api.getVipBenefits()
        if (res.status == "success" && res.data != null) {
            vipBenefits = res.data
        } else {
            vipBenefits = listOf(
                VipBenefitModel("VIP0", "Standard User on Registration", "Direct Commission: 50%")
            )
        }
    }

    Scaffold(
        topBar = { FeatureTopAppBar("VIP Rank Benefits", navController) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Upgrade your affiliate tier to unlock dynamic performance bonuses and direct referral shares of up to 60%.",
                    style = Typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(vipBenefits) { benefit ->
                val isCurrent = benefit.level == "VIP$userVipLevel"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isCurrent) Color(0xFFFFFAD6) else PureWhite)
                        .border(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) PrimaryGold else LightGrayBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PrimaryGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "VIP", tint = PureWhite, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    benefit.level,
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PrimaryGold)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Current Rank", style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkCharcoal))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(benefit.requirement, style = Typography.bodyMedium.copy(fontSize = 12.sp, color = MediumGray))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(benefit.benefitDesc, style = Typography.labelSmall.copy(color = PrimaryGold, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

/**
 * ORDER HISTORY SCREEN
 */
@Composable
fun OrderHistoryScreen(
    navController: NavController,
    viewModel: ListsViewModel,
    modifier: Modifier = Modifier
) {
    val ordersState by viewModel.orders.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    Scaffold(
        topBar = { FeatureTopAppBar("My Investments", navController) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Filter tabs: All, Active, Completed, Expired
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Active", "Completed", "Expired")
                    filters.forEach { filter ->
                        val active = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (active) PrimaryGold else PureWhite)
                                .border(1.dp, if (active) Color.Transparent else LightGrayBorder, RoundedCornerShape(50.dp))
                                .clickable { selectedFilter = filter },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                filter,
                                style = Typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) DarkCharcoal else MediumGray
                                )
                            )
                        }
                    }
                }
            }

            when (ordersState) {
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
                is UiState.Error -> { item { Text("No investment history found") } }
                is UiState.Success -> {
                    val rawOrders = (ordersState as UiState.Success<List<OrderModel>>).data
                    val filtered = if (selectedFilter == "All") rawOrders else rawOrders.filter { it.status == selectedFilter }
                    
                    if (filtered.isEmpty()) {
                        item {
                            Text("No records found", style = Typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
                        }
                    }

                    items(filtered) { order ->
                        InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Order", tint = PrimaryGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(order.planName, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryGold)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(order.status, style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = LightGrayBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Invested Sum", style = Typography.labelSmall)
                                    Text("₹${"%.2f".format(order.investAmount)}", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Daily Earning", style = Typography.labelSmall)
                                    Text("₹${"%.2f".format(order.dailyEarnings)}", style = Typography.labelLarge.copy(color = PrimaryGold, fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Total Yield", style = Typography.labelSmall)
                                    Text("₹${"%.2f".format(order.totalReturn)}", style = Typography.labelLarge.copy(color = PrimaryGold, fontWeight = FontWeight.Bold))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Term: ${order.startDate} to ${order.endDate}", style = Typography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/**
 * FUND HISTORY SCREEN
 */
@Composable
fun FundHistoryScreen(
    navController: NavController,
    viewModel: ListsViewModel,
    modifier: Modifier = Modifier
) {
    val txsState by viewModel.transactions.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        viewModel.loadTransactions()
    }

    Scaffold(
        topBar = { FeatureTopAppBar("Transaction Ledger", navController) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter pills: All, Deposit, Withdrawal, Commission, Bonus
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Deposit", "Withdrawal", "Commission", "Bonus")
                    items(filters) { filter ->
                        val active = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (active) PrimaryGold else PureWhite)
                                .border(1.dp, if (active) Color.Transparent else LightGrayBorder, RoundedCornerShape(50.dp))
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                filter,
                                style = Typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) DarkCharcoal else MediumGray
                                )
                            )
                        }
                    }
                }
            }

            when (txsState) {
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
                is UiState.Error -> { item { Text("No transactions recorded") } }
                is UiState.Success -> {
                    val rawList = (txsState as UiState.Success<List<TransactionModel>>).data
                    val filtered = if (selectedFilter == "All") {
                        rawList
                    } else {
                        rawList.filter { it.type.equals(selectedFilter, ignoreCase = true) }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Text("No ledger entries found", style = Typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
                        }
                    }

                    items(filtered) { tx ->
                        val isPositive = tx.type != "withdrawal"
                        InvexxCard(borderRadius = 12.dp, padding = 12.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(GoldenLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPositive) Icons.Default.Check else Icons.Default.Lock,
                                            contentDescription = "Tx Type",
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.description,
                                            style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        if (!tx.orderNo.isNullOrBlank()) {
                                            Text("Order: ${tx.orderNo}", style = Typography.labelSmall.copy(fontSize = 10.sp, color = MediumGray))
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(tx.date, style = Typography.labelSmall.copy(fontSize = 10.sp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${tx.status}",
                                                style = Typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when(tx.status.lowercase()) {
                                                        "success" -> GreenSuccess
                                                        "pending" -> PrimaryGold
                                                        else -> RedError
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "${if (isPositive) "+" else "-"}₹${"%.2f".format(tx.amount)}",
                                    style = Typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isPositive) GreenSuccess else RedError
                                    ),
                                    maxLines = 1
                                )
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
 * BLOG DETAIL SCREEN
 */
@Composable
fun BlogDetailScreen(
    blogId: Int,
    navController: NavController,
    viewModel: BlogViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val blogsState by viewModel.blogsState.collectAsState()
    val blogItem = when (val state = blogsState) {
        is UiState.Success -> state.data.find { it.id == blogId }
        else -> null
    }

    LaunchedEffect(Unit) {
        viewModel.loadBlogs()
    }

    Scaffold(
        topBar = { FeatureTopAppBar("Article Details", navController) },
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (blogItem == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGold)
            }
        } else {
            val blog = blogItem!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Image full width, height 200, corner 0
                item {
                    AsyncImage(
                        model = blog.imageUrl,
                        contentDescription = blog.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(LightGrayBorder),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.app_icon)
                    )
                }

                // Header Metadata
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = "Date", tint = PrimaryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(blog.date, style = Typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            blog.title,
                            style = Typography.headlineMedium.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = LightGrayBorder)
                    }
                }

                // Full content
                item {
                    Text(
                        text = blog.content,
                        style = Typography.bodyLarge.copy(
                            lineHeight = 26.sp,
                            color = DarkCharcoal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 32.dp)
                    )
                }
            }
        }
    }
}

/**
 * ABOUT INVEXX SCREEN
 * Fully in English, describing the value proposition and core operations of INVEXX.
 */
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = { FeatureTopAppBar(title = "About INVEXX", navController = navController) },
        containerColor = SoftBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFF4B2), Color(0xFFFFFDF5))
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        InvexxLogo(modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "INVEXX WEALTH",
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = DarkCharcoal)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Secure. Transparent. Future-Proof.",
                            style = Typography.bodyMedium.copy(color = MediumGray, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            item {
                InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Who We Are",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal)
                        )
                        Text(
                            text = "INVEXX is a leading global investment platform specializing in diversified high-yield financial portfolios. Our mission is to democratize premium wealth generation, allowing retail investors to tap into institutional-grade Fixed Funds, Welfare Funds, and Yearly Funds with institutional-level security and maximum flexibility.",
                            style = Typography.bodyMedium.copy(lineHeight = 22.sp, color = DarkCharcoal)
                        )
                    }
                }
            }

            item {
                InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Our Core Pillars",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal)
                        )
                        
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = "Security", tint = PrimaryGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Advanced Security Protocols", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text("Your funds and returns are protected by robust escrow networks and secure blockchain transaction registers, ensuring 100% security against unauthorized access.", style = Typography.bodyMedium.copy(color = MediumGray))
                            }
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = "Returns", tint = PrimaryGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Guaranteed Wealth Growth", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text("With systematic plans backed by energy assets, commercial technologies, and market-hedged index structures, we ensure your daily dividends are credited like clockwork.", style = Typography.bodyMedium.copy(color = MediumGray))
                            }
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Default.People, contentDescription = "Affiliate", tint = PrimaryGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Dynamic Affiliate Rewards", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text("Grow together with our multi-level partner programs. Earn up to 35% in direct deposit commissions and secondary team dividends, boosting your passive income.", style = Typography.bodyMedium.copy(color = MediumGray))
                            }
                        }
                    }
                }
            }

            item {
                InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("INVEXX App Version", style = Typography.labelSmall.copy(color = MediumGray))
                        Text("v${com.example.BuildConfig.VERSION_NAME} Premium (Gold Edition)", style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal))
                        Text("© 2026 INVEXX International Ltd. All rights reserved.", style = Typography.labelSmall.copy(color = MediumGray), textAlign = TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * SETTINGS SCREEN
 * Allows basic configuration, security information, language choosing (English), and account verification details.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    val scope = rememberCoroutineScope()

    var pushNotificationsEnabled by remember { mutableStateOf(prefs.pushNotificationsEnabled) }
    var transactionPinsEnabled by remember { mutableStateOf(prefs.transactionPinsEnabled) }
    var biometricsEnabled by remember { mutableStateOf(prefs.biometricsEnabled) }
    var appLanguage by remember { mutableStateOf(prefs.appLanguage) }

    // Dialog flags
    var showBiometricsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // Password reset fields
    var oldPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    // Dialog for Biometric setup
    if (showBiometricsDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "Biometrics", tint = PrimaryGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register Biometrics", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column {
                    Text(
                        text = "To enable quick sign-in, confirm by touching your device's fingerprint sensor or allowing face recognition.",
                        style = Typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Sensor Scan",
                            tint = PrimaryGold,
                            modifier = Modifier
                                .size(64.dp)
                                .clickable {
                                    // Simulate successful scan
                                    biometricsEnabled = true
                                    prefs.biometricsEnabled = true
                                    showBiometricsDialog = false
                                    android.widget.Toast.makeText(context, "Biometrics enabled successfully! 🔒", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                    Text(
                        text = "Tap fingerprint icon to simulate verification scan",
                        style = Typography.labelSmall.copy(color = MediumGray),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Fast bypass
                        biometricsEnabled = true
                        prefs.biometricsEnabled = true
                        showBiometricsDialog = false
                        android.widget.Toast.makeText(context, "Biometrics enabled successfully! 🔒", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Verify", color = PrimaryGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricsDialog = false }) {
                    Text("Cancel", color = DarkCharcoal)
                }
            }
        )
    }

    // Dialog for Language choice
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text("Select App Language", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val languages = listOf("English", "Hindi (हिन्दी)", "Spanish (Español)")
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appLanguage = lang
                                    prefs.appLanguage = lang
                                    showLanguageDialog = false
                                    android.widget.Toast.makeText(context, "Language changed to $lang!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lang, style = Typography.bodyLarge)
                            if (appLanguage == lang) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = PrimaryGold)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Dialog for Password Change
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = {
                Text("Reset Account Password", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (passwordError.isNotEmpty()) {
                        Text(passwordError, style = Typography.bodyMedium.copy(color = Color.Red))
                    }
                    OutlinedTextField(
                        value = oldPasswordInput,
                        onValueChange = { oldPasswordInput = it },
                        label = { Text("Old Password") },
                        placeholder = { Text("Enter current password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password") },
                        placeholder = { Text("Enter new password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text("Confirm New Password") },
                        placeholder = { Text("Re-enter new password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPasswordInput != prefs.accountPassword) {
                            passwordError = "Incorrect Old Password!"
                        } else if (newPasswordInput.isBlank()) {
                            passwordError = "New Password cannot be empty!"
                        } else if (newPasswordInput != confirmPasswordInput) {
                            passwordError = "New passwords do not match!"
                        } else {
                            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance("https://prime-khatab-default-rtdb.firebaseio.com").getReference("users")
                            scope.launch {
                                try {
                                    if (currentUser != null) {
                                        currentUser.updatePassword(newPasswordInput).await()
                                    }
                                    if (prefs.phone.isNotBlank()) {
                                        dbRef.child(prefs.phone).child("password").setValue(newPasswordInput).await()
                                        try {
                                            val notifRef = dbRef.child(prefs.phone).child("notifications")
                                            val notifSnapshot = notifRef.get().await()
                                            val notifList = mutableListOf<Map<String, Any>>()
                                            var nextId = 1
                                            if (notifSnapshot.exists()) {
                                                for (child in notifSnapshot.children) {
                                                    val nid = child.child("id").getValue(Int::class.java) ?: nextId
                                                    val nTitle = child.child("title").getValue(String::class.java) ?: ""
                                                    val nMessage = child.child("message").getValue(String::class.java) ?: ""
                                                     val nDate = child.child("date").getValue(String::class.java) ?: ""
                                                     val nRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                                                     val nTimestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                                                     
                                                     notifList.add(mapOf(
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
                                                 "title" to "Security Alert! 🔐",
                                                 "message" to "Your account password has been successfully changed.",
                                                 "date" to "Just now",
                                                 "isRead" to false,
                                                 "timestamp" to System.currentTimeMillis()
                                             )
                                             notifList.add(0, newNotif)
                                             notifRef.setValue(notifList).await()
                                         } catch (ne: Exception) {}
                                    }
                                    prefs.accountPassword = newPasswordInput
                                    showPasswordDialog = false
                                    android.widget.Toast.makeText(context, "Password updated successfully! 🔐", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    passwordError = e.message ?: "Failed to sync password to Firebase."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                ) {
                    Text("Save Changes", color = DarkCharcoal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel", color = DarkCharcoal)
                }
            }
        )
    }

    Scaffold(
        topBar = { FeatureTopAppBar(title = "Settings", navController = navController) },
        containerColor = SoftBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Preferences",
                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MediumGray)
                )
            }

            item {
                InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Notifications, contentDescription = "Push", tint = PrimaryGold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Push Notifications", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Receive daily earnings alerts", style = Typography.bodyMedium.copy(color = MediumGray))
                                }
                            }
                            Switch(
                                checked = pushNotificationsEnabled,
                                onCheckedChange = { checked ->
                                    pushNotificationsEnabled = checked
                                    prefs.pushNotificationsEnabled = checked
                                    val msg = if (checked) "Push Notifications enabled! 🔔" else "Push Notifications disabled."
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryGold)
                            )
                        }
                        
                        HorizontalDivider(color = LightGrayBorder, modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Translate, contentDescription = "Language", tint = PrimaryGold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("App Language", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Currently: $appLanguage", style = Typography.bodyMedium.copy(color = MediumGray))
                                }
                            }
                            Text(appLanguage, style = Typography.bodyMedium.copy(color = PrimaryGold, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Security & Protection",
                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MediumGray)
                )
            }

            item {
                InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = "PIN", tint = PrimaryGold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Withdrawal PIN", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Require security PIN on payments", style = Typography.bodyMedium.copy(color = MediumGray))
                                }
                            }
                            Switch(
                                checked = transactionPinsEnabled,
                                onCheckedChange = { checked ->
                                    transactionPinsEnabled = checked
                                    prefs.transactionPinsEnabled = checked
                                    val msg = if (checked) "Withdrawal PIN protection active! 🔒" else "Withdrawal PIN protection inactive."
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryGold)
                            )
                        }

                        HorizontalDivider(color = LightGrayBorder, modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    oldPasswordInput = ""
                                    newPasswordInput = ""
                                    confirmPasswordInput = ""
                                    passwordError = ""
                                    showPasswordDialog = true
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.VpnKey, contentDescription = "Password", tint = PrimaryGold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Reset Account Password", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Change your login credential safely", style = Typography.bodyMedium.copy(color = MediumGray))
                                }
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Reset", tint = MediumGray)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Account Verification",
                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MediumGray)
                )
            }

            item {
                InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("KYC Identity Verified", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text("Full wallet features unlocked", style = Typography.bodyMedium.copy(color = MediumGray))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Active", style = Typography.labelSmall.copy(color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * SUPPORT / ONLINE CUSTOMER SERVICE SCREEN
 * Provides FAQs and instant access links to official customer services.
 */
@Composable
fun SupportScreen(navController: NavController) {
    val context = LocalContext.current
    var telegramUrl by remember { mutableStateOf("https://t.me/invexx_official") }
    var whatsappUrl by remember { mutableStateOf("https://wa.me/919999999999") }
    var supportEmail by remember { mutableStateOf("support@invexx-wealth.com") }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    LaunchedEffect(Unit) {
        try {
            val api = ServiceLocator.getApiService(context)
            val settings = api.getSystemSettings()
            if (settings.status == "success" && settings.data != null) {
                telegramUrl = settings.data["telegram_url"]?.toString() ?: "https://t.me/invexx_official"
                whatsappUrl = settings.data["whatsapp_url"]?.toString() ?: "https://wa.me/919999999999"
                supportEmail = settings.data["support_email"]?.toString() ?: "support@invexx-wealth.com"
            }
        } catch (e: Exception) {
            // Keep default fallbacks
        }
    }

    Scaffold(
        topBar = { FeatureTopAppBar(title = "Customer Support", navController = navController) },
        containerColor = SoftBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFF4B2), Color(0xFFFFFDF5))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "Need Help? 💬",
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = DarkCharcoal)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Our premium support services are online 24/7. Connect with us directly for fast resolution of your deposit, withdrawal, or system inquiries.",
                            style = Typography.bodyMedium.copy(lineHeight = 20.sp, color = DarkCharcoal)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Official Customer Service",
                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MediumGray)
                )
            }

            item {
                InvexxCard(borderRadius = 16.dp, padding = 12.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        uriHandler.openUri(telegramUrl)
                                    } catch (e: Exception) {}
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE3F2FD)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = "Telegram", tint = Color(0xFF2196F3))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Official Telegram Channel", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Real-time corporate updates & welfares", style = Typography.bodyMedium.copy(color = MediumGray))
                                }
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = MediumGray)
                        }

                        HorizontalDivider(color = LightGrayBorder)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        uriHandler.openUri(whatsappUrl)
                                    } catch (e: Exception) {}
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = "WhatsApp", tint = Color(0xFF4CAF50))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("VIP WhatsApp Hot-line", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("Fast response for VIP rank holders", style = Typography.bodyMedium.copy(color = MediumGray))
                                }
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = MediumGray)
                        }

                        HorizontalDivider(color = LightGrayBorder)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        uriHandler.openUri("mailto:$supportEmail")
                                    } catch (e: Exception) {}
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFF3E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = Color(0xFFFF9800))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Official Support Email", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text(supportEmail, style = Typography.bodyMedium.copy(color = MediumGray))
                                }
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = MediumGray)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Frequently Asked Questions",
                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MediumGray)
                )
            }

            item {
                InvexxCard(borderRadius = 16.dp, padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text("Q: How long does a deposit take to reflect?", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal))
                            Text("A: Standard UPI deposits are verified automatically within 1 to 5 minutes. If your balance is delayed, please contact support with your UPI reference number (UTR).", style = Typography.bodyMedium.copy(color = MediumGray))
                        }
                        HorizontalDivider(color = LightGrayBorder)
                        Column {
                            Text("Q: What is the minimum withdrawal amount?", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = DarkCharcoal))
                            Text("A: The minimum withdrawal is ₹200. Withdrawals are processed daily between 10:00 AM and 6:00 PM directly to your registered bank account.", style = Typography.bodyMedium.copy(color = MediumGray))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
