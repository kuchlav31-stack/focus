package com.dark.focusclan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dark.focusclan.ui.auth.*
import com.dark.focusclan.ui.dashboard.*
import com.dark.focusclan.ui.setup.PermissionScreen
import com.dark.focusclan.ui.splash.SplashScreen
import com.dark.focusclan.ui.theme.FocusclanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusclanTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        // Auth & Setup
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignupScreen(navController) }
        composable("profile_setup") { ProfileSetupScreen(navController) }
        composable("permission_setup") { PermissionScreen(navController) }

        // Dashboard Core
        composable("home") { HomeScreen(navController) }
        composable("discover") { DiscoverScreen(navController) }
        composable("challenge_feed") { ChallengeFeedScreen(navController) }
        composable("leaderboard") { LeaderboardScreen(navController) }
        composable("profile") { ProfileScreen(navController) }

        // Mode & Lobby
        composable(
            route = "mode_selection/{time}",
            arguments = listOf(navArgument("time") { type = NavType.IntType })
        ) { backStackEntry ->
            val time = backStackEntry.arguments?.getInt("time") ?: 25
            ModeSelectionScreen(navController, time)
        }

        composable(
            route = "lobby/{challengeId}",
            arguments = listOf(navArgument("challengeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId") ?: ""
            LobbyScreen(navController, challengeId)
        }

        // Focus Mode (Nuclear Lock)
        // ... baki imports
        composable(
            route = "focus_mode/{challengeId}/{time}/{mode}",
            arguments = listOf(
                navArgument("challengeId") { type = NavType.StringType },
                navArgument("time") { type = NavType.IntType },
                navArgument("mode") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId") ?: "solo"
            val time = backStackEntry.arguments?.getInt("time") ?: 25
            val mode = backStackEntry.arguments?.getInt("mode") ?: 1
            FocusModeScreen(navController, challengeId, time, mode)
        }
        composable("friend_requests") { FriendRequestScreen(navController) }
        // MainActivity.kt ke NavHost mein check/update karein:
        composable(
            route = "other_profile/{userId}", // Route with argument
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (userId.isNotEmpty()) {
                OtherUserProfileScreen(navController, userId)
            }
        }

//        composable("other_profile/{userId}") { backStackEntry ->
//            val userId = backStackEntry.arguments?.getString("userId") ?: ""
//            OtherUserProfileScreen(navController, userId)
//        }


        composable(
            route = "chat/{friendUid}/{friendName}",
            arguments = listOf(
                navArgument("friendUid") { type = NavType.StringType },
                navArgument("friendName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("friendUid") ?: ""
            val name = backStackEntry.arguments?.getString("friendName") ?: ""
            ChatScreen(navController, uid, name)
        }
    }
}