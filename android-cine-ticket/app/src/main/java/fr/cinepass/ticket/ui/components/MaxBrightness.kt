package fr.cinepass.ticket.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
 * n'est pas modifié, et la valeur précédente est restaurée à la sortie de l'écran.
 */
@Composable
fun MaxBrightnessEffect(enabled: Boolean = true) {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity, enabled) {
        val window = activity?.window
        val previousBrightness = window?.attributes?.screenBrightness

        if (window != null && enabled) {
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            if (window != null && enabled) {
                window.attributes = window.attributes.apply {
                    screenBrightness = previousBrightness
                        ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
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
