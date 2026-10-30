package app.undermat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.undermat.R

/** Notepaper, ballpoint blue, a stairwell's brown and the red of a luggage label. */
@Immutable
object C {
    val paper = Color(0xFFFBF4E4)
    val paper2 = Color(0xFFF3E7CC)
    val line = Color(0xFFE7D9BB)
    val card = Color(0xFFFFF9EC)
    val cardEdge = Color(0xFFE3D2AD)
    val ink = Color(0xFF3A2A1E)
    val muted = Color(0xFF8A7360)
    val faint = Color(0xFFB8A58A)
    val pen = Color(0xFF2B3F6B)
    val red = Color(0xFFB3392B)
    val green = Color(0xFF5D8A63)
    val gold = Color(0xFFD8B25A)
    val cork = Color(0xFFB98A5A)
    val corkDark = Color(0xFF9A6E43)
    val stair = Color(0xFF2A211B)
    val wood = Color(0xFF7A4B2E)
}

/** PT Serif to read by, Caveat for everything the hosts wrote by hand (both SIL OFL, both with Cyrillic). */
val Serif = FontFamily(
    Font(R.font.pt_serif_regular, FontWeight.Normal),
    Font(R.font.pt_serif_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.pt_serif_bold, FontWeight.Bold),
)
val Hand = FontFamily(
    Font(R.font.caveat_medium, FontWeight.Normal),
    Font(R.font.caveat_bold, FontWeight.Bold),
)

object T {
    val Display = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, color = C.ink)
    val Eyebrow = TextStyle(fontFamily = Serif, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.12.em, color = C.muted)
    val Body = TextStyle(fontFamily = Serif, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val Sub = TextStyle(fontFamily = Serif, fontSize = 13.sp, lineHeight = 18.sp, color = C.muted)
    val Italic = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 14.sp, lineHeight = 19.sp, color = C.muted)
    val Small = TextStyle(fontFamily = Serif, fontSize = 12.sp, lineHeight = 16.sp, color = C.muted)
    val Note = TextStyle(fontFamily = Hand, fontSize = 23.sp, lineHeight = 28.sp, color = C.pen)
    val NoteBig = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 32.sp, color = C.pen)
    val Button = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp)
    val Big = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, fontSize = 28.sp, lineHeight = 32.sp, color = Color(0xFF2C5D8A))
}

@Composable
fun UndermatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.ink, onPrimary = C.paper, background = C.paper, surface = C.paper, onSurface = C.ink,
            onBackground = C.ink, surfaceContainer = C.paper, surfaceContainerHigh = C.paper, surfaceContainerHighest = C.paper2,
            outline = C.ink, error = C.red,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
