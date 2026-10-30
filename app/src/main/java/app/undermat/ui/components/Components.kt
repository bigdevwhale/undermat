package app.undermat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.undermat.ui.theme.C
import app.undermat.ui.theme.T

/** Lined notepaper with a strip of tape on top: every note from a host is written on this. */
@Composable
fun Paper(modifier: Modifier = Modifier, pad: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(top = 6.dp)
                .background(C.paper, RoundedCornerShape(6.dp))
                .lined()
                .padding(pad),
            content = content,
        )
        Box(Modifier.align(Alignment.TopCenter).rotate(-2f).background(Color(0xCCE9DCB1)).padding(horizontal = 34.dp, vertical = 7.dp))
    }
}

/** Ruled lines every 28dp, like a school exercise book. */
fun Modifier.lined(): Modifier = this.then(
    Modifier.drawBehindLines()
)

private fun Modifier.drawBehindLines(): Modifier = this.drawBehind {
    var y = 27.dp.toPx()
    val step = 28.dp.toPx()
    while (y < size.height) {
        drawLine(C.line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        y += step
    }
}

/** A card inside a note: a label and plain printed text. */
@Composable
fun Card(label: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(top = 12.dp)
            .background(C.card, RoundedCornerShape(6.dp))
            .border(1.dp, C.cardEdge, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (label != null) Text(label.uppercase(), style = T.Eyebrow, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}

/** A rounded button: dark ink, quiet outline, or done green. */
@Composable
fun Btn(text: String, modifier: Modifier = Modifier, primary: Boolean = true, done: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val bg = when {
        done -> C.green
        !enabled -> C.paper2
        primary -> C.ink
        else -> Color.Transparent
    }
    val fg = if ((primary || done) && enabled) C.paper else if (!enabled) C.faint else C.ink
    Box(
        modifier.clip(shape).background(bg).then(if (!primary && !done) Modifier.border(1.dp, Color(0xFFCDB98F), shape) else Modifier)
            .clickable(enabled = enabled && !done, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = T.Button.copy(color = fg, fontWeight = if (primary || done) T.Button.fontWeight else null), textAlign = TextAlign.Center)
    }
}

/**
 * A note sliding up over the flat: the room dims, the paper sits at the bottom, a tap outside closes it.
 */
@Composable
fun NoteSheet(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x661D1410))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 520.dp).navigationBarsPadding().padding(10.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState()),
        ) {
            Paper {
                Box(Modifier.fillMaxWidth()) {
                    Column(content = content)
                    Text("×", style = T.Title.copy(color = C.muted), modifier = Modifier.align(Alignment.TopEnd).clickable(onClick = onClose).padding(start = 12.dp, bottom = 8.dp))
                }
            }
        }
    }
}

/** A page of its own: a title with a back arrow, and scrolling content. */
@Composable
fun Page(title: String, onBack: () -> Unit, bg: Color = C.paper, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", style = T.Title, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 4.dp))
            Text(title, style = T.Title)
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(), content = content)
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, lines: Int = 1) {
    Box(modifier.fillMaxWidth().background(C.card, RoundedCornerShape(6.dp)).border(1.dp, C.cardEdge, RoundedCornerShape(6.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        if (value.isEmpty()) Text(hint, style = T.Body.copy(color = C.faint))
        BasicTextField(
            value, onChange, textStyle = T.Body, singleLine = lines == 1, minLines = lines, cursorBrush = SolidColor(C.red),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PaperDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Paper { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
    }
}

/**
 * A luggage sticker: an oval (or a square label) in the city's colour, a dashed inner rim, the city on top
 * and what it was for underneath.
 */
@Composable
fun StickerView(city: String, label: String, color: Color, square: Boolean, angle: Float, modifier: Modifier = Modifier) {
    val shape = if (square) RoundedCornerShape(6.dp) else RoundedCornerShape(percent = 50)
    Box(modifier.rotate(angle).clip(shape).background(color), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize().padding(4.dp)) {
            val r = if (square) 4.dp.toPx() else size.height / 2
            drawRoundRect(
                Color.White.copy(alpha = 0.55f), Offset.Zero, Size(size.width, size.height), androidx.compose.ui.geometry.CornerRadius(r),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(city, style = T.Eyebrow.copy(color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1)
            Text(label, style = T.Italic.copy(color = Color.White, fontSize = T.Small.fontSize, lineHeight = T.Small.lineHeight), textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

/** Something to tap in a row: a label with a sub-line. */
@Composable
fun Row2(title: String, sub: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Text(title, style = T.Heading)
        if (sub != null) Text(sub, style = T.Sub)
    }
}

/** Fills a box with cork: a warm brown with darker flecks. */
@Composable
fun BoxScope.Cork() {
    Canvas(Modifier.matchParentSize()) {
        drawRect(C.cork)
        var seed = 17L
        fun rnd(): Float { seed = (seed * 9301 + 49297) % 233280; return seed / 233280f }
        repeat(900) {
            drawCircle(if (rnd() < 0.5f) C.corkDark else Color(0xFFC99A68), (0.6f + rnd() * 1.6f) * density, Offset(rnd() * size.width, rnd() * size.height))
        }
    }
}
