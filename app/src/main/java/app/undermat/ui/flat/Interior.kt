package app.undermat.ui.flat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import app.undermat.data.Floor
import app.undermat.data.Host
import app.undermat.data.Lamp
import app.undermat.data.Look
import app.undermat.data.Sign
import app.undermat.data.Tiles
import app.undermat.data.Win
import app.undermat.domain.Sky
import app.undermat.ui.theme.Hand
import app.undermat.ui.theme.Serif
import java.time.ZonedDateTime
import java.time.format.TextStyle as DateStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/** The corners of the flat that hold a note, in room units (the room is 390 wide; its floor line is at 470). */
enum class Spot(val id: String, val l: Float, val t: Float, val w: Float, val h: Float, val px: Float, val py: Float) {
    WORD("word", 14f, 40f, 72f, 56f, 80f, 46f),
    WINDOW("window", 116f, 46f, 158f, 160f, 246f, 58f),
    CALENDAR("calendar", 282f, 34f, 58f, 76f, 334f, 40f),
    RULES("rules", 8f, 186f, 62f, 34f, 56f, 190f),
    FRIDGE("fridge", 6f, 220f, 62f, 244f, 54f, 238f),
    RECORD("record", 250f, 362f, 56f, 40f, 290f, 372f),
    DRAWER("drawer", 168f, 402f, 126f, 32f, 278f, 408f),
    TV("tv", 170f, 312f, 84f, 90f, 244f, 336f),
    KITCHEN("kitchen", 66f, 260f, 98f, 204f, 148f, 268f),
    BOOKS("books", 304f, 146f, 84f, 318f, 372f, 160f);

    fun hit(x: Float, y: Float) = x in l..(l + w) && y in t..(t + h)

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id }
    }
}

/** Where the room sits on the canvas: scaled to fit, its floor on the bottom edge, extra wall above. */
private class Frame(w: Float, h: Float) {
    val s = min(w / 390f, h / 700f)
    val ox = (w - 390f * s) / 2
    val oy = h - 560f * s
    fun room(p: Offset) = Offset((p.x - ox) / s, (p.y - oy) / s)
}

private const val GX0 = 148f
private const val GX1 = 242f
private const val GY0 = 56f
private const val GY1 = 190f
private val BASKET = Offset(195f, 120f)

@Composable
fun Interior(
    host: Host,
    look: Look,
    local: ZonedDateTime,
    sky: Sky?,
    unread: Set<Spot>,
    basket: Boolean,
    still: Boolean,
    modifier: Modifier = Modifier,
    onSpot: (Spot) -> Unit,
    onBasket: () -> Unit,
) {
    val tm = rememberTextMeasurer()
    var time by remember { mutableFloatStateOf(0.8f) }
    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        val t0 = withFrameMillis { it }
        while (true) withFrameMillis { time = (it - t0) / 1000f + 0.8f }
    }
    val drop by animateFloatAsState(if (basket) 1f else 0f, tween(if (still) 0 else 2600), label = "basket")
    val light = remember(local.hour, local.minute, sky, look) { Light.at(local, look, sky) }
    val spines = remember(host.id) { spines(host.id, look) }
    val month = remember(local.monthValue, host.lang) {
        local.month.getDisplayName(DateStyle.SHORT_STANDALONE, Locale.forLanguageTag(host.lang)).trimEnd('.').let { m -> if (m.first().code < 0x0530) m.uppercase() else m }.take(4)
    }
    val currentBasket = basket
    Canvas(
        modifier.clipToBounds().pointerInput(host.id, currentBasket) {
            detectTapGestures { p ->
                val r = Frame(size.width.toFloat(), size.height.toFloat()).room(p)
                if (currentBasket && abs(r.x - BASKET.x) < 26 && abs(r.y - BASKET.y) < 30) onBasket()
                else Spot.entries.firstOrNull { it.hit(r.x, r.y) }?.let(onSpot)
            }
        },
    ) {
        drawRect(look.wall)
        val f = Frame(size.width, size.height)
        withTransform({ translate(f.ox, f.oy); scale(f.s, f.s, Offset.Zero) }) {
            Room(this, tm, host, look, light, sky, time, month, local.dayOfMonth, spines).draw(unread, drop)
        }
    }
}

private class Spine(val x: Float, val w: Float, val h: Float, val c: Color, val row: Int)

private fun spines(id: String, look: Look): List<Spine> {
    val cols = look.stickers + listOf(Color(0xFFD9CBB0), Color(0xFF5B6C8A), Color(0xFFC06D47))
    var seed = id.hashCode().toLong() and 0xFFFF
    fun rnd(): Float { seed = (seed * 9301 + 49297) % 233280; return seed / 233280f }
    val out = mutableListOf<Spine>()
    for (r in 0 until 5) {
        var x = 312f
        while (true) {
            val w = 5f + (rnd() * 6).toInt()
            val h = 30f + (rnd() * 20).toInt()
            if (x + w > 378) break
            out += Spine(x, w, h, cols[(rnd() * cols.size).toInt().coerceAtMost(cols.size - 1)], r)
            x += w + 1
            if (rnd() < 0.08f) x += 8
        }
    }
    return out
}

