package fr.cinepass.ticket.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Force la luminosité de l'écran au maximum tant que le composable est affiché,
 * et empêche la mise en veille : indispensable pour qu'un scanner de salle
 * lise le code-barres, y compris en plein jour.
 *
 * L'override est local à la fenêtre de l'app : le réglage système de l'appareil
 * n'est pas modifié, et la valeur précédente est restaurée à la sortie.
 */
@Composable
fun MaxBrightnessEffect(enabled: Boolean = true) {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity, enabled) {
        val window = activity?.window
        if (window != null && enabled) ScreenBrightness.acquire(window)

        onDispose {
            if (window != null && enabled) ScreenBrightness.release(window)
        }
    }
}

/**
 * Compteur de demandeurs de luminosité maximale.
 *
 * Sans ce comptage, passer de la fiche à la vue plein écran éteignait l'effet :
 * les deux écrans coexistent brièvement, et le `onDispose` de la fiche —
 * exécuté *après* l'activation par le plein écran — restaurait l'ancienne
 * valeur. La luminosité n'est donc rendue au système que lorsque plus aucun
 * écran ne la demande.
 */
private object ScreenBrightness {

    private var holders = 0
    private var previousBrightness: Float? = null

    fun acquire(window: Window) {
        if (holders == 0) {
            previousBrightness = window.attributes.screenBrightness
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        holders++
    }

    fun release(window: Window) {
        holders = (holders - 1).coerceAtLeast(0)
        if (holders == 0) {
            window.attributes = window.attributes.apply {
                screenBrightness = previousBrightness
                    ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            previousBrightness = null
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
