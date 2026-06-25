package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.R
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.UiState

/**
 * SPLASH SCREEN
 * Animates a loading progress bar from 0 to 100 over 2.5 seconds, then routes to Login.
 * Features a gorgeous glassmorphic center circle, decorative gold blobs, and the INVEXX custom logo.
 */
@Composable
fun SplashScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val progressAnimation by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 2500),
        label = "progress"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    if (progressAnimation >= 1f) {
        LaunchedEffect(key1 = true) {
            val prefs = com.example.data.PreferenceManager(context)
            val dest = if (!prefs.token.isNullOrBlank()) "home" else "login"
            navController.navigate(dest) {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftBackground)
    ) {
        // Floating decorative yellow circles scattered around the screen
        // Top-left large (200dp)
        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = (-50).dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(PrimaryGold.copy(alpha = 0.8f), Color.Transparent)))
        )
        // Top-right medium overlapping white
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-20).dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(SecondaryGold.copy(alpha = 0.7f), Color.Transparent)))
        )
        // Bottom-left small
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 80.dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(PrimaryGold.copy(alpha = 0.6f), Color.Transparent)))
        )
        // Bottom-right large yellow blob
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .size(280.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(SecondaryGold.copy(alpha = 0.85f), Color.Transparent)))
        )

        // Center Content Container
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Glassmorphic white circle in center (radius fills 65% of screen width)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f)
                    .drawBehind {
                        // Soft outer glass neumorphic shadow
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.03f),
                            radius = size.width / 2f + 16f,
                            center = center
                        )
                    }
                    .clip(CircleShape)
                    .background(PureWhite.copy(alpha = 0.95f))
                    .border(1.dp, PureWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // INVEXX custom Canvas Logo
                    InvexxLogo(
                        modifier = Modifier
                            .size(75.dp)
                            .testTag("splash_logo")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "INVEXX",
                        style = Typography.displayLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "INVEST  |  EARN  |  GROW",
                        style = Typography.labelSmall.copy(
                            color = MediumGray,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Loading Text & Progress bar
            Text(
                text = "Loading...",
                style = Typography.bodyMedium.copy(color = MediumGray)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Custom progress bar width 200dp, height 6dp, rounded, golden fill, gray track
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(LightGrayBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnimation)
                        .background(Brush.horizontalGradient(listOf(PrimaryGold, SecondaryGold)))
                )
            }
        }
    }
}

