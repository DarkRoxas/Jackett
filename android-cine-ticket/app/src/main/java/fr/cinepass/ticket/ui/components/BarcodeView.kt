package fr.cinepass.ticket.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import fr.cinepass.ticket.data.TicketBarcodeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Rend le code-barres du billet. Le fond est toujours blanc pur et les modules
 * noirs, quel que soit le thème : c'est le contraste attendu par les scanners.
 */
@Composable
fun BarcodeView(
    content: String,
    format: TicketBarcodeFormat,
    modifier: Modifier = Modifier,
) {
    val aspectRatio = if (format.isTwoDimensional) 1f else 2.6f

    val bitmap by produceState<Result<Bitmap>?>(initialValue = null, content, format) {
        value = withContext(Dispatchers.Default) {
            runCatching { encode(content, format) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        contentAlignment = Alignment.Center,
    ) {
        val result = bitmap
        when {
            result == null -> CircularProgressIndicator()
            result.isSuccess -> Image(
                bitmap = result.getOrThrow().asImageBitmap(),
                contentDescription = "Code-barres du billet",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
                // Sans filtrage : des bords nets restent lisibles à l'agrandissement.
                filterQuality = FilterQuality.None,
            )

            else -> Text(
                text = "Code illisible pour le format ${format.label} :\n" +
                    (result.exceptionOrNull()?.message ?: "contenu invalide"),
                color = Color.Black,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

private fun encode(content: String, format: TicketBarcodeFormat): Bitmap {
    require(content.isNotBlank()) { "contenu vide" }

    val (width, height) = if (format.isTwoDimensional) 600 to 600 else 900 to 300
    val hints = buildMap<EncodeHintType, Any> {
        put(EncodeHintType.MARGIN, if (format.isTwoDimensional) 1 else 8)
        put(EncodeHintType.CHARACTER_SET, "UTF-8")
    }

    val matrix: BitMatrix = MultiFormatWriter().encode(content, format.zxing, width, height, hints)
    val pixels = IntArray(matrix.width * matrix.height)
    for (y in 0 until matrix.height) {
        val offset = y * matrix.width
        for (x in 0 until matrix.width) {
            pixels[offset + x] = if (matrix.get(x, y)) BLACK else WHITE
        }
    }

    return Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    }
}

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
