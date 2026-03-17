package com.bettermingle.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bettermingle.app.ui.screen.auth.ForgotPasswordScreen
import com.bettermingle.app.ui.screen.auth.LoginScreen
import com.bettermingle.app.ui.screen.auth.OnboardingScreen
import com.bettermingle.app.ui.screen.auth.ProfileSetupScreen
import com.bettermingle.app.ui.screen.auth.RegisterScreen
import com.bettermingle.app.ui.screen.create.CreateEventScreen
import com.bettermingle.app.ui.screen.event.CarpoolScreen
import com.bettermingle.app.ui.screen.event.ChatScreen
import com.bettermingle.app.ui.screen.event.EventDashboardScreen
import com.bettermingle.app.ui.screen.event.EventSettingsScreen
import com.bettermingle.app.ui.screen.event.ExpensesScreen
import com.bettermingle.app.ui.screen.event.ActivityFeedScreen
import com.bettermingle.app.ui.screen.event.EventSummaryScreen
import com.bettermingle.app.ui.screen.event.JoinEventScreen
import com.bettermingle.app.ui.screen.event.TasksScreen
import com.bettermingle.app.ui.screen.event.ParticipantsScreen
import com.bettermingle.app.ui.screen.event.RatingScreen
import com.bettermingle.app.ui.screen.event.PackingListScreen
import com.bettermingle.app.ui.screen.event.WishlistScreen
import com.bettermingle.app.ui.screen.event.RoomsScreen
import com.bettermingle.app.ui.screen.event.ScheduleScreen
import com.bettermingle.app.ui.screen.event.VotingScreen
import com.bettermingle.app.ui.screen.home.EventListScreen
import com.bettermingle.app.ui.screen.home.NotificationsScreen
import com.bettermingle.app.ui.screen.home.YearInReviewScreen
import com.bettermingle.app.ui.screen.profile.EditProfileScreen
import com.bettermingle.app.ui.screen.profile.HelpScreen
import com.bettermingle.app.ui.screen.profile.ProfileScreen
import com.bettermingle.app.ui.screen.profile.SettingsScreen
import com.bettermingle.app.ui.screen.profile.UpgradeScreen
import com.bettermingle.app.data.ads.AdManager
import com.bettermingle.app.data.billing.BillingManager
import com.bettermingle.app.data.preferences.SettingsManager
import androidx.compose.ui.res.stringResource
import com.bettermingle.app.R
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.PrimaryBlue

import com.bettermingle.app.viewmodel.AuthViewModel
import com.bettermingle.app.viewmodel.NotificationsViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val EVENT_LIST = "event_list"
    const val CREATE_EVENT = "create_event"
    const val PROFILE = "profile"
    const val EVENT_DASHBOARD = "event_dashboard/{eventId}"
    const val VOTING = "voting/{eventId}"
    const val PARTICIPANTS = "participants/{eventId}"
    const val EXPENSES = "expenses/{eventId}"
    const val CARPOOL = "carpool/{eventId}"
    const val ROOMS = "rooms/{eventId}"
    const val SCHEDULE = "schedule/{eventId}"
    const val CHAT = "chat/{eventId}"
    const val RATING = "rating/{eventId}"
    const val EVENT_SETTINGS = "event_settings/{eventId}"
    const val TASKS = "tasks/{eventId}"
    const val PACKING_LIST = "packing_list/{eventId}"
    const val WISHLIST = "wishlist/{eventId}"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val UPGRADE = "upgrade"
    const val EDIT_PROFILE = "edit_profile"
    const val NOTIFICATIONS = "notifications"
    const val YEAR_IN_REVIEW = "year_in_review"
    const val ONBOARDING = "onboarding"
    const val PROFILE_SETUP = "profile_setup"
    const val ACTIVITY_FEED = "activity_feed/{eventId}"
    const val EVENT_SUMMARY = "event_summary/{eventId}"
    const val INVITATION = "invitation/{inviteCode}"

    fun eventDashboard(eventId: String) = "event_dashboard/$eventId"
    fun voting(eventId: String) = "voting/$eventId"
    fun participants(eventId: String) = "participants/$eventId"
    fun expenses(eventId: String) = "expenses/$eventId"
    fun carpool(eventId: String) = "carpool/$eventId"
    fun rooms(eventId: String) = "rooms/$eventId"
    fun schedule(eventId: String) = "schedule/$eventId"
    fun chat(eventId: String) = "chat/$eventId"
    fun rating(eventId: String) = "rating/$eventId"
    fun tasks(eventId: String) = "tasks/$eventId"
    fun packingList(eventId: String) = "packing_list/$eventId"
    fun wishlist(eventId: String) = "wishlist/$eventId"
    fun eventSettings(eventId: String) = "event_settings/$eventId"
    fun activityFeed(eventId: String) = "activity_feed/$eventId"
    fun eventSummary(eventId: String) = "event_summary/$eventId"
    fun invitation(inviteCode: String) = "invitation/$inviteCode"
}

