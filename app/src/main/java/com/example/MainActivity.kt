package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Person
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import com.example.ui.components.*
import androidx.compose.material.icons.filled.*
import com.example.ui.utils.tr
import com.example.data.PreferenceManager
import com.example.data.ServiceLocator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

object AppLockState {
    var isLocked by mutableStateOf(false)
}

class MainActivity : FragmentActivity() {
    private val channelId = "invexx_welcome_notifications"
    private var notificationsListener: ChildEventListener? = null
    private var announcementsListener: ChildEventListener? = null
    private var plansListener: ChildEventListener? = null
    private var blogsListener: ChildEventListener? = null
    private var paymentCallbacksListener: ChildEventListener? = null
    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "phone_number" || key == "phone" || key == "key_phone" || key == "user_phone") {
            startFirebaseNotificationListeners()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            sendWelcomeNotification()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        checkNotificationPermission()

        val prefs = PreferenceManager(this)
        prefs.registerListener(prefChangeListener)
        startFirebaseNotificationListeners()
        // Biometrics removed as per user instruction. No biometric lock overlay trigger.

        setContent {
            InvexxTheme {
                val context = LocalContext.current
                
                var minVersion by remember { mutableStateOf("1.0.0") }
                var latestVersion by remember { mutableStateOf("1.0.0") }
                var updateUrl by remember { mutableStateOf("https://invexx.app") }
                var upsetScreenEnabled by remember { mutableStateOf(false) }
                var upsetMessage by remember { mutableStateOf("Under maintenance.") }
                var isSettingsLoaded by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val databaseUrl = "https://prime-khatab-default-rtdb.firebaseio.com"
                    val ref = FirebaseDatabase.getInstance(databaseUrl).getReference("system_settings")
                    ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                minVersion = snapshot.child("minVersion").getValue(String::class.java) ?: "1.0.0"
                                latestVersion = snapshot.child("latestVersion").getValue(String::class.java) ?: "1.0.0"
                                updateUrl = snapshot.child("updateUrl").getValue(String::class.java) ?: "https://invexx.app"
                                upsetScreenEnabled = snapshot.child("upsetScreenEnabled").getValue(Boolean::class.java) ?: false
                                upsetMessage = snapshot.child("upsetMessage").getValue(String::class.java) ?: "Maintenance in progress."
                            }
                            isSettingsLoaded = true
                        }
                        override fun onCancelled(error: DatabaseError) {
                            isSettingsLoaded = true
                        }
                    })
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val currentVersion = "1.0.0"
                    val isOldVersion = try {
                        val minV = minVersion.split(".").map { it.toInt() }
                        val curV = currentVersion.split(".").map { it.toInt() }
                        var old = false
                        for (i in 0 until minOf(minV.size, curV.size)) {
                            if (minV[i] > curV[i]) { old = true; break }
                            if (minV[i] < curV[i]) { break }
                        }
                        old
                    } catch (e: Exception) {
                        false
                    }

                    if (isSettingsLoaded && upsetScreenEnabled) {
                        // Polished Maintenance Screen
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkCharcoal)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "System Maintenance",
                                    style = com.example.ui.theme.Typography.headlineMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = upsetMessage,
                                    style = com.example.ui.theme.Typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.7f)
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else if (isSettingsLoaded && isOldVersion) {
                        // Polished Update Screen
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkCharcoal)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NewReleases,
                                    contentDescription = null,
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "App Update Required",
                                    style = com.example.ui.theme.Typography.headlineMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "You are currently running an older version of Invexx. Please download the latest version ($latestVersion) to continue using our secure services.",
                                    style = com.example.ui.theme.Typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.7f)
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                InvexxButton(
                                    text = "Download Update",
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        MainAppContent()
                    }
                }
            }
        }
    }

    private fun startFirebaseNotificationListeners() {
        val prefs = PreferenceManager(this)
        val phone = prefs.phone
        val databaseUrl = "https://prime-khatab-default-rtdb.firebaseio.com"
        
        // Remove old listeners to avoid duplicates
        stopFirebaseNotificationListeners()

        val appLaunchTime = System.currentTimeMillis()

        // 1. Listen for personal user notifications
        if (phone.isNotBlank()) {
            val userNotifRef = FirebaseDatabase.getInstance(databaseUrl)
                .getReference("users")
                .child(phone)
                .child("notifications")
                
            val listener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                    // Only trigger for notifications added after app launch (approx. last 5 secs tolerance)
                    if (timestamp >= appLaunchTime - 5000) {
                        val title = snapshot.child("title").getValue(String::class.java) ?: "New Alert"
                        val message = snapshot.child("message").getValue(String::class.java) ?: ""
                        com.example.ui.utils.PushNotificationManager.showNotification(this@MainActivity, title, message)
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            }
            userNotifRef.addChildEventListener(listener)
            notificationsListener = listener
        }

        // 2. Listen for global admin announcements
        val announcementsRef = FirebaseDatabase.getInstance(databaseUrl).getReference("announcements")
        val annListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                if (timestamp >= appLaunchTime - 5000) {
                    val title = snapshot.child("title").getValue(String::class.java) ?: "Global Announcement"
                    val message = snapshot.child("message").getValue(String::class.java) ?: ""
                    com.example.ui.utils.PushNotificationManager.showNotification(this@MainActivity, title, message)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        announcementsRef.addChildEventListener(annListener)
        announcementsListener = annListener

        // 3. Listen for new plans
        val plansRef = FirebaseDatabase.getInstance(databaseUrl).getReference("plans")
        val initialPlanKeys = mutableSetOf<String>()
        var isPlansInitialLoadDone = false
        plansRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { initialPlanKeys.add(it.key ?: "") }
                isPlansInitialLoadDone = true
                
                val pListener = object : ChildEventListener {
                    override fun onChildAdded(child: DataSnapshot, previousChildName: String?) {
                        val key = child.key ?: return
                        if (isPlansInitialLoadDone && !initialPlanKeys.contains(key)) {
                            val planName = child.child("name").getValue(String::class.java) ?: "New Plan"
                            com.example.ui.utils.PushNotificationManager.showNotification(
                                this@MainActivity, 
                                "New Investment Plan! \uD83D\uDE80", 
                                "Check out our new plan: $planName. Invest now and earn high returns!"
                            )
                            initialPlanKeys.add(key)
                        }
                    }
                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onChildRemoved(snapshot: DataSnapshot) {}
                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {}
                }
                plansRef.addChildEventListener(pListener)
                plansListener = pListener
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 4. Listen for new blogs
        val blogsRef = FirebaseDatabase.getInstance(databaseUrl).getReference("blogs")
        val initialBlogKeys = mutableSetOf<String>()
        var isBlogsInitialLoadDone = false
        blogsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { initialBlogKeys.add(it.key ?: "") }
                isBlogsInitialLoadDone = true
                
                val bListener = object : ChildEventListener {
                    override fun onChildAdded(child: DataSnapshot, previousChildName: String?) {
                        val key = child.key ?: return
                        if (isBlogsInitialLoadDone && !initialBlogKeys.contains(key)) {
                            val blogTitle = child.child("title").getValue(String::class.java) ?: "New Article"
                            com.example.ui.utils.PushNotificationManager.showNotification(
                                this@MainActivity, 
                                "New Blog Post \uD83D\uDCDC", 
                                "Just published: $blogTitle. Read it now!"
                            )
                            initialBlogKeys.add(key)
                        }
                    }
                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onChildRemoved(snapshot: DataSnapshot) {}
                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {}
                }
                blogsRef.addChildEventListener(bListener)
                blogsListener = bListener
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 5. Listen for successful payment callbacks to automatically credit deposits
        val paymentCallbacksRef = FirebaseDatabase.getInstance(databaseUrl).getReference("payment_callbacks")
        val pCbListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handlePaymentCallbackSnapshot(snapshot)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handlePaymentCallbackSnapshot(snapshot)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        paymentCallbacksRef.addChildEventListener(pCbListener)
        paymentCallbacksListener = pCbListener
    }

    private fun stopFirebaseNotificationListeners() {
        val databaseUrl = "https://prime-khatab-default-rtdb.firebaseio.com"
        val prefs = PreferenceManager(this)
        val phone = prefs.phone
        
        try {
            notificationsListener?.let {
                if (phone.isNotBlank()) {
                    FirebaseDatabase.getInstance(databaseUrl)
                        .getReference("users")
                        .child(phone)
                        .child("notifications")
                        .removeEventListener(it)
                }
            }
        } catch (e: Exception) {}
        notificationsListener = null

        try {
            announcementsListener?.let {
                FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("announcements")
                    .removeEventListener(it)
            }
        } catch (e: Exception) {}
        announcementsListener = null

        try {
            plansListener?.let {
                FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("plans")
                    .removeEventListener(it)
            }
        } catch (e: Exception) {}
        plansListener = null

        try {
            blogsListener?.let {
                FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("blogs")
                    .removeEventListener(it)
            }
        } catch (e: Exception) {}
        blogsListener = null
        announcementsListener = null

        try {
            paymentCallbacksListener?.let {
                FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("payment_callbacks")
                    .removeEventListener(it)
            }
        } catch (e: Exception) {}
        paymentCallbacksListener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        val prefs = PreferenceManager(this)
        prefs.unregisterListener(prefChangeListener)
        stopFirebaseNotificationListeners()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Welcome Messages"
            val descriptionText = "Notifications sent to welcome you to INVEXX"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                sendWelcomeNotification()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            sendWelcomeNotification()
        }
    }

    private fun sendWelcomeNotification() {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.example.R.drawable.ic_notification)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, com.example.R.drawable.app_icon))
            .setContentTitle("Welcome to INVEXX! ✨")
            .setContentText("Your smart gateway to premium wealth growth and secure fixed funds. Start earning today!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(101, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted or SecurityException
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handlePaymentCallbackSnapshot(snapshot: DataSnapshot) {
        val prefs = PreferenceManager(this)
        val userId = prefs.userId.toString()
        if (userId.isBlank()) return

        val orderNo = snapshot.key ?: return
        
        val merchantOrder = snapshot.child("merchant_order_no").getValue(String::class.java)
            ?: snapshot.child("merchantOrder").getValue(String::class.java)
            ?: snapshot.child("orderNo").getValue(String::class.java)
            ?: orderNo

        if (merchantOrder.startsWith("${userId}_")) {
            val status = snapshot.child("status").getValue(String::class.java) ?: ""
            val amountVal = snapshot.child("amount").getValue(Double::class.java)
                ?: snapshot.child("amount").getValue(Float::class.java)?.toDouble()
                ?: snapshot.child("amount").getValue(String::class.java)?.toDoubleOrNull()
                ?: 0.0

            if (status.equals("success", ignoreCase = true)) {
                val apiService = ServiceLocator.getApiService(this@MainActivity)
                lifecycleScope.launch {
                    try {
                        val result = apiService.processPaymentCallback(merchantOrder, "success", amountVal.toFloat())
                        if (result.status == "success") {
                            com.example.ui.utils.PushNotificationManager.showNotification(
                                this@MainActivity,
                                "Recharge Successful! 🎉",
                                "Your deposit of ₹${amountVal} for order $merchantOrder was processed automatically!"
                            )
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }
}

sealed class BottomBarTab(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomBarTab("home", "Home", Icons.Default.Home)
    object Team : BottomBarTab("team", "Team", Icons.Default.Share)
    object Blog : BottomBarTab("blog", "Blog", Icons.Default.Menu)
    object Mine : BottomBarTab("mine", "Mine", Icons.Default.Person)
}

@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if the bottom navigation bar should be shown
    val bottomTabs = listOf(
        BottomBarTab.Home,
        BottomBarTab.Team,
        BottomBarTab.Blog,
        BottomBarTab.Mine
    )
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    // Unified ViewModels
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val teamViewModel: TeamViewModel = viewModel()
    val blogViewModel: BlogViewModel = viewModel()
    val mineViewModel: MineViewModel = viewModel()
    val depositViewModel: DepositViewModel = viewModel()
    val withdrawViewModel: WithdrawViewModel = viewModel()
    val listsViewModel: ListsViewModel = viewModel()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Soft premium neumorphic shadow top border
                            drawLine(
                                color = ShadowColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 2.dp.toPx()
                            )
                        },
                    color = PureWhite.copy(alpha = 0.85f)
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(64.dp)
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tr(tab.route),
                                        tint = if (selected) PrimaryGold else MediumGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tr(tab.route),
                                        style = Typography.labelSmall.copy(
                                            color = if (selected) DarkCharcoal else MediumGray,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = GoldenLight
                                )
                            )
                        }
                    }
                }
            }
        },
        containerColor = SoftBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 400 },
                    animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -400 },
                    animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -400 },
                    animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 400 },
                    animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            // Screen 1: Splash
            composable("splash") {
                SplashScreen(navController = navController)
            }

            // Screen 2: Login
            composable("login") {
                LoginScreen(navController = navController, viewModel = authViewModel)
            }

            // Screen 3: Register
            composable("register") {
                RegisterScreen(navController = navController, viewModel = authViewModel)
            }
            
            // Forgot Password
            composable("forgot_password") {
                com.example.ui.screens.ForgotPasswordScreen(navController = navController, viewModel = authViewModel)
            }

            // Screen 4: Home (Bottom Tab)
            composable(
                route = "home"
            ) {
                HomeScreen(navController = navController, viewModel = homeViewModel)
            }

            // Screen 5: Team (Bottom Tab)
            composable("team") {
                TeamScreen(navController = navController, viewModel = teamViewModel)
            }

            // Screen 6: Blog (Bottom Tab)
            composable("blog") {
                BlogScreen(navController = navController, viewModel = blogViewModel)
            }

            // Screen 7: Mine (Bottom Tab)
            composable("mine") {
                MineScreen(navController = navController, viewModel = mineViewModel)
            }

            // Screen 8: Deposit
            composable("deposit") {
                DepositScreen(navController = navController, viewModel = depositViewModel)
            }

            // Screen 9: Withdraw
            composable("withdraw") {
                WithdrawScreen(navController = navController, viewModel = withdrawViewModel)
            }

            // Screen 10: Order History (My Investments)
            composable("orderHistory") {
                OrderHistoryScreen(navController = navController, viewModel = listsViewModel)
            }

            // Screen 11: Fund History (Transaction Ledger)
            composable("fundHistory") {
                FundHistoryScreen(navController = navController, viewModel = listsViewModel)
            }

            // Screen 12: Gift Code Redeem
            composable("giftCode") {
                GiftCodeScreen(navController = navController, viewModel = listsViewModel)
            }

            // Screen 13: Daily Tasks
            composable("task") {
                TaskScreen(navController = navController, viewModel = listsViewModel)
            }

            // Screen 14: Notifications List
            composable("notifications") {
                NotificationScreen(navController = navController, viewModel = listsViewModel)
            }

            // Screen 15: Rank Benefits Info
            composable("rankBenefits") {
                RankBenefitsScreen(navController = navController)
            }

            // Screen 16: Blog detail (with parameters)
            composable(
                route = "blogDetail/{blogId}",
                arguments = listOf(navArgument("blogId") { type = NavType.IntType })
            ) { backStackEntry ->
                val blogId = backStackEntry.arguments?.getInt("blogId") ?: 1
                BlogDetailScreen(blogId = blogId, navController = navController)
            }

            // Screen 17: Customer Support
            composable("support") {
                SupportScreen(navController = navController)
            }

            // Screen 18: Settings Page
            composable("settings") {
                SettingsScreen(navController = navController)
            }

            // Screen 19: About Page
            composable("about") {
                AboutScreen(navController = navController)
            }

            // Screen 20: Payment Callbacks
            composable("paymentCallbacks") {
                com.example.ui.screens.PaymentCallbacksScreen(navController = navController, viewModel = listsViewModel)
            }
        }
    }
}