/**
 * LOGIN SCREEN
 * Features decorative blobs, username/password fields with square leading icons,
 * circular remember me, forgot password action, and full-width golden button.
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    val loginState by viewModel.loginState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(loginState) {
        if (loginState is UiState.Success) {
            android.widget.Toast.makeText(context, "Login Successful!", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetStates()
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        } else if (loginState is UiState.Error) {
            val err = (loginState as UiState.Error).message
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
            viewModel.resetStates()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftBackground)
    ) {
        // Decorative blobs in corners
        Box(
            modifier = Modifier
                .offset(x = (-40).dp, y = (-40).dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(PrimaryGold.copy(alpha = 0.5f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .size(200.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(SecondaryGold.copy(alpha = 0.5f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo & Header Group
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logo Circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .border(1.dp, LightGrayBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    InvexxLogo(modifier = Modifier.size(56.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome Back!",
                    style = Typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Login to continue your journey.",
                    style = Typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Fields Card
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Phone input field
                InvexxTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    hintText = "Enter phone number",
                    leadingIcon = Icons.Default.Phone,
                    leadingText = "+91",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    testTag = "login_phone_input"
                )

                // Password input field
                InvexxTextField(
                    value = password,
                    onValueChange = { password = it },
                    hintText = "Enter password",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle Password Visibility",
                                tint = MediumGray,
                                modifier = Modifier.size(20.dp).alpha(0.5f)
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    testTag = "login_password_input"
                )

                // Remember me & Forgot password row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (rememberMe) PrimaryGold else Color.Transparent)
                                .border(1.5.dp, PrimaryGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rememberMe) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Checked",
                                    tint = PureWhite,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Remember me",
                            style = Typography.bodyMedium.copy(fontSize = 13.sp)
                        )
                    }

                    Text(
                        text = "Forgot password?",
                        style = Typography.bodyMedium.copy(
                            color = PrimaryGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.clickable { navController.navigate("forgot_password") }
                    )
                }

                // Error Message if any
                if (loginState is UiState.Error) {
                    Text(
                        text = (loginState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Login Button
                InvexxButton(
                    text = "Login",
                    onClick = { viewModel.login(phone, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = loginState !is UiState.Loading,
                    isLoading = loginState is UiState.Loading,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Login",
                            tint = DarkCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    testTag = "login_button"
                )

                // Sign Up Clickable Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        style = Typography.bodyMedium.copy(color = MediumGray)
                    )
                    Text(
                        text = "Sign Up",
                        style = Typography.bodyMedium.copy(
                            color = PrimaryGold,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable {
                            navController.navigate("register")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer row 100% Secure
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Shield Secure",
                    tint = PrimaryGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your data is 100% secure with us",
                    style = Typography.labelSmall.copy(color = MediumGray, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

/**
 * REGISTER SCREEN
 * Back navigation button inside a white shadow circle. Title framed by golden sparkle symbols.
 * 5 distinct fields, Terms/Privacy agreement, register button, and secure footer.
 */
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var phone by remember { mutableStateOf("") }
    val autoUserId = remember(phone) {
        if (phone.length >= 4) "Inv_" + phone.takeLast(4) else "User_" + (1000..9999).random()
    }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val registerState by viewModel.registerState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(registerState) {
        if (registerState is UiState.Success) {
            android.widget.Toast.makeText(context, "Registration Successful!", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetStates()
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else if (registerState is UiState.Error) {
            val err = (registerState as UiState.Error).message
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
            viewModel.resetStates()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftBackground)
    ) {
        // Decorative Blobs
        Box(
            modifier = Modifier
                .offset(x = (-30).dp, y = (-30).dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(PrimaryGold.copy(alpha = 0.4f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AppBar Row with Back Arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .clickable { navController.popBackStack() }
                        .drawBehind {
                            drawCircle(color = ShadowColor, radius = size.width / 2f + 4f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkCharcoal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logo & Title
            InvexxLogo(modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨ ", fontSize = 18.sp)
                Text(
                    text = "Create Your Account",
                    style = Typography.headlineMedium.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.ExtraBold
                )
                Text(" ✨", fontSize = 18.sp)
            }
            Text(
                text = "Join Invexx and start your journey today",
                style = Typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5 Input Fields
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Field 1: Phone
                InvexxTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    hintText = "Enter phone number",
                    leadingIcon = Icons.Default.Phone,
                    leadingText = "+91",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    testTag = "reg_phone_input"
                )

                // Field 3: Create password
                InvexxTextField(
                    value = password,
                    onValueChange = { password = it },
                    hintText = "Create password",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle",
                                tint = MediumGray,
                                modifier = Modifier.size(20.dp).alpha(0.5f)
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    testTag = "reg_password_input"
                )

                // Field 4: Confirm password
                InvexxTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    hintText = "Confirm password",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle",
                                tint = MediumGray,
                                modifier = Modifier.size(20.dp).alpha(0.5f)
                            )
                        }
                    },
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    testTag = "reg_confirm_password_input"
                )

                // Field 5: Referral code (Optional)
                InvexxTextField(
                    value = referralCode,
                    onValueChange = { referralCode = it },
                    hintText = "Referral code (Optional)",
                    leadingIcon = Icons.Default.Lock, // Giftbox indicator fallback
                    testTag = "reg_referral_input"
                )

                // Checkbox row T&C and Privacy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (agreeToTerms) PrimaryGold else Color.Transparent)
                            .border(1.5.dp, PrimaryGold, CircleShape)
                            .clickable { agreeToTerms = !agreeToTerms },
                        contentAlignment = Alignment.Center
                    ) {
                        if (agreeToTerms) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Checked",
                                tint = PureWhite,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("I agree to the ", style = Typography.bodyMedium.copy(fontSize = 12.sp, color = MediumGray))
                            Text(
                                "Terms & Conditions",
                                style = Typography.bodyMedium.copy(fontSize = 12.sp, color = PrimaryGold, fontWeight = FontWeight.Bold),
                                modifier = Modifier.clickable { /* T&C Click */ }
                            )
                            Text(" and", style = Typography.bodyMedium.copy(fontSize = 12.sp, color = MediumGray))
                        }
                        Text(
                            "Privacy Policy",
                            style = Typography.bodyMedium.copy(fontSize = 12.sp, color = PrimaryGold, fontWeight = FontWeight.Bold),
                            modifier = Modifier.clickable { /* Privacy Click */ }
                        )
                    }
                }

                // Error Message
                if (registerState is UiState.Error) {
                    Text(
                        text = (registerState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sign Up Pill Button
                InvexxButton(
                    text = "Sign Up",
                    onClick = {
                        if (password != confirmPassword) {
                            return@InvexxButton
                        }
                        viewModel.register(phone, autoUserId, password, referralCode)
                    },
                    enabled = agreeToTerms,
                    isLoading = registerState is UiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    testTag = "reg_button"
                )

                // Login click row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        style = Typography.bodyMedium.copy(color = MediumGray)
                    )
                    Text(
                        text = "Login",
                        style = Typography.bodyMedium.copy(
                            color = PrimaryGold,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable {
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Shield Secure",
                    tint = PrimaryGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your data is 100% secure with us",
                    style = Typography.labelSmall.copy(color = MediumGray, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(1) } // 1 = enter phone, 2 = enter otp & pass

    var currentVerificationToken by remember { mutableStateOf("") }
    var currentDeviceId by remember { mutableStateOf("") }

    val forgotPassState by viewModel.forgotPassState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(forgotPassState) {
        if (forgotPassState is UiState.Success) {
            val msg = (forgotPassState as UiState.Success).data
            if (msg.startsWith("OTP_SENT")) {
                val parts = msg.split("|")
                if (parts.size >= 3) {
                    currentVerificationToken = parts[1]
                    currentDeviceId = parts[2]
                }
                android.widget.Toast.makeText(context, "OTP Sent to $phone", android.widget.Toast.LENGTH_SHORT).show()
                step = 2
                viewModel.resetStates()
            } else if (msg == "PASSWORD_RESET_SUCCESS") {
                android.widget.Toast.makeText(context, "Password reset successful!", android.widget.Toast.LENGTH_SHORT).show()
                viewModel.resetStates()
                navController.popBackStack()
            }
        } else if (forgotPassState is UiState.Error) {
            val err = (forgotPassState as UiState.Error).message
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
            viewModel.resetStates()
        }
    }

    Scaffold(
        containerColor = SoftBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .border(1.dp, LightGrayBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(40.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Forgot Password",
                style = Typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (step == 1) "Enter your registered phone number to receive an OTP." else "Enter the OTP sent to $phone and your new password.",
                style = Typography.bodyMedium.copy(color = MediumGray),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            InvexxCard(borderRadius = 16.dp, padding = 20.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (step == 1) {
                        InvexxTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            hintText = "Phone Number",
                            leadingIcon = Icons.Default.Phone,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        
                        InvexxButton(
                            text = "Send OTP",
                            onClick = { viewModel.sendOtp(phone) },
                            isLoading = forgotPassState is UiState.Loading
                        )
                    } else {
                        InvexxTextField(
                            value = otp,
                            onValueChange = { otp = it },
                            hintText = "Enter 6-digit OTP",
                            leadingIcon = Icons.Default.Check,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        InvexxTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            hintText = "New Password",
                            leadingIcon = Icons.Default.Lock,
                            trailingIcon = {
                                val icon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(imageVector = icon, contentDescription = "Toggle password visibility", tint = MediumGray)
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        
                        InvexxButton(
                            text = "Reset Password",
                            onClick = { 
                                viewModel.verifyOtpAndResetPass(phone, otp, currentDeviceId, currentVerificationToken, newPassword)
                            },
                            isLoading = forgotPassState is UiState.Loading
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Back to Login",
                style = Typography.bodyMedium.copy(color = PrimaryGold, fontWeight = FontWeight.Bold),
                modifier = Modifier.clickable { navController.popBackStack() }
            )
        }
    }
}
