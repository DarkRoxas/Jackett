package fr.cinepass.ticket

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.gms.pay.PayClient
import fr.cinepass.ticket.ui.CinePassApp
import fr.cinepass.ticket.ui.theme.CinePassTheme
import fr.cinepass.ticket.wallet.WalletRepository
import fr.cinepass.ticket.wallet.WalletResultBus
import fr.cinepass.ticket.wallet.WalletSaveResult

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = appContainer

        setContent {
            CinePassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CinePassApp(
                        settingsRepository = container.settingsRepository,
                        walletRepository = container.walletRepository,
                    )
                }
            }
        }
    }

    /**
     * `PayClient.savePassesJwt` renvoie son résultat par l'ancien mécanisme
     * `onActivityResult` ; on le relaie au ViewModel via [WalletResultBus].
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != WalletRepository.SAVE_PASS_REQUEST_CODE) return

        val result = when (resultCode) {
            RESULT_OK -> WalletSaveResult.Saved
            RESULT_CANCELED -> WalletSaveResult.Cancelled
            PayClient.SavePassesResult.SAVE_ERROR -> WalletSaveResult.Error(
                data?.getStringExtra(PayClient.EXTRA_API_ERROR_MESSAGE),
            )

            else -> WalletSaveResult.Error("code de résultat inattendu ($resultCode)")
        }
        WalletResultBus.publish(result)
    }
}