data class BottomNavItem(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.EVENT_LIST, R.string.nav_events, Icons.Filled.Event, Icons.Outlined.Event),
    BottomNavItem(Routes.CREATE_EVENT, R.string.nav_create, Icons.Filled.Add, Icons.Outlined.Add),
    BottomNavItem(Routes.NOTIFICATIONS, R.string.nav_notifications, Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem(Routes.PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun BetterMingleNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val billingManager = remember { BillingManager(context, settingsManager) }
    val billingState by billingManager.uiState.collectAsState()
    val settings by settingsManager.settingsFlow.collectAsState(
        initial = com.bettermingle.app.data.preferences.AppSettings()
    )

    // Start billing connection and reset ad session state
    LaunchedEffect(Unit) {
        billingManager.startConnection()
        AdManager.resetSession()
    }

    // Preload interstitial ad for FREE tier
    LaunchedEffect(settings.premiumTier) {
        if (AdManager.hasAds(settings.premiumTier)) {
            AdManager.loadInterstitial(context)
        }
    }

    val startDestination = when {
        !authState.isLoggedIn && !settings.onboardingCompleted -> Routes.ONBOARDING
        !authState.isLoggedIn -> Routes.LOGIN
        authState.isLoggedIn -> Routes.PROFILE_SETUP
        else -> Routes.EVENT_LIST
    }

    val showBottomBar = currentDestination?.route in listOf(
        Routes.EVENT_LIST,
        Routes.CREATE_EVENT,
        Routes.NOTIFICATIONS,
        Routes.PROFILE
    )

    // Unread notifications count from local DataStore (reactive, instant)
    val notificationsViewModel: NotificationsViewModel = viewModel()
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()

    // Clear unread count when navigating to notifications
    LaunchedEffect(currentDestination?.route) {
        if (currentDestination?.route == Routes.NOTIFICATIONS) {
            notificationsViewModel.markAllRead()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                if (item.route == Routes.NOTIFICATIONS && unreadCount > 0) {
                                    Box {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelResId)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-2).dp)
                                                .size(16.dp)
                                                .background(AccentOrange, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (unreadCount > 99) "99" else "$unreadCount",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = com.bettermingle.app.ui.theme.TextOnColor
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelResId)
                                    )
                                }
                            },
                            label = { Text(stringResource(item.labelResId)) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = AccentPink.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { BetterMingleMotion.screenEnter },
            exitTransition = { BetterMingleMotion.screenExit },
            popEnterTransition = { BetterMingleMotion.screenPopEnter },
            popExitTransition = { BetterMingleMotion.screenPopExit }
        ) {
            // Onboarding
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        scope.launch { settingsManager.setOnboardingCompleted() }
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            // Auth
            composable(Routes.LOGIN) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                    onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                    onLoginSuccess = {
                        navController.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }
            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    authViewModel = authViewModel
                )
            }

            // Profile setup after first login
            composable(Routes.PROFILE_SETUP) {
                ProfileSetupScreen(
                    onComplete = {
                        navController.navigate(Routes.EVENT_LIST) {
                            popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                        }
                    }
                )
            }

            // Main tabs
            composable(Routes.EVENT_LIST) {
                EventListScreen(
                    onEventClick = { eventId ->
                        navController.navigate(Routes.eventDashboard(eventId))
                    },
                    onCreateEvent = {
                        navController.navigate(Routes.CREATE_EVENT)
                    }
                )
            }
            composable(Routes.CREATE_EVENT) {
                val createContext = LocalContext.current
                CreateEventScreen(
                    onEventCreated = { eventId ->
                        val navigateToDashboard = {
                            navController.navigate(Routes.eventDashboard(eventId)) {
                                popUpTo(Routes.EVENT_LIST) { inclusive = false }
                            }
                        }
                        val currentSettings = settings
                        if (currentSettings != null && AdManager.hasAds(currentSettings.premiumTier)) {
                            val activity = createContext as? android.app.Activity
                            if (activity != null) {
                                AdManager.showInterstitial(activity) { navigateToDashboard() }
                            } else {
                                navigateToDashboard()
                            }
                        } else {
                            navigateToDashboard()
                        }
                    },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUpgrade = { navController.navigate(Routes.UPGRADE) }
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onEventClick = { eventId ->
                        navController.navigate(Routes.eventDashboard(eventId))
                    },
                    viewModel = notificationsViewModel
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        scope.launch {
                            authViewModel.logout()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onUpgrade = {
                        navController.navigate(Routes.UPGRADE)
                    },
                    onSettings = {
                        navController.navigate(Routes.SETTINGS)
                    },
                    onHelp = {
                        navController.navigate(Routes.HELP)
                    },
                    onEditProfile = {
                        navController.navigate(Routes.EDIT_PROFILE)
                    },
                    onYearInReview = {
                        navController.navigate(Routes.YEAR_IN_REVIEW)
                    }
                )
            }
            composable(Routes.UPGRADE) {
                UpgradeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSubscribe = { productId, isYearly ->
                        val activity = context as? android.app.Activity ?: return@UpgradeScreen
                        val product = billingState.products.find { it.productId == productId }
                            ?: return@UpgradeScreen
                        val productDetails = product.productDetails ?: return@UpgradeScreen

                        if (productId == BillingManager.PRODUCT_LIFETIME) {
                            billingManager.launchInappPurchaseFlow(activity, productDetails)
                        } else {
                            val offerToken = if (isYearly) product.yearlyOfferToken else product.monthlyOfferToken
                            if (offerToken != null) {
                                billingManager.launchPurchaseFlow(activity, productDetails, offerToken)
                            }
                        }
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAccountDeleted = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HELP) {
                HelpScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.YEAR_IN_REVIEW) {
                YearInReviewScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Event detail
            composable(
                route = Routes.EVENT_DASHBOARD,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                val dashboardContext = LocalContext.current
                EventDashboardScreen(
                    eventId = eventId,
                    onNavigateBack = {
                        val currentSettings = settings
                        if (currentSettings != null && AdManager.hasAds(currentSettings.premiumTier)) {
                            val activity = dashboardContext as? android.app.Activity
                            if (activity != null) {
                                AdManager.showReturnInterstitial(activity) {
                                    navController.popBackStack()
                                }
                            } else {
                                navController.popBackStack()
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onModuleClick = { module ->
                        val route = when (module) {
                            "voting" -> Routes.voting(eventId)
                            "participants" -> Routes.participants(eventId)
                            "expenses" -> Routes.expenses(eventId)
                            "carpool" -> Routes.carpool(eventId)
                            "rooms" -> Routes.rooms(eventId)
                            "schedule" -> Routes.schedule(eventId)
                            "chat" -> Routes.chat(eventId)
                            "tasks" -> Routes.tasks(eventId)
                            "packing" -> Routes.packingList(eventId)
                            "wishlist" -> Routes.wishlist(eventId)
                            "rating" -> Routes.rating(eventId)
                            "settings" -> Routes.eventSettings(eventId)
                            "activity" -> Routes.activityFeed(eventId)
                            "summary" -> Routes.eventSummary(eventId)
                            else -> return@EventDashboardScreen
                        }
                        navController.navigate(route)
                    }
                )
            }

            // Module screens
            composable(
                route = Routes.VOTING,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                VotingScreen(
                    eventId = eventId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUpgrade = { navController.navigate(Routes.UPGRADE) }
                )
            }
            composable(
                route = Routes.PARTICIPANTS,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                ParticipantsScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.EXPENSES,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                ExpensesScreen(
                    eventId = eventId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUpgrade = { navController.navigate(Routes.UPGRADE) }
                )
            }
            composable(
                route = Routes.CARPOOL,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                CarpoolScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.ROOMS,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                RoomsScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.SCHEDULE,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                ScheduleScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.TASKS,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                TasksScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.PACKING_LIST,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                PackingListScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.WISHLIST,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                WishlistScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.CHAT,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                ChatScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.RATING,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                RatingScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.EVENT_SETTINGS,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                EventSettingsScreen(
                    eventId = eventId,
                    onNavigateBack = { navController.popBackStack() },
                    onEventDeleted = {
                        navController.navigate(Routes.EVENT_LIST) {
                            popUpTo(Routes.EVENT_LIST) { inclusive = true }
                        }
                    },
                    onRepeatEvent = { name, desc, theme, location, modules ->
                        // Navigate to create event (user creates fresh copy)
                        navController.navigate(Routes.CREATE_EVENT) {
                            popUpTo(Routes.EVENT_LIST) { inclusive = false }
                        }
                    },
                    onNavigateToUpgrade = { navController.navigate(Routes.UPGRADE) }
                )
            }

            // Activity Feed
            composable(
                route = Routes.ACTIVITY_FEED,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                ActivityFeedScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
            }

            // Event Summary
            composable(
                route = Routes.EVENT_SUMMARY,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val eventId = it.arguments?.getString("eventId") ?: ""
                EventSummaryScreen(
                    eventId = eventId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUpgrade = { navController.navigate(Routes.UPGRADE) }
                )
            }

            // Invitation
            composable(
                route = Routes.INVITATION,
                arguments = listOf(navArgument("inviteCode") { type = NavType.StringType })
            ) {
                val inviteCode = it.arguments?.getString("inviteCode") ?: ""
                JoinEventScreen(
                    inviteCode = inviteCode,
                    onNavigateBack = { navController.popBackStack() },
                    onJoined = { eventId ->
                        navController.navigate(Routes.eventDashboard(eventId)) {
                            popUpTo(Routes.EVENT_LIST) { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}
