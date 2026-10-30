package app.undermat.ui.door

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.undermat.R
import app.undermat.data.Host
import app.undermat.data.Look
import app.undermat.ru
import app.undermat.ui.components.Btn
import app.undermat.ui.theme.C
import app.undermat.ui.theme.Serif
import app.undermat.ui.theme.T
import kotlin.math.abs
import kotlin.math.min

/** The landing outside the flat: the host's door, the doormat, and under it the key and a note. */
@Composable
fun DoorScreen(host: Host, look: Look, lifted0: Boolean = false, onEnter: () -> Unit, onBack: () -> Unit) {
    val ru = LocalContext.current.ru
    var lifted by remember { mutableStateOf(lifted0) }
    val lift by animateFloatAsState(if (lifted) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 120f), label = "mat")
    val shown by animateFloatAsState(if (lifted) 1f else 0f, tween(500, delayMillis = 250), label = "note")
    val tm = rememberTextMeasurer()
    val number = remember(host.id) { abs(host.id.hashCode() % 23) + 2 }
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF4B3D33), C.stair), radius = 1400f))) {
        Canvas(
            Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { lifted = true },
        ) {
            val s = min(size.width / 390f, size.height / 844f)
            val ox = (size.width - 390f * s) / 2
            withTransform({ translate(ox, 0f); scale(s, s, Offset.Zero) }) {
                // floor of the landing
                drawRect(Brush.verticalGradient(listOf(Color(0xFF5A4535), Color(0xFF3A2C22)), 520f, 900f), Offset(-400f, 520f), Size(1190f, 400f))
                // the door
                val dx = 90f
                drawRect(Color(0xFF5B3B27), Offset(dx - 10, 80f), Size(230f, 440f))
                drawRect(look.door, Offset(dx, 90f), Size(210f, 430f))
                val panel = lerp(look.door, Color.Black, 0.18f)
                drawRect(panel, Offset(dx + 22, 120f), Size(166f, 180f), style = Stroke(3f))
                drawRect(panel, Offset(dx + 22, 330f), Size(166f, 160f), style = Stroke(3f))
                drawCircle(C.gold, 7f, Offset(dx + 186, 316f))
                val num = tm.measure("$number", TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = (22f / density / fontScale).sp, color = C.gold))
                drawText(num, topLeft = Offset(dx + 105 - num.size.width / 2f, 96f))
                val plate = tm.measure(host.doorplate, TextStyle(fontFamily = Serif, fontSize = (10f / density / fontScale).sp, letterSpacing = 0.06.em, color = Color(0xFF3A2A1E)))
                val pw = plate.size.width + 16f
                drawRect(C.gold, Offset(dx + 105 - pw / 2, 196f), Size(pw, plate.size.height + 8f))
                drawText(plate, topLeft = Offset(dx + 105 - plate.size.width / 2f, 200f))
                // the key, under where the mat was
                val ky = 586f
                if (lift > 0.05f) {
                    val a = lift.coerceIn(0f, 1f)
                    withTransform({ rotate(-12f, Offset(195f, ky)) }) {
                        drawCircle(C.gold.copy(alpha = a), 11f, Offset(162f, ky), style = Stroke(5f))
                        drawLine(C.gold.copy(alpha = a), Offset(173f, ky), Offset(228f, ky), 5f)
                        drawLine(C.gold.copy(alpha = a), Offset(214f, ky), Offset(214f, ky + 9), 5f)
                        drawLine(C.gold.copy(alpha = a), Offset(224f, ky), Offset(224f, ky + 7), 5f)
                    }
                }
                // the mat, tipped up from its far edge
                val my = 540f - lift * 40f
                val squash = 1f - lift * 0.55f
                withTransform({ translate(0f, my); scale(1f, squash, Offset(195f, 0f)) }) {
                    drawRoundRect(Color(0x55000000), Offset(78f, 8f), Size(238f, 104f), CornerRadius(8f))
                    val p = Path().apply { moveTo(90f, 0f); lineTo(300f, 0f); lineTo(316f, 100f); lineTo(74f, 100f); close() }
                    drawPath(p, Color(0xFFA8683A))
                    var x = 80f
                    while (x < 312f) { drawLine(Color(0xFF9A5A2F), Offset(x + 6, 2f), Offset(x, 98f), 3f); x += 6f }
                    val inner = Path().apply { moveTo(98f, 8f); lineTo(292f, 8f); lineTo(305f, 92f); lineTo(85f, 92f); close() }
                    drawPath(inner, Color(0x886B3A1A), style = Stroke(2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7f, 5f))))
                    val t = tm.measure(host.mat, TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, letterSpacing = 0.25.em, fontSize = (17f / density / fontScale).sp, color = Color(0xFF5A3016)))
                    val sc = min(1f, 190f / t.size.width)
                    withTransform({ scale(sc, sc, Offset(195f, 50f)) }) { drawText(t, topLeft = Offset(195f - t.size.width / 2f, 50f - t.size.height / 2f)) }
                }
            }
        }
        Text("←", style = T.Title.copy(color = C.paper), modifier = Modifier.statusBarsPadding().padding(12.dp).clickable(onClick = onBack))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!lifted) {
                Text(stringResource(R.string.door_where, host.city.of(ru), host.address.of(ru)), style = T.Note.copy(color = Color(0xFFF3E2C2)), textAlign = TextAlign.Center)
                Text(stringResource(R.string.door_tap), style = T.Small.copy(color = Color(0xFFB09A80)))
            } else {
                Text("«" + host.away.of(ru) + "»", style = T.Note.copy(color = Color(0xFFF3E2C2)), textAlign = TextAlign.Center, modifier = Modifier.alpha(shown))
                Text(host.sign.of(ru), style = T.Note.copy(color = Color(0xFFD8C4A0)), modifier = Modifier.alpha(shown))
                Btn(stringResource(R.string.open_door), Modifier.alpha(shown).height(48.dp), onClick = onEnter)
            }
        }
    }
}