@Composable
fun AppLockOverlay(
    onUnlockSuccess: () -> Unit,
    onTriggerBiometrics: () -> Unit,
    accountPasswordFallback: String,
    context: Context
) {
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var showPasswordFallback by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative yellow blobs
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = (-60).dp)
                .size(200.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(PrimaryGold.copy(alpha = 0.4f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .size(240.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(SecondaryGold.copy(alpha = 0.4f), Color.Transparent)))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(PureWhite.copy(alpha = 0.95f))
                .border(1.dp, LightGrayBorder, RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            // Security Icon / Lock
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GoldenLight)
                    .border(2.dp, PrimaryGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Locked",
                    tint = PrimaryGold,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "INVEXX Secure Lock",
                style = Typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp),
                color = DarkCharcoal
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please authenticate using biometrics or your password to access your secure wallet.",
                style = Typography.bodyMedium,
                color = MediumGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main unlock button
            Button(
                onClick = onTriggerBiometrics,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = DarkCharcoal),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Unlock with Biometrics",
                        style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!showPasswordFallback) {
                TextButton(
                    onClick = { showPasswordFallback = true }
                ) {
                    Text(
                        text = "Use Password Fallback",
                        color = PrimaryGold,
                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Enter Account Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = LightGrayBorder,
                            focusedLabelColor = PrimaryGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passwordError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = passwordError,
                            color = MaterialTheme.colorScheme.error,
                            style = Typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (passwordInput == accountPasswordFallback || (accountPasswordFallback.isBlank() && passwordInput == "123456")) {
                                onUnlockSuccess()
                            } else {
                                passwordError = "Incorrect password! Please try again."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCharcoal, contentColor = PureWhite),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Verify & Unlock")
                    }
                }
            }
        }
    }
}