/** One frame of the flat, in room units. */
private class Room(
    val d: DrawScope,
    val tm: TextMeasurer,
    val host: Host,
    val k: Look,
    val light: Light,
    val sky: Sky?,
    val time: Float,
    val month: String,
    val dayOfMonth: Int,
    val spines: List<Spine>,
) {
    private fun c(hex: Long) = Color(0xFF000000 or hex)
    private fun c(hex: Int) = c(hex.toLong())
    private fun box(x: Float, y: Float, w: Float, h: Float, color: Color) = d.drawRect(color, Offset(x, y), Size(w, h))
    private fun rbox(x: Float, y: Float, w: Float, h: Float, r: Float, color: Color) = d.drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r))
    private fun line(x0: Float, y0: Float, x1: Float, y1: Float, color: Color, w: Float = 1f) = d.drawLine(color, Offset(x0, y0), Offset(x1, y1), w)
    private fun dot(x: Float, y: Float, r: Float, color: Color) = d.drawCircle(color, r, Offset(x, y))
    private fun poly(color: Color, vararg p: Float) {
        val path = Path().apply {
            moveTo(p[0], p[1])
            var i = 2
            while (i < p.size) { lineTo(p[i], p[i + 1]); i += 2 }
            close()
        }
        d.drawPath(path, color)
    }
    private fun darker(col: Color, t: Float = 0.25f) = lerp(col, Color.Black, t)
    private fun lighter(col: Color, t: Float = 0.25f) = lerp(col, Color.White, t)

    private fun text(s: String, x: Float, y: Float, size: Float, color: Color, family: FontFamily = Serif, bold: Boolean = false, maxW: Float = 0f) {
        val sp = (size / (d.density * d.fontScale)).sp
        var style = TextStyle(fontFamily = family, fontSize = sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = color)
        var m = tm.measure(s, style)
        if (maxW > 0 && m.size.width > maxW) {
            style = style.copy(fontSize = sp * (maxW / m.size.width))
            m = tm.measure(s, style)
        }
        d.drawText(m, topLeft = Offset(x - m.size.width / 2f, y - m.size.height / 2f))
    }

    fun draw(unread: Set<Spot>, drop: Float) {
        wall()
        floor()
        word()
        lamp()
        window(drop)
        calendar()
        wallSign()
        fridge()
        kitchen()
        cabinet()
        tv()
        record()
        books()
        night()
        pins(unread)
    }

    private fun wall() {
        box(-400f, -400f, 1190f, 870f, k.wall)
        val low = k.wallLow
        if (low != null) {
            box(-400f, 330f, 1190f, 132f, low)
            box(-400f, 328f, 1190f, 3f, darker(low, 0.18f))
        }
        if (k.sign == Sign.PAPEL_PICADO) papelPicado()
    }

    private fun floor() {
        val a = k.floorA
        val b = k.floorB
        box(-400f, 470f, 1190f, 200f, a)
        when (k.floor) {
            Floor.TERRACOTTA -> {
                var y = 470f
                var row = 0
                while (y < 600) {
                    var x = -400f + (row % 2) * 15
                    while (x < 790) {
                        if (((x + 400) / 30).toInt() % 2 == 0) box(x, y, 15f, 15f, lighter(a, 0.08f))
                        d.drawRect(b, Offset(x, y), Size(30f, 15f), style = Stroke(1f))
                        x += 30
                    }
                    y += 15; row++
                }
            }
            Floor.PARQUET -> {
                var y = 470f
                var row = 0
                while (y < 600) {
                    var x = -400f + (row % 2) * 20
                    while (x < 790) {
                        box(x, y, 40f, 9f, if (((x + 400) / 40).toInt() % 2 == row % 2) a else b)
                        line(x, y, x, y + 9, darker(b, 0.2f), 0.6f)
                        x += 40
                    }
                    y += 9; row++
                }
            }
            Floor.TATAMI -> {
                var x = -400f
                var i = 0
                while (x < 790) {
                    val w = if (i % 2 == 0) 120f else 60f
                    box(x, 470f, w, 120f, if (i % 2 == 0) a else lighter(a, 0.06f))
                    var lx = x + 3
                    while (lx < x + w) { line(lx, 474f, lx, 586f, b.copy(alpha = 0.35f), 0.6f); lx += 3 }
                    box(x, 470f, w, 4f, c(0x2E3B55))
                    box(x, 470f, 3f, 120f, c(0x2E3B55))
                    x += w; i++
                }
            }
            Floor.PLANKS -> {
                var y = 470f
                var row = 0
                while (y < 600) {
                    box(-400f, y, 1190f, 10f, if (row % 2 == 0) a else lerp(a, b, 0.4f))
                    line(-400f, y, 790f, y, darker(b, 0.15f), 0.7f)
                    var x = -400f + (row * 53 % 140)
                    while (x < 790) { line(x, y, x, y + 10, darker(b, 0.15f), 0.7f); x += 140 }
                    y += 10; row++
                }
            }
            Floor.CEMENT -> {
                var y = 470f
                while (y < 600) {
                    var x = -400f
                    while (x < 790) {
                        box(x, y, 30f, 30f, b)
                        poly(a, x + 15, y + 2, x + 28, y + 15, x + 15, y + 28, x + 2, y + 15)
                        dot(x + 15, y + 15, 5f, b)
                        dot(x + 15, y + 15, 2.5f, a)
                        dot(x, y, 4f, a); dot(x + 30, y, 4f, a); dot(x, y + 30, 4f, a); dot(x + 30, y + 30, 4f, a)
                        x += 30
                    }
                    y += 30
                }
            }
            Floor.CHECKER -> {
                var y = 470f
                var row = 0
                while (y < 600) {
                    var x = -400f
                    var col = 0
                    while (x < 790) {
                        box(x, y, 22f, 22f, if ((row + col) % 2 == 0) a else b)
                        x += 22; col++
                    }
                    y += 22; row++
                }
            }
        }
        box(-400f, 462f, 1190f, 8f, darker(k.wood, 0.05f))
        d.drawOval(k.rug, Offset(116f, 486f), Size(240f, 52f))
        d.drawOval(k.rugEdge, Offset(132f, 493f), Size(208f, 38f), style = Stroke(2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f))))
        if (k.sign == Sign.FAN_ALPANA) alpana()
    }

    private fun word() {
        box(18f, 44f, 64f, 48f, darker(k.wood, 0.1f))
        box(23f, 49f, 54f, 38f, c(0xFBF4E4))
        text(host.word.substringBefore(" · "), 50f, 64f, 12f, c(0x2B3F6B), Hand, maxW = 50f)
        val p = Path().apply { moveTo(32f, 76f); quadraticTo(50f, 82f, 68f, 76f) }
        d.drawPath(p, c(0xB3392B), style = Stroke(1f))
    }

    private fun lamp() {
        val x = 108f
        when (k.lamp) {
            Lamp.FAN -> {
                line(195f, -400f, 195f, 8f, c(0x3A3A3A), 1.5f)
                rbox(188f, 6f, 14f, 8f, 3f, k.lampColor)
                val a = time * 240f
                d.rotate(a, Offset(195f, 12f)) {
                    for (i in 0 until 3) d.rotate(i * 120f, Offset(195f, 12f)) { d.drawOval(c(0x6B4A33), Offset(195f, 9f), Size(56f, 7f)) }
                }
                dot(195f, 12f, 4f, k.lampColor)
                line(x, -400f, x, 40f, c(0x3A2A1E))
                dot(x, 44f, 5f, if (light.lamps) c(0xFFE9A8) else c(0xEDE6D6))
            }
            Lamp.PAPER -> {
                line(x, -400f, x, 30f, c(0x3A2A1E))
                d.drawOval(if (light.lamps) c(0xFFF1CF) else k.lampColor, Offset(x - 13, 30f), Size(26f, 30f))
                for (i in 0..4) line(x - 12 + (if (i == 0 || i == 4) 2f else 0f), 33f + i * 6, x + 12 - (if (i == 0 || i == 4) 2f else 0f), 33f + i * 6, c(0xC9B98F), 0.6f)
                box(x - 6, 28f, 12f, 3f, c(0x3A2A1E)); box(x - 6, 59f, 12f, 3f, c(0x3A2A1E))
            }
            Lamp.GLASS -> {
                line(x, -400f, x, 34f, c(0x3A2A1E))
                dot(x, 46f, 12f, if (light.lamps) c(0xFFE9A8) else k.lampColor.copy(alpha = 0.85f))
                d.drawCircle(darker(k.lampColor, 0.3f), 12f, Offset(x, 46f), style = Stroke(1f))
                box(x - 4, 32f, 8f, 4f, c(0x3A2A1E))
            }
            Lamp.BRASS -> {
                line(x, -400f, x, 38f, c(0x3A2A1E))
                poly(c(0xC9A04A), x - 15, 54f, x - 8, 38f, x + 8, 38f, x + 15, 54f)
                line(x - 15, 54f, x + 15, 54f, c(0x8A6A2A), 1.5f)
                if (light.lamps) dot(x, 55f, 3.5f, c(0xFFE9A8))
            }
            Lamp.PENDANT -> {
                line(x, -400f, x, 40f, c(0x3A2A1E))
                poly(k.lampColor, x - 14, 54f, x - 8, 40f, x + 8, 40f, x + 14, 54f)
                if (light.lamps) dot(x, 55f, 3.5f, c(0xFFE9A8))
            }
        }
    }

    // ---------- window ----------

    private fun glassPath(): Path = if (k.window == Win.ARCH) Path().apply {
        moveTo(GX0, GY1); lineTo(GX0, GY0 + 47)
        arcTo(androidx.compose.ui.geometry.Rect(GX0, GY0, GX1, GY0 + 94), 180f, 180f, false)
        lineTo(GX1, GY1); close()
    } else Path().apply { addRect(androidx.compose.ui.geometry.Rect(GX0, GY0, GX1, GY1)) }

    private fun window(drop: Float) {
        val fr = k.frame
        when (k.window) {
            Win.SHUTTERS, Win.LOUVRES -> {
                val w = if (k.window == Win.LOUVRES) 26f else 22f
                for (sx in listOf(140f - w, 250f)) {
                    box(sx, 48f, w, 150f, k.shutter)
                    var y = 60f
                    while (y < 192) { line(sx + 3, y, sx + w - 3, y, darker(k.shutter, 0.22f), if (k.window == Win.LOUVRES) 2f else 1.2f); y += if (k.window == Win.LOUVRES) 7f else 12f }
                }
            }
            Win.SASH -> {
                for (left in listOf(true, false)) {
                    val x0 = if (left) 112f else 250f
                    val p = Path().apply {
                        if (left) { moveTo(x0, 40f); lineTo(x0 + 34, 40f); quadraticTo(x0 + 14, 110f, x0 + 22, 200f); lineTo(x0, 200f) }
                        else { moveTo(x0 + 28, 40f); lineTo(x0 - 6, 40f); quadraticTo(x0 + 14, 110f, x0 + 6, 200f); lineTo(x0 + 28, 200f) }
                        close()
                    }
                    d.drawPath(p, k.shutter)
                    for (i in 1..3) line(x0 + i * 7f, 44f, x0 + i * 7f, 196f, darker(k.shutter, 0.15f), 0.8f)
                }
                box(106f, 36f, 178f, 4f, c(0x6B4A33))
            }
            Win.ARCH -> {
                box(122f, 70f, 18f, 128f, k.shutter)
                box(250f, 70f, 18f, 128f, k.shutter)
                d.drawRect(darker(k.shutter, 0.2f), Offset(125f, 74f), Size(12f, 120f), style = Stroke(1f))
                d.drawRect(darker(k.shutter, 0.2f), Offset(253f, 74f), Size(12f, 120f), style = Stroke(1f))
            }
            Win.SHOJI -> Unit
        }
        // the frame
        if (k.window == Win.ARCH) {
            val outer = Path().apply {
                moveTo(140f, 198f); lineTo(140f, 103f)
                arcTo(androidx.compose.ui.geometry.Rect(140f, 48f, 250f, 158f), 180f, 180f, false)
                lineTo(250f, 198f); close()
            }
            d.drawPath(outer, fr)
        } else box(140f, 48f, 110f, 150f, fr)
        val glass = glassPath()
        d.clipPath(glass) {
            view()
            if (drop > 0.01f) basket(drop)
            weather()
        }
        // mullions and screens
        when (k.window) {
            Win.SHOJI -> {
                box(GX0, GY0, 47f, GY1 - GY0, k.shutter)
                var y = GY0
                while (y <= GY1) { line(GX0, y, GX0 + 47, y, k.frame, 1.6f); y += 16.75f }
                var x = GX0
                while (x <= GX0 + 47) { line(x, GY0, x, GY1, k.frame, 1.6f); x += 15.6f }
                box(GX0 + 46, GY0, 3f, GY1 - GY0, k.frame)
            }
            Win.LOUVRES -> {
                var x = GX0 + 10
                while (x < GX1) { line(x, GY0, x, GY1, c(0x2B2B2B), 1.6f); x += 12 }
                line(GX0, 123f, GX1, 123f, fr, 4f)
            }
            else -> {
                line(195f, GY0, 195f, GY1, fr, 4f)
                line(GX0, 123f, GX1, 123f, fr, 4f)
            }
        }
        box(134f, 196f, 122f, 8f, darker(fr, 0.12f))
        if (k.sign == Sign.CANDLES) candles()
        else {
            poly(c(0xB9633E), 222f, 196f, 225f, 182f, 239f, 182f, 242f, 196f)
            dot(227f, 178f, 5f, c(0x4F8A3E)); dot(234f, 174f, 6f, c(0x5E9A48)); dot(240f, 179f, 4.5f, c(0x4F8A3E))
        }
        if (k.sign == Sign.FURIN) furin()
        if (k.sign == Sign.FITAS) fitas()
    }

    private fun sunAndStars() {
        val x = GX0 + 6 + light.arc * (GX1 - GX0 - 12)
        val y = 150f - sin(light.arc * PI).toFloat() * 78f
        val clear = sky == null || !sky.grey
        if (light.day) {
            if (clear) dot(x, y, 8f, c(0xFFE08A))
        } else {
            if (clear) {
                for ((sx, sy) in listOf(156f to 66f, 170f to 80f, 188f to 62f, 205f to 76f, 222f to 64f, 236f to 86f, 162f to 98f, 214f to 96f)) {
                    dot(sx, sy, 0.9f, Color.White.copy(alpha = 0.75f))
                }
            }
            if (clear || sky == Sky.CLOUDY) dot(x, y, 6f, c(0xF2EFE0))
        }
    }

    private fun lit(x: Float, y: Float) { if (light.lamps) box(x, y, 2.6f, 3.2f, c(0xFFD36B)) }

    /** The street outside, city by city. */
    private fun view() {
        d.drawRect(Brush.verticalGradient(listOf(light.top, light.bottom), GY0, GY1), Offset(GX0, GY0), Size(GX1 - GX0, GY1 - GY0))
        sunAndStars()
        val n = light.dark
        fun shade(col: Color) = lerp(col, c(0x1A2040), (n * 1.15f).coerceAtMost(0.62f))
        when (host.id) {
            "naples" -> {
                poly(shade(c(0x6C6F88)), 148f, 158f, 176f, 132f, 186f, 128f, 193f, 133f, 201f, 124f, 242f, 158f)
                box(148f, 156f, 94f, 10f, shade(c(0x2F6F8F)))
                box(148f, 164f, 16f, 26f, shade(c(0xD99A6C))); box(186f, 160f, 20f, 30f, shade(c(0xD99A6C))); box(226f, 166f, 16f, 24f, shade(c(0xD99A6C)))
                box(163f, 168f, 24f, 22f, shade(c(0xC7795A))); box(205f, 163f, 22f, 27f, shade(c(0xC7795A)))
                lit(152f, 170f); lit(170f, 174f); lit(192f, 168f); lit(211f, 172f); lit(231f, 174f)
                val p = Path().apply { moveTo(148f, 70f); quadraticTo(195f, 82f, 242f, 72f) }
                d.drawPath(p, c(0x555555), style = Stroke(0.8f))
                box(160f, 73f, 10f, 12f, c(0xFBF4E4)); box(174f, 75f, 8f, 9f, c(0xB3392B)); box(200f, 76f, 12f, 10f, c(0x2C5D8A)); box(220f, 74f, 7f, 11f, c(0xE2B13C))
            }
            "kyoto" -> {
                poly(shade(c(0x6E8A6A)), 148f, 150f, 170f, 128f, 196f, 134f, 222f, 118f, 242f, 132f, 242f, 160f, 148f, 160f)
                poly(shade(c(0x4E6B4C)), 148f, 160f, 180f, 144f, 210f, 152f, 242f, 142f, 242f, 170f, 148f, 170f)
                // a five-storey pagoda
                val px = 214f
                box(px - 1, 100f, 2f, 12f, shade(c(0x3A2A1E)))
                for (i in 0 until 5) {
                    val y = 112f + i * 9
                    val w = 12f + i * 2.4f
                    poly(shade(c(0x3A3A40)), px - w, y + 3, px, y - 2, px + w, y + 3)
                    box(px - w * 0.55f, y + 3, w * 1.1f, 6f, shade(c(0x8A3A2A)))
                }
                for (i in 0 until 4) {
                    val x = 148f + i * 26
                    poly(shade(c(0x4A4A52)), x - 4, 178f, x + 13, 168f, x + 30, 178f)
                    box(x, 178f, 26f, 12f, shade(c(0x7A5A3E)))
                    lit(x + 8, 182f)
                }
            }
            "oaxaca" -> {
                poly(shade(c(0x7B8DA6)), 148f, 140f, 168f, 120f, 190f, 130f, 214f, 112f, 242f, 128f, 242f, 160f, 148f, 160f)
                poly(shade(c(0x5C7A5A)), 148f, 156f, 176f, 140f, 204f, 150f, 242f, 140f, 242f, 165f, 148f, 165f)
                // Santo Domingo: two bell towers and a dome
                val tx = 196f
                box(tx - 22, 140f, 44f, 30f, shade(c(0xC9A26A)))
                for (bx in listOf(tx - 22, tx + 12)) {
                    box(bx, 124f, 10f, 20f, shade(c(0xC9A26A)))
                    d.drawArc(shade(c(0xB98E56)), 180f, 180f, true, Offset(bx, 119f), Size(10f, 10f))
                }
                d.drawArc(shade(c(0xB98E56)), 180f, 180f, true, Offset(tx - 9, 130f), Size(18f, 18f))
                for ((i, col) in listOf(0xE65A7A, 0x3F8FA0, 0xE7B33F, 0x2F7A5A, 0xD6455B).withIndex()) {
                    box(148f + i * 19, 172f, 19f, 18f, shade(c(col)))
                    lit(154f + i * 19, 178f)
                }
            }
            "bergen" -> {
                poly(shade(c(0x3E5B48)), 148f, 96f, 176f, 86f, 210f, 98f, 242f, 112f, 242f, 160f, 148f, 160f)
                poly(shade(c(0x587A5E)), 148f, 130f, 190f, 120f, 242f, 140f, 242f, 160f, 148f, 160f)
                box(148f, 172f, 94f, 18f, shade(c(0x355C77)))
                for ((i, col) in listOf(0xB23A2E, 0xF1E9D2, 0xE0A93A, 0xB23A2E, 0xF1E9D2, 0x7A3A2E).withIndex()) {
                    val x = 148f + i * 16
                    box(x, 156f, 15f, 16f, shade(c(col)))
                    poly(shade(darker(c(col), 0.3f)), x - 1, 156f, x + 7.5f, 146f, x + 16, 156f)
                    lit(x + 6, 161f)
                }
                for (i in 0 until 5) line(150f + i * 20, 180f + (i % 2) * 4, 160f + i * 20, 180f + (i % 2) * 4, Color.White.copy(alpha = 0.35f), 0.8f)
            }
            "seoul" -> {
                poly(shade(c(0x6A7F8C)), 148f, 130f, 166f, 110f, 186f, 122f, 206f, 104f, 228f, 118f, 242f, 112f, 242f, 160f, 148f, 160f)
                // Namsan and its tower
                d.drawArc(shade(c(0x4E6B54)), 180f, 180f, true, Offset(186f, 132f), Size(50f, 30f))
                box(210f, 104f, 2f, 34f, shade(c(0xE9E9E9)))
                rbox(206f, 108f, 10f, 6f, 2f, shade(c(0xE9E9E9)))
                for ((i, h) in listOf(46f, 34f, 52f, 40f, 30f).withIndex()) {
                    val x = 150f + i * 18
                    box(x, 190f - h, 15f, h, shade(c(0xD8D4CC)))
                    var y = 194f - h
                    while (y < 186f) { line(x + 2, y, x + 13, y, shade(c(0x9AA3AD)), 0.7f); if (light.lamps && (y.toInt() + i) % 3 == 0) box(x + 4, y - 2.5f, 3f, 2f, c(0xFFD36B)); y += 5f }
                }
                rbox(222f, 166f, 14f, 10f, 2f, shade(c(0x4A6FA5)))
            }
            "lyon" -> {
                poly(shade(c(0x6E8C5E)), 148f, 140f, 172f, 116f, 214f, 114f, 242f, 132f, 242f, 160f, 148f, 160f)
                // Fourvière: a white basilica on the hill
                box(182f, 104f, 26f, 14f, shade(c(0xEFEBE2)))
                for (bx in listOf(181f, 190f, 199f, 206f)) box(bx, 98f, 4f, 20f, shade(c(0xEFEBE2)))
                box(148f, 158f, 94f, 8f, shade(c(0x4E7488)))
                d.drawArc(shade(c(0xC9B99A)), 180f, 180f, false, Offset(168f, 156f), Size(22f, 16f), style = Stroke(3f))
                d.drawArc(shade(c(0xC9B99A)), 180f, 180f, false, Offset(196f, 156f), Size(22f, 16f), style = Stroke(3f))
                for ((i, col) in listOf(0xE9C9A0, 0xD99A6C, 0xF0DCC0, 0xC7795A).withIndex()) {
                    val x = 148f + i * 24
                    box(x, 168f, 24f, 22f, shade(c(col)))
                    poly(shade(c(0x8A4A3A)), x - 1, 168f, x + 4, 163f, x + 20, 163f, x + 25, 168f)
                    box(x + 16, 158f, 3f, 6f, shade(c(0x8A4A3A)))
                    lit(x + 5, 174f); lit(x + 15, 180f)
                }
            }
            "tbilisi" -> {
                poly(shade(c(0x9A8A6A)), 148f, 120f, 176f, 102f, 206f, 112f, 242f, 98f, 242f, 160f, 148f, 160f)
                // Narikala on the ridge
                for (i in 0 until 5) { box(176f + i * 9, 100f + (i % 2) * 3, 6f, 10f, shade(c(0x8A7A5A))) }
                line(176f, 108f, 220f, 108f, shade(c(0x8A7A5A)), 3f)
                // a church with a conical dome
                box(206f, 138f, 18f, 22f, shade(c(0xD9CFB8)))
                box(211f, 126f, 8f, 12f, shade(c(0xD9CFB8)))
                poly(shade(c(0x7A8A8A)), 210f, 126f, 215f, 116f, 220f, 126f)
                box(148f, 160f, 94f, 6f, shade(c(0x6A8A8A)))
                for ((i, col) in listOf(0xE0C9A0, 0xC99A7A, 0xEAD9B8, 0xB98A6A).withIndex()) {
                    val x = 148f + i * 24
                    box(x, 166f, 24f, 24f, shade(c(col)))
                    box(x + 2, 172f, 20f, 3f, shade(c(0x6E4A30)))
                    for (j in 0..4) line(x + 3 + j * 4, 172f, x + 3 + j * 4, 180f, shade(c(0x6E4A30)), 0.8f)
                    lit(x + 8, 182f)
                }
            }
            "kolkata" -> {
                d.drawRect(c(0xE8D9B8).copy(alpha = 0.3f * (1 - n)), Offset(GX0, GY0), Size(94f, 134f))
                box(148f, 150f, 94f, 10f, shade(c(0x7A8A80)))
                // Howrah Bridge on the river
                val bc = shade(c(0x6A6F78))
                line(150f, 146f, 240f, 146f, bc, 2f)
                for (tx in listOf(170f, 220f)) { box(tx - 2, 120f, 4f, 28f, bc); line(tx, 120f, tx - 18, 146f, bc, 1f); line(tx, 120f, tx + 18, 146f, bc, 1f) }
                line(170f, 122f, 220f, 122f, bc, 1.2f)
                for ((i, col) in listOf(0xE7C6A0, 0x9FC2B0, 0xE5A6A0, 0xEAD9A0, 0xB7C9D9).withIndex()) {
                    val x = 148f + i * 19
                    val h = 30f + (i % 3) * 6
                    box(x, 190f - h, 19f, h, shade(c(col)))
                    box(x + 3, 194f - h, 13f, 2f, shade(c(0x2F6B4A)))
                    lit(x + 7, 198f - h + 6)
                }
            }
            "salvador" -> {
                box(148f, 128f, 94f, 30f, shade(c(0x2E7FA8)))
                for (i in 0 until 4) line(152f + i * 22, 136f + (i % 2) * 8, 166f + i * 22, 136f + (i % 2) * 8, Color.White.copy(alpha = 0.4f), 0.8f)
                // the Lacerda lift
                box(214f, 120f, 10f, 46f, shade(c(0xE9E4D6)))
                box(212f, 118f, 14f, 6f, shade(c(0xE9E4D6)))
                // a church with twin towers
                box(158f, 140f, 26f, 22f, shade(c(0xF4F1E8)))
                for (bx in listOf(156f, 180f)) { box(bx, 130f, 8f, 16f, shade(c(0xF4F1E8))); poly(shade(c(0x2E86AB)), bx, 130f, bx + 4, 124f, bx + 8, 130f) }
                for ((i, col) in listOf(0xF2A541, 0x2A9D5B, 0xE63946, 0x2E86AB, 0xF2C14E).withIndex()) {
                    val x = 148f + i * 19
                    box(x, 166f + (i % 2) * 4, 19f, 24f, shade(c(col)))
                    box(x + 4, 172f + (i % 2) * 4, 4f, 7f, shade(c(0x3A2A1E)))
                    lit(x + 12, 174f)
                }
                // a palm
                line(236f, 190f, 232f, 150f, shade(c(0x6B4A33)), 2f)
                for (a in listOf(-60f, -20f, 20f, 60f, 100f)) d.rotate(a, Offset(232f, 150f)) { d.drawOval(shade(c(0x3E8A4A)), Offset(232f, 148f), Size(18f, 4f)) }
            }
            "lisbon" -> {
                box(148f, 126f, 94f, 24f, shade(c(0x5E8FA8)))
                // the bridge over the Tagus
                val rc = shade(c(0xB3412E))
                for (tx in listOf(172f, 222f)) box(tx, 104f, 3f, 26f, rc)
                line(148f, 126f, 242f, 126f, rc, 1.5f)
                val cable = Path().apply { moveTo(148f, 118f); quadraticTo(160f, 104f, 173f, 105f); quadraticTo(197f, 124f, 223f, 105f); quadraticTo(234f, 104f, 242f, 116f) }
                d.drawPath(cable, rc, style = Stroke(1f))
                // a white dome
                d.drawArc(shade(c(0xF2EEE4)), 180f, 180f, true, Offset(150f, 138f), Size(18f, 18f))
                box(150f, 146f, 18f, 8f, shade(c(0xF2EEE4)))
                for (row in 0 until 3) for (i in 0 until 5) {
                    val x = 148f + i * 19 + (row % 2) * 8
                    val y = 154f + row * 12
                    box(x, y + 4, 18f, 12f, shade(c(if ((i + row) % 3 == 0) 0xF0E2C8 else if ((i + row) % 3 == 1) 0xF2C14E else 0xE9D6C8)))
                    poly(shade(c(0xB9553A)), x - 1, y + 5, x + 9, y, x + 19, y + 5)
                    lit(x + 6, y + 8)
                }
                rbox(208f, 180f, 18f, 9f, 2f, shade(c(0xF2C14E)))
                box(211f, 182f, 4f, 3f, shade(c(0xEDE6D6))); box(218f, 182f, 4f, 3f, shade(c(0xEDE6D6)))
            }
            "galway" -> {
                poly(shade(c(0x8A9AA0)), 148f, 136f, 190f, 128f, 242f, 134f, 242f, 150f, 148f, 150f)
                box(148f, 150f, 94f, 22f, shade(c(0x4E6E80)))
                // a Galway hooker under brown sails
                poly(shade(c(0x2B2B2B)), 160f, 160f, 182f, 160f, 178f, 165f, 163f, 165f)
                poly(shade(c(0x7A3E22)), 170f, 159f, 170f, 136f, 183f, 158f)
                poly(shade(c(0x7A3E22)), 168f, 159f, 168f, 142f, 160f, 158f)
                for ((i, col) in listOf(0xB23A48, 0xF2C14E, 0x2F4B6E, 0xEDE9DD, 0x3E6B48).withIndex()) {
                    val x = 148f + i * 19
                    box(x, 172f, 19f, 18f, shade(c(col)))
                    box(x + 13, 166f, 3f, 6f, shade(c(0x5B4130)))
                    lit(x + 5, 177f)
                }
            }
            "istanbul" -> {
                box(148f, 140f, 94f, 26f, shade(c(0x3E7C9A)))
                // the far shore: a mosque and its minarets
                val sc = shade(c(0x7A8494))
                box(148f, 128f, 94f, 12f, shade(c(0x8E98A6)))
                d.drawArc(sc, 180f, 180f, true, Offset(178f, 112f), Size(30f, 26f))
                box(178f, 124f, 30f, 6f, sc)
                d.drawArc(sc, 180f, 180f, true, Offset(172f, 122f), Size(12f, 10f))
                d.drawArc(sc, 180f, 180f, true, Offset(202f, 122f), Size(12f, 10f))
                for (mx in listOf(170f, 216f)) { box(mx, 100f, 2.5f, 30f, sc); poly(sc, mx - 0.5f, 100f, mx + 1.25f, 94f, mx + 3f, 100f) }
                // a ferry
                rbox(150f, 150f, 30f, 7f, 2f, shade(c(0xF4F4F0)))
                box(156f, 146f, 16f, 4f, shade(c(0xF4F4F0)))
                box(162f, 142f, 3f, 4f, shade(c(0x2B2B2B)))
                for ((i, col) in listOf(0xC0392B, 0xEFE2CC, 0x2E7D6B, 0xD4A017).withIndex()) {
                    val x = 148f + i * 24
                    box(x, 166f, 24f, 24f, shade(c(col)))
                    box(x + 2, 170f, 20f, 6f, shade(darker(c(col), 0.2f)))
                    lit(x + 9, 180f)
                }
                val gull = Path().apply { moveTo(222f, 80f); quadraticTo(226f, 76f, 230f, 80f); quadraticTo(234f, 76f, 238f, 80f) }
                d.drawPath(gull, shade(c(0xF4F4F0)), style = Stroke(1f))
            }
        }
    }

    private fun weather() {
        val s = sky ?: return
        val night = light.dark > 0.3f
        if (s == Sky.CLOUDY || s.grey) {
            val cc = if (night) c(0x5A6070) else Color.White
            val drift = (time * 3f) % 140f
            for ((i, cy) in listOf(78f, 96f, 70f).withIndex()) {
                val cx = GX0 - 30 + ((i * 47 + drift) % 150f)
                d.drawOval(cc.copy(alpha = if (s.grey) 0.9f else 0.8f), Offset(cx, cy), Size(34f, 11f))
                d.drawOval(cc.copy(alpha = if (s.grey) 0.9f else 0.8f), Offset(cx + 8, cy - 6), Size(18f, 12f))
            }
        }
        if (s == Sky.OVERCAST || s.rain) d.drawRect(c(0x8A93A0).copy(alpha = if (night) 0.25f else 0.3f), Offset(GX0, GY0), Size(94f, 134f))
        if (s == Sky.FOG) d.drawRect(Color.White.copy(alpha = if (night) 0.3f else 0.55f), Offset(GX0, GY0), Size(94f, 134f))
        if (s.rain) {
            for (i in 0 until 26) {
                val x = GX0 + (i * 37 % 94)
                val y = GY0 + ((i * 53 + time * 170f) % 134f)
                line(x, y, x - 3, y + 8, c(0xC9D6E2).copy(alpha = 0.7f), 0.8f)
            }
        }
        if (s.snow) {
            for (i in 0 until 30) {
                val x = GX0 + ((i * 31 + sin(time + i) * 4).toFloat() % 94 + 94) % 94
                val y = GY0 + ((i * 47 + time * 22f) % 134f)
                dot(x, y, 1.3f, Color.White)
            }
        }
        if (s == Sky.STORM && (time % 7f) < 0.12f) d.drawRect(Color.White.copy(alpha = 0.6f), Offset(GX0, GY0), Size(94f, 134f))
    }

    private fun basket(drop: Float) {
        val y = BASKET.y - (1 - drop) * 90f
        val sway = sin(time * 2.1f) * 2.5f
        line(195f, 0f, 195f + sway, y - 8, c(0x6B4A33), 1f)
        val x = 195f + sway
        poly(c(0xC69A5A), x - 14, y - 8, x + 14, y - 8, x + 10, y + 8, x - 10, y + 8)
        line(x - 12, y - 3, x + 12, y - 3, c(0x7A5530), 0.8f)
        line(x - 11, y + 2, x + 11, y + 2, c(0x7A5530), 0.8f)
        val handle = Path().apply { moveTo(x - 11, y - 8); quadraticTo(x, y - 20, x + 11, y - 8) }
        d.drawPath(handle, c(0x7A5530), style = Stroke(1.5f))
    }

    private fun calendar() {
        box(286f, 44f, 50f, 62f, c(0xFBF4E4))
        d.drawRect(c(0xCBB88E), Offset(286f, 44f), Size(50f, 62f), style = Stroke(1f))
        box(286f, 44f, 50f, 16f, k.stickers[0])
        text(month, 311f, 52f, 8.5f, Color.White, maxW = 44f)
        text(dayOfMonth.toString(), 311f, 84f, 25f, c(0x3A2A1E), bold = true)
        dot(311f, 40f, 2f, c(0x3A2A1E))
    }

    // ---------- the signs of each flat ----------

    private fun papelPicado() {
        val p = Path().apply { moveTo(-20f, 16f); quadraticTo(195f, 46f, 410f, 16f) }
        d.drawPath(p, c(0x3A2A1E), style = Stroke(0.8f))
        var i = 0
        var x = 0f
        while (x < 390) {
            val t = (x + 20) / 430f
            val y = 16f + 4 * 30f * t * (1 - t)
            val col = k.stickers[i % k.stickers.size]
            d.rotate(sin(time * 1.6f + i) * 4f, Offset(x + 9, y)) {
                poly(col, x, y, x + 18, y, x + 18, y + 18, x + 13, y + 22, x + 9, y + 18, x + 5, y + 22, x, y + 18)
                dot(x + 9, y + 8, 2.4f, k.wall); dot(x + 4, y + 13, 1.2f, k.wall); dot(x + 14, y + 13, 1.2f, k.wall)
            }
            x += 24; i++
        }
    }

    private fun alpana() {
        val cx = 50f
        val cy = 512f
        val w = Color.White.copy(alpha = 0.85f)
        for (a in 0 until 8) d.rotate(a * 45f, Offset(cx, cy)) { d.drawOval(w, Offset(cx - 3, cy - 18), Size(6f, 13f)) }
        d.drawCircle(w, 5f, Offset(cx, cy), style = Stroke(1.2f))
        d.drawCircle(w, 22f, Offset(cx, cy), style = Stroke(0.8f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 3f))))
    }

    private fun candles() {
        for ((i, x) in listOf(214f, 226f, 238f).withIndex()) {
            val h = 14f - i * 3
            box(x - 3, 196f - h, 6f, h, c(0xF4F0E2))
            val fl = 1f + sin(time * 9f + i * 2) * 0.15f
            d.drawOval(c(0xFFC24A), Offset(x - 2f * fl, 196f - h - 7), Size(4f * fl, 7f))
            if (light.lamps) dot(x, 196f - h - 4, 7f, c(0xFFD98A).copy(alpha = 0.25f))
        }
    }

    private fun furin() {
        val x = 230f
        line(x, GY0, x, 68f, c(0x3A2A1E), 0.6f)
        d.drawArc(c(0xA9D3E8).copy(alpha = 0.9f), 180f, 180f, true, Offset(x - 6, 66f), Size(12f, 12f))
        val sw = sin(time * 2.4f) * 10f
        d.rotate(sw, Offset(x, 72f)) {
            line(x, 72f, x, 82f, c(0x3A2A1E), 0.5f)
            box(x - 2.5f, 82f, 5f, 14f, c(0xF6E3A0))
        }
    }

    private fun fitas() {
        for ((i, col) in k.stickers.take(5).withIndex()) {
            val x = 252f + i * 3.5f
            val sw = sin(time * 2f + i) * 3f
            val p = Path().apply { moveTo(x, 60f); quadraticTo(x + sw, 80f, x + sw * 1.4f, 104f + i * 3) }
            d.drawPath(p, col, style = Stroke(2f))
        }
    }

    private fun wallSign() {
        when (k.sign) {
            Sign.KANTSI -> {
                val p = Path().apply { moveTo(279f, 226f); quadraticTo(304f, 236f, 292f, 278f); lineTo(287f, 276f); quadraticTo(296f, 240f, 279f, 233f); close() }
                d.drawPath(p, c(0x5A3E2A))
                box(277f, 224f, 5f, 10f, c(0xC9C9C9)); box(286f, 274f, 7f, 4f, c(0xC9C9C9))
                line(280f, 222f, 290f, 220f, c(0x3A2A1E), 0.6f)
            }
            Sign.SARDINE -> d.rotate(-18f, Offset(289f, 245f)) {
                d.drawOval(c(0x1C4E8C), Offset(276f, 238f), Size(26f, 12f))
                poly(c(0xC8553D), 300f, 244f, 308f, 238f, 308f, 250f)
                dot(281f, 243f, 1.4f, Color.White)
                for (i in 0..2) line(285f + i * 5, 240f, 285f + i * 5, 248f, c(0xF2C14E), 1.2f)
            }
            Sign.BRIGID -> {
                val st = c(0xD9B86A)
                box(287f, 222f, 6f, 46f, st); box(267f, 242f, 46f, 6f, st)
                box(286f, 241f, 8f, 8f, darker(st, 0.2f))
                box(285f, 222f, 2f, 8f, st); box(293f, 260f, 2f, 8f, st); box(267f, 248f, 8f, 2f, st); box(305f, 240f, 8f, 2f, st)
            }
            Sign.NAZAR -> {
                line(290f, 214f, 290f, 230f, c(0x3A2A1E), 0.6f)
                dot(290f, 242f, 12f, c(0x1F4E9A)); dot(290f, 242f, 8f, Color.White); dot(290f, 242f, 5f, c(0x6FB1E0)); dot(290f, 242f, 2.4f, c(0x111111))
            }
            Sign.STICKY_NOTES -> for ((i, col) in listOf(0xF2E36B, 0xF59AB0, 0x9AD6E8).withIndex()) {
                d.rotate(i * 7f - 6f, Offset(282f + i * 7f, 236f + i * 16f)) { box(274f + i * 7f, 228f + i * 16f, 16f, 15f, c(col)) }
            }
            else -> Unit
        }
    }

    // ---------- the furniture ----------

    private fun fridge() {
        rbox(8f, 222f, 58f, 240f, 8f, k.fridge)
        d.drawRoundRect(darker(k.fridge, 0.15f), Offset(8f, 222f), Size(58f, 240f), CornerRadius(8f), style = Stroke(1f))
        line(8f, 296f, 66f, 296f, darker(k.fridge, 0.15f))
        rbox(56f, 240f, 4f, 40f, 2f, darker(k.fridge, 0.22f))
        rbox(56f, 312f, 4f, 60f, 2f, darker(k.fridge, 0.22f))
        val m = k.stickers
        d.drawOval(m[3], Offset(17f, 241f), Size(14f, 10f))
        dot(22f, 324f, 8f, c(0xE9B468)); dot(22f, 324f, 5f, m[0])
        box(34f, 342f, 16f, 12f, m[1])
        d.rotate(-6f, Offset(27f, 387f)) { box(16f, 380f, 22f, 14f, c(0xFBF4E4)) }
        if (k.sign == Sign.CORNICELLO) poly(c(0xB3392B), 39f, 262f, 43f, 250f, 47f, 246f, 45f, 254f, 43f, 264f)
        if (k.sign == Sign.STICKY_NOTES) {
            box(18f, 270f, 14f, 13f, c(0xF2E36B)); box(36f, 404f, 14f, 13f, c(0x9AD6E8)); box(14f, 420f, 14f, 13f, c(0xF59AB0))
        }
        // what stands on top of the fridge: the note under it is the house rule
        when (host.id) {
            "naples" -> {
                d.drawOval(c(0xD39A55), Offset(17f, 207f), Size(40f, 16f))
                for (bx in listOf(24f, 33f, 42f)) { val p = Path().apply { moveTo(bx, 214f); quadraticTo(bx + 3, 210f, bx + 6, 214f) }; d.drawPath(p, c(0xA96D33), style = Stroke(1f)) }
            }
            "lyon" -> d.rotate(-8f, Offset(37f, 214f)) {
                rbox(8f, 208f, 62f, 11f, 5.5f, c(0xD9A25A))
                for (bx in listOf(18f, 30f, 42f, 54f)) line(bx, 210f, bx + 6, 216f, c(0xA9702E), 1.2f)
            }
            else -> {
                rbox(16f, 200f, 40f, 22f, 3f, k.stickers[1])
                box(16f, 204f, 40f, 3f, darker(k.stickers[1], 0.2f))
                rbox(26f, 208f, 20f, 9f, 2f, lighter(k.stickers[3], 0.3f))
            }
        }
    }

    private fun tilesPattern() {
        val x0 = 68f
        val y0 = 262f
        val w = 92f
        val h = 118f
        box(x0, y0, w, h, k.tileBg)
        val a = k.tileA
        val b = k.tileB
        when (k.tiles) {
            Tiles.MAJOLICA, Tiles.TALAVERA, Tiles.AZULEJO, Tiles.IZNIK -> {
                var y = y0
                while (y < y0 + h) {
                    var x = x0
                    while (x < x0 + w) {
                        val cx = x + 9
                        val cy = y + 9
                        when (k.tiles) {
                            Tiles.MAJOLICA -> { d.drawPath(Path().apply { moveTo(cx, y + 1); lineTo(x + 17, cy); lineTo(cx, y + 17); lineTo(x + 1, cy); close() }, a, style = Stroke(1.4f)); dot(cx, cy, 2.6f, b) }
                            Tiles.TALAVERA -> { dot(cx, cy, 5f, a); dot(cx, cy, 2.2f, b); for (q in listOf(x + 2, x + 16)) for (r in listOf(y + 2, y + 16)) dot(q, r, 1.6f, b) }
                            Tiles.AZULEJO -> { d.drawCircle(a, 6f, Offset(cx, cy), style = Stroke(1.2f)); for (q in 0 until 4) d.rotate(q * 90f, Offset(cx, cy)) { d.drawOval(a, Offset(cx - 1.5f, y + 0.5f), Size(3f, 5f)) }; dot(cx, cy, 1.8f, a) }
                            else -> { for (q in 0 until 4) d.rotate(q * 90f + 45f, Offset(cx, cy)) { d.drawOval(a, Offset(cx - 2f, cy - 8f), Size(4f, 7f)) }; dot(cx, cy, 2.4f, b) }
                        }
                        d.drawRect(darker(k.tileBg, 0.1f), Offset(x, y), Size(18f, 18f), style = Stroke(0.6f))
                        x += 18
                    }
                    y += 18
                }
            }
            Tiles.SUBWAY -> {
                var y = y0
                var row = 0
                while (y < y0 + h) {
                    var x = x0 - (row % 2) * 9
                    while (x < x0 + w) { rbox(x + 0.5f, y + 0.5f, 17f, 8f, 1.5f, if ((row + (x / 18).toInt()) % 3 == 0) b else a); x += 18 }
                    y += 9; row++
                }
            }
            Tiles.HEX -> {
                var y = y0
                var row = 0
                while (y < y0 + h) {
                    var x = x0 + (row % 2) * 7
                    while (x < x0 + w) {
                        val p = Path().apply { moveTo(x, y + 3); lineTo(x + 6, y); lineTo(x + 12, y + 3); lineTo(x + 12, y + 9); lineTo(x + 6, y + 12); lineTo(x, y + 9); close() }
                        d.drawPath(p, if ((x.toInt() / 14 + row) % 5 == 0) b else a)
                        d.drawPath(p, darker(k.tileBg, 0.15f), style = Stroke(0.6f))
                        x += 14
                    }
                    y += 10.5f; row++
                }
            }
            Tiles.CHECKER -> {
                var y = y0
                var row = 0
                while (y < y0 + h) {
                    var x = x0
                    var col = 0
                    while (x < x0 + w) { box(x, y, 11.5f, 11.8f, if ((row + col) % 2 == 0) a else b); x += 11.5f; col++ }
                    y += 11.8f; row++
                }
            }
            Tiles.WOOD -> {
                var x = x0
                var i = 0
                while (x < x0 + w) { box(x, y0, 11.5f, h, if (i % 2 == 0) a else b); line(x, y0, x, y0 + h, darker(a, 0.2f), 0.6f); x += 11.5f; i++ }
                box(x0, y0 + 40, w, 4f, darker(a, 0.25f))
            }
        }
    }

    private fun kitchen() {
        tilesPattern()
        box(66f, 380f, 96f, 8f, darker(k.wood, 0.05f))
        box(66f, 388f, 96f, 74f, lighter(k.wood, 0.28f))
        line(114f, 392f, 114f, 458f, k.wood); line(70f, 420f, 158f, 420f, k.wood)
        dot(108f, 406f, 2f, darker(k.wood, 0.3f)); dot(120f, 406f, 2f, darker(k.wood, 0.3f))
        for (i in 0 until 3) {
            val ph = ((time * 0.33f + i / 3f) % 1f)
            val a = when { ph < 0.4f -> ph / 0.4f * 0.7f; else -> (1 - ph) / 0.6f * 0.7f }
            val x = 90f + i * 8
            val y = 350f - ph * 14
            val p = Path().apply { moveTo(x, y); quadraticTo(x - 4, y - 6, x, y - 12); quadraticTo(x + 4, y - 18, x, y - 24) }
            d.drawPath(p, Color.White.copy(alpha = a), style = Stroke(2f))
        }
        rbox(82f, 354f, 34f, 26f, 3f, c(0x9AA3A8))
        rbox(79f, 351f, 40f, 5f, 2f, c(0x7F888D))
        vessel()
    }

    /** What's on the second ring: a moka, a kettle, a clay pot… */
    private fun vessel() {
        val x = 138f
        val y = 380f
        when (host.id) {
            "naples" -> { poly(c(0xB8BFC4), x - 4, y, x - 7, y - 12, x - 5, y - 14, x - 7, y - 24, x + 7, y - 24, x + 5, y - 14, x + 7, y - 12, x + 4, y); box(x + 7, y - 20, 4f, 2f, c(0x3A2A1E)) }
            "kyoto" -> { d.drawOval(c(0x2B2B2B), Offset(x - 11, y - 16), Size(22f, 16f)); d.drawArc(c(0x2B2B2B), 180f, 180f, false, Offset(x - 8, y - 26), Size(16f, 16f), style = Stroke(1.6f)) }
            "oaxaca", "tbilisi", "salvador" -> { d.drawOval(c(0xB5643A), Offset(x - 12, y - 20), Size(24f, 20f)); box(x - 6, y - 24, 12f, 5f, c(0xA0532E)) }
            "seoul" -> { rbox(x - 12, y - 12, 24f, 12f, 2f, c(0xE8B83A)); box(x - 18, y - 10, 6f, 2f, c(0xE8B83A)); box(x + 12, y - 10, 6f, 2f, c(0xE8B83A)) }
            "lyon" -> { rbox(x - 13, y - 14, 26f, 14f, 5f, c(0xC0392B)); d.drawOval(c(0xA93226), Offset(x - 13, y - 17), Size(26f, 6f)) }
            "lisbon" -> { d.drawArc(c(0xC87A3A), 0f, 180f, true, Offset(x - 12, y - 14), Size(24f, 20f)); d.drawArc(c(0xD98A4A), 180f, 180f, true, Offset(x - 12, y - 22), Size(24f, 20f)) }
            "istanbul" -> { rbox(x - 9, y - 12, 18f, 12f, 3f, c(0xC9C9C9)); rbox(x - 6, y - 22, 12f, 10f, 3f, c(0xD9D9D9)); dot(x, y - 23, 1.6f, c(0x3A2A1E)) }
            "kolkata" -> { rbox(x - 11, y - 14, 22f, 14f, 3f, c(0xC9C9C9)); box(x - 3, y - 20, 6f, 6f, c(0x3A3A3A)); box(x + 11, y - 12, 9f, 2.5f, c(0x3A3A3A)) }
            else -> { d.drawOval(c(0xD9D9D9), Offset(x - 11, y - 18), Size(22f, 18f)); box(x + 9, y - 14, 6f, 2f, c(0xD9D9D9)); box(x - 4, y - 21, 8f, 3f, c(0x3A3A3A)) }
        }
    }

    private fun cabinet() {
        box(168f, 400f, 126f, 62f, k.wood)
        box(172f, 436f, 56f, 22f, darker(k.wood, 0.12f)); box(234f, 436f, 56f, 22f, darker(k.wood, 0.12f))
        box(176f, 410f, 110f, 20f, lighter(k.wood, 0.1f))
        d.drawRect(darker(k.wood, 0.12f), Offset(176f, 410f), Size(110f, 20f), style = Stroke(1f))
        rbox(223f, 418f, 16f, 4f, 2f, c(0xD8B25A))
        d.rotate(-4f, Offset(207f, 409f)) { box(196f, 406f, 22f, 6f, c(0xFBF4E4)) }
    }

    private fun tv() {
        line(196f, 342f, 186f, 320f, c(0x3A2A1E), 1.5f); line(216f, 342f, 226f, 318f, c(0x3A2A1E), 1.5f)
        rbox(176f, 342f, 74f, 58f, 9f, darker(k.wood, 0.25f))
        rbox(182f, 348f, 50f, 46f, 7f, c(0x2C3A3F))
        val flick = if (light.lamps) 0.28f + sin(time * 5f) * 0.05f else 0.16f
        rbox(182f, 348f, 50f, 46f, 7f, c(0x9FC3C9).copy(alpha = flick))
        dot(241f, 358f, 3f, c(0xD8B25A)); dot(241f, 370f, 3f, c(0xD8B25A))
        for (i in 0..2) line(238f, 382f + i * 4, 244f, 382f + i * 4, c(0x2C3A3F))
    }

    private fun record() {
        rbox(254f, 384f, 38f, 16f, 2f, c(0xB47A4A))
        d.drawOval(c(0x1D1714), Offset(256f, 380.5f), Size(28f, 7f))
        d.drawOval(k.stickers[0], Offset(266f, 382.8f), Size(8f, 2.4f))
        line(288f, 380f, 280f, 386f, c(0xCCCCCC), 1.5f)
        d.rotate(8f, Offset(301f, 386f)) { box(300f, 372f, 3f, 28f, c(0x1D1714)) }
    }

    private fun books() {
        box(306f, 152f, 78f, 310f, k.wood)
        box(310f, 156f, 70f, 302f, darker(k.wood, 0.3f))
        for (r in 0 until 5) box(310f, 156f + (r + 1) * 60, 70f, 4f, k.wood)
        for (s in spines) {
            val base = 156f + (s.row + 1) * 60
            box(s.x, base - s.h, s.w, s.h, s.c)
            box(s.x, base - s.h + 6, s.w, 2f, Color.Black.copy(alpha = 0.13f))
        }
    }

    private fun night() {
        if (light.dark <= 0f) return
        d.drawRect(c(0x0F1A3A).copy(alpha = light.dark * 0.72f), Offset(-400f, -400f), Size(1190f, 1000f))
        if (light.lamps) {
            val (lx, ly) = if (k.lamp == Lamp.FAN) 108f to 46f else 108f to 54f
            d.drawCircle(
                Brush.radialGradient(listOf(c(0xFFD98A).copy(alpha = 0.55f), c(0xFFD98A).copy(alpha = 0f)), Offset(lx, ly), 110f),
                110f, Offset(lx, ly),
            )
            if (k.sign == Sign.CANDLES) d.drawCircle(
                Brush.radialGradient(listOf(c(0xFFC24A).copy(alpha = 0.4f), c(0xFFC24A).copy(alpha = 0f)), Offset(226f, 185f), 70f),
                70f, Offset(226f, 185f),
            )
            d.drawCircle(
                Brush.radialGradient(listOf(c(0x9FC3C9).copy(alpha = 0.22f), c(0x9FC3C9).copy(alpha = 0f)), Offset(207f, 371f), 60f),
                60f, Offset(207f, 371f),
            )
        }
    }

    private fun pins(unread: Set<Spot>) {
        for ((i, s) in Spot.entries.withIndex()) {
            if (s !in unread) continue
            val bob = sin(time * 2.6f + i) * 1.6f
            val x = s.px
            val y = s.py + bob
            d.rotate((x % 7f) - 3f, Offset(x, y)) {
                box(x - 7, y - 5, 14f, 11f, Color.White)
                d.drawRect(c(0xCBB88E), Offset(x - 7, y - 5), Size(14f, 11f), style = Stroke(0.6f))
                line(x - 4, y - 1, x + 4, y - 1, c(0x2B3F6B), 0.7f)
                line(x - 4, y + 2, x + 2, y + 2, c(0x2B3F6B), 0.7f)
                dot(x, y - 5, 2.4f, c(0xB3392B))
            }
        }
    }
}
