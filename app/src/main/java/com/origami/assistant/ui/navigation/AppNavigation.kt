package com.origami.assistant.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.origami.assistant.ui.assistants.AssistantsScreen
import com.origami.assistant.ui.chat.ChatScreen
import com.origami.assistant.ui.modelsetup.ModelSetupScreen
import com.origami.assistant.ui.onboarding.OnboardingScreen
import com.origami.assistant.ui.settings.SettingsScreen
import com.origami.assistant.ui.skills.SkillsScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val startDest = remember {
        val prefs = EntryPointAccessors.fromApplication(
            context.applicationContext,
            PrefsEntryPoint::class.java
        ).prefs()
        val done = runBlocking { prefs.onboardingDone.first() }
        if (done) Screen.Chat.withId() else Screen.Onboarding.route
    }

    val bottomNavItems = listOf(
        BottomNavItem("Chat", Screen.Chat.withId(), Icons.Default.Chat),
        BottomNavItem("Assistants", Screen.Assistants.route, Icons.Default.SmartToy),
        BottomNavItem("Skills", Screen.Skills.route, Icons.Default.Extension),
        BottomNavItem("Settings", Screen.Settings.route, Icons.Default.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute !in listOf(Screen.Onboarding.route, Screen.ModelSetup.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute?.startsWith(item.route.substringBefore("?")) == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.ModelSetup.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ModelSetup.route) {
                ModelSetupScreen(
                    onComplete = {
                        navController.navigate(Screen.Chat.withId()) {
                            popUpTo(Screen.ModelSetup.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    navArgument("conversationId") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStack ->
                val conversationId = backStack.arguments?.getString("conversationId")
                ChatScreen(initialConversationId = conversationId)
            }

            composable(Screen.Assistants.route) {
                AssistantsScreen(
                    onNavigateToChat = { convId ->
                        navController.navigate(Screen.Chat.withId(convId))
                    }
                )
            }

            composable(Screen.Skills.route) {
                SkillsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToModelSetup = {
                        navController.navigate(Screen.ModelSetup.route)
                    }
                )
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)
