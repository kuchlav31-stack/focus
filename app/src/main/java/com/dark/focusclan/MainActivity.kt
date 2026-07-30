package com.dark.focusclan

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

        // 1. Edge-to-Edge enable kiya (Modern look)
        enableEdgeToEdge()

        // 2. Rotation globally OFF (Sirf Portrait mode)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            FocusclanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // YAHAN BADLAV HAI:
                    // 1. safeDrawingPadding() hata diya (Top space khatam)
                    // 2. navigationBarsPadding() lagaya (Bottom space safe rahega)
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        // --- Onboarding & Auth ---
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignupScreen(navController) }
        composable("profile_setup") { ProfileSetupScreen(navController) }
        composable("permission_setup") { PermissionScreen(navController) }

        // --- Dashboard Core ---
        composable("home") { HomeScreen(navController) }
        composable("discover") { DiscoverScreen(navController) }
        composable("challenge_feed") { ChallengeFeedScreen(navController) }
        composable("leaderboard") { LeaderboardScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("friend_requests") { FriendRequestScreen(navController) }

        // --- Interaction & Profiles ---
        composable(
            route = "other_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (userId.isNotEmpty()) {
                OtherUserProfileScreen(navController, userId)
            }
        }

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

        // --- Focus & Modes ---
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
        // MainActivity.kt ke NavHost mein ise paste karein
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
    }
}