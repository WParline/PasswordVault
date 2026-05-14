package com.passwordvault.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.passwordvault.app.ui.screens.detail.AccountDetailScreen
import com.passwordvault.app.ui.screens.settings.SettingsScreen
import com.passwordvault.app.ui.screens.unlock.UnlockScreen
import com.passwordvault.app.ui.screens.vault.VaultScreen

object Routes {
    const val UNLOCK = "unlock"
    const val VAULT = "vault"
    const val ACCOUNT_DETAIL = "account_detail/{accountId}?totpSecret={totpSecret}"
    const val QR_SCANNER = "qr_scanner"
    const val SETTINGS = "settings"

    fun accountDetail(accountId: Long, totpSecret: String = "") =
        "account_detail/$accountId?totpSecret=$totpSecret"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.UNLOCK) {
        composable(Routes.UNLOCK) {
            UnlockScreen(
                onUnlocked = {
                    navController.navigate(Routes.VAULT) {
                        popUpTo(Routes.UNLOCK) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.VAULT) {
            VaultScreen(
                onNavigateToAccount = { accountId ->
                    navController.navigate(Routes.accountDetail(accountId))
                },
                onOpenScanner = {
                    navController.navigate(Routes.QR_SCANNER)
                },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        composable(
            route = Routes.ACCOUNT_DETAIL,
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType },
                navArgument("totpSecret") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            val totpSecret = backStackEntry.arguments?.getString("totpSecret") ?: ""
            AccountDetailScreen(
                accountId = accountId,
                prefilledTotp = totpSecret,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.QR_SCANNER) {
            com.passwordvault.app.ui.screens.totp.QrScannerScreen(
                onScanResult = { rawValue ->
                    val data = com.passwordvault.app.domain.totp.TotpGenerator.parseTotpUri(rawValue)
                    val secret = data?.secret ?: rawValue.uppercase()
                    navController.navigate(Routes.accountDetail(-1L, secret)) {
                        popUpTo(Routes.VAULT)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
