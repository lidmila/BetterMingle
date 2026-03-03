package com.bettermingle.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.bettermingle.app.ui.screen.auth.RegisterScreen
import com.bettermingle.app.ui.screen.create.CreateEventScreen
import com.bettermingle.app.ui.screen.event.CarpoolScreen
import com.bettermingle.app.ui.screen.event.ChatScreen
import com.bettermingle.app.ui.screen.event.EventDashboardScreen
import com.bettermingle.app.ui.screen.event.EventSettingsScreen
import com.bettermingle.app.ui.screen.event.ExpensesScreen
import com.bettermingle.app.ui.screen.event.JoinEventScreen
import com.bettermingle.app.ui.screen.event.TasksScreen
import com.bettermingle.app.ui.screen.event.ParticipantsScreen
import com.bettermingle.app.ui.screen.event.RatingScreen
import com.bettermingle.app.ui.screen.event.RoomsScreen
import com.bettermingle.app.ui.screen.event.ScheduleScreen
import com.bettermingle.app.ui.screen.event.VotingScreen
import com.bettermingle.app.ui.screen.home.EventListScreen
import com.bettermingle.app.ui.screen.profile.HelpScreen
import com.bettermingle.app.ui.screen.profile.ProfileScreen
import com.bettermingle.app.ui.screen.profile.SettingsScreen
import com.bettermingle.app.ui.screen.profile.UpgradeScreen
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.viewmodel.AuthViewModel

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
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val UPGRADE = "upgrade"
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
    fun eventSettings(eventId: String) = "event_settings/$eventId"
    fun invitation(inviteCode: String) = "invitation/$inviteCode"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.EVENT_LIST, "Akce", Icons.Filled.Event, Icons.Outlined.Event),
    BottomNavItem(Routes.CREATE_EVENT, "Vytvořit", Icons.Filled.Add, Icons.Outlined.Add),
    BottomNavItem(Routes.PROFILE, "Profil", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun BetterMingleNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    val startDestination = if (authState.isLoggedIn) Routes.EVENT_LIST else Routes.LOGIN

    val showBottomBar = currentDestination?.route in listOf(
        Routes.EVENT_LIST,
        Routes.CREATE_EVENT,
        Routes.PROFILE
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
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
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
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
            // Auth
            composable(Routes.LOGIN) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                    onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                    onLoginSuccess = {
                        navController.navigate(Routes.EVENT_LIST) {
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
                        navController.navigate(Routes.EVENT_LIST) {
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
                CreateEventScreen(
                    onEventCreated = { eventId ->
                        navController.navigate(Routes.eventDashboard(eventId)) {
                            popUpTo(Routes.EVENT_LIST) { inclusive = false }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
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
                    }
                )
            }
            composable(Routes.UPGRADE) {
                UpgradeScreen(
                    onNavigateBack = { navController.popBackStack() }
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

            // Event detail
            composable(
                route = Routes.EVENT_DASHBOARD,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                EventDashboardScreen(
                    eventId = eventId,
                    onNavigateBack = { navController.popBackStack() },
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
                            "rating" -> Routes.rating(eventId)
                            "settings" -> Routes.eventSettings(eventId)
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
                VotingScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
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
                ExpensesScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
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
                    }
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
