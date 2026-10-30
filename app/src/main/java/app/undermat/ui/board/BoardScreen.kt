package app.undermat.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.undermat.R
import app.undermat.data.Host
import app.undermat.data.Looks
import app.undermat.data.Save
import app.undermat.ru
import app.undermat.ui.components.Btn
import app.undermat.ui.components.Cork
import app.undermat.ui.components.PaperDialog
import app.undermat.ui.theme.C
import app.undermat.ui.theme.T

/** The cork board of flats offered for a week: one card each, pinned slightly askew. */
@Composable
fun BoardScreen(hosts: List<Host>, save: Save, today: Long, onTake: (Host) -> Unit, onHome: (() -> Unit)?, onSuitcase: () -> Unit, onSettings: () -> Unit, onWeeks: () -> Unit) {
    val ru = LocalContext.current.ru
    var pick by remember { mutableStateOf<Host?>(null) }
    Box(Modifier.fillMaxSize()) {
        Cork()
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onHome != null) Text("←", style = T.Title.copy(color = C.paper), modifier = Modifier.clickable(onClick = onHome).padding(end = 12.dp))
                Text(stringResource(R.string.board), style = T.Title.copy(color = C.paper), modifier = Modifier.weight(1f), maxLines = 1)
                Chip("⚙", onSettings)
            }
            Text(stringResource(R.string.board_sub), style = T.Sub.copy(color = C.paper.copy(alpha = 0.9f)), modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.suitcase) + if (save.stickers.isNotEmpty()) " · ${save.stickers.size}" else "", onSuitcase)
                if (save.past.isNotEmpty()) Chip(stringResource(R.string.weeks) + " · ${save.past.size}", onWeeks)
            }
            val st = save.stay
            if (st != null && onHome != null) {
                val h = hosts.firstOrNull { it.id == st.host }
                if (h != null) Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp).background(C.ink, RoundedCornerShape(12.dp)).clickable(onClick = onHome).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.now_at, Looks.of(h.id).at.of(ru), st.day(today)), style = T.Body.copy(color = C.paper), modifier = Modifier.weight(1f))
                    Text("→", style = T.Title.copy(color = C.gold))
                }
            }
            for (row in hosts.chunked(2)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for ((i, h) in row.withIndex()) Listing(h, save.visits(h.id), current = st?.host == h.id, tilt = ((h.id.hashCode() % 5) - 2) * 0.7f + i * 0.4f, Modifier.weight(1f)) { pick = h }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
    pick?.let { h ->
        val look = Looks.of(h.id)
        PaperDialog({ pick = null }) {
            Text(h.city.of(ru).uppercase() + " · " + h.country.of(ru), style = T.Eyebrow)
            Text(h.name.of(ru), style = T.Title)
            Text(h.address.of(ru), style = T.Italic)
            Text(h.about.of(ru), style = T.Body)
            Text(h.listing.of(ru), style = T.Note)
            val st = save.stay
            if (st != null && st.host != h.id) {
                val cur = hosts.firstOrNull { it.id == st.host }
                if (cur != null) Text(stringResource(R.string.ends_stay, Looks.of(cur.id).at.of(ru)), style = T.Small.copy(color = C.red))
            }
            if (st?.host == h.id) Btn(stringResource(R.string.go_home), Modifier.fillMaxWidth()) { pick = null; onHome?.invoke() }
            else Btn(stringResource(R.string.take_key), Modifier.fillMaxWidth()) { pick = null; onTake(h) }
            Text(stringResource(R.string.week_hint, look.inCity.of(ru)), style = T.Small)
        }
    }
}

@Composable
private fun Chip(text: String, onClick: () -> Unit) {
    Text(
        text, style = T.Sub.copy(color = C.ink),
        modifier = Modifier.background(C.paper, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun Listing(h: Host, visits: Int, current: Boolean, tilt: Float, modifier: Modifier, onClick: () -> Unit) {
    val ru = LocalContext.current.ru
    val look = Looks.of(h.id)
    Box(modifier.rotate(tilt)) {
        Column(
            Modifier.fillMaxWidth().padding(top = 6.dp).shadow(4.dp, RoundedCornerShape(3.dp)).background(C.paper, RoundedCornerShape(3.dp))
                .clickable(onClick = onClick),
        ) {
            Row(Modifier.fillMaxWidth().height(8.dp)) { for (col in look.stickers) Box(Modifier.weight(1f).height(8.dp).background(col)) }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(h.stamp, style = T.Eyebrow.copy(color = look.stickers[0]))
                Text(h.city.of(ru), style = T.Title)
                Text(h.country.of(ru) + " · " + h.name.of(ru), style = T.Small, maxLines = 1)
                Text(h.listing.of(ru), style = T.Note.copy(fontSize = T.Note.fontSize * 0.82f, lineHeight = T.Note.lineHeight * 0.82f), modifier = Modifier.padding(top = 6.dp), maxLines = 5)
                if (current || visits > 0) Text(
                    if (current) stringResource(R.string.you_are_here) else stringResource(R.string.been_here),
                    style = T.Small.copy(color = if (current) C.red else C.green), modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Canvas(Modifier.align(Alignment.TopCenter).size(14.dp)) {
            drawCircle(Color(0x33000000), size.width / 2, Offset(size.width / 2 + 2, size.height / 2 + 2))
            drawCircle(look.stickers[0], size.width / 2)
            drawCircle(Color.White.copy(alpha = 0.5f), size.width / 6, Offset(size.width * 0.38f, size.height * 0.38f))
        }
    }
}
