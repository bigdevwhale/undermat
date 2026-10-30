package app.undermat.ui.suitcase

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.undermat.R
import app.undermat.data.Host
import app.undermat.data.Kind
import app.undermat.data.Looks
import app.undermat.data.Save
import app.undermat.data.Stay
import app.undermat.ru
import app.undermat.ui.components.Card
import app.undermat.ui.components.Page
import app.undermat.ui.components.StickerView
import app.undermat.ui.theme.C
import app.undermat.ui.theme.T
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The old suitcase with every sticker earned, newest on top, in each city's colours. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuitcaseScreen(save: Save, hosts: List<Host>, onBack: () -> Unit) {
    val ru = LocalContext.current.ru
    Page(stringResource(R.string.suitcase), onBack) {
        Text(pluralStringResource(R.plurals.stickers, save.stickers.size, save.stickers.size) + " · " + pluralStringResource(R.plurals.cities, save.stickers.map { it.host }.distinct().size, save.stickers.map { it.host }.distinct().size), style = T.Sub)
        Box(
            Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 320.dp)
                .background(Color(0xFF7B4A2C), RoundedCornerShape(16.dp))
                .border(4.dp, Color(0xFF5F371F), RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            if (save.stickers.isEmpty()) Text(stringResource(R.string.suitcase_empty), style = T.Note.copy(color = Color(0xFFE9CFAE)), textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((i, s) in save.stickers.withIndex().reversed()) {
                    val look = Looks.all[s.host] ?: continue
                    val host = hosts.firstOrNull { it.id == s.host } ?: continue
                    val colour = when (s.kind) {
                        Kind.KITCHEN.name -> look.stickers[0]
                        Kind.TV.name -> look.stickers[1]
                        Kind.RECORD.name -> look.stickers[2]
                        Kind.BOOKS.name -> look.stickers[4]
                        else -> look.stickers[3]
                    }
                    val label = if (s.kind == "stay") stringResource(R.string.a_week, look.at.of(ru)) else s.label
                    val big = s.kind == "stay"
                    StickerView(
                        host.stamp, label, if (big) C.red else colour, square = i % 3 == 1 && !big, angle = ((i * 37) % 15 - 7).toFloat(),
                        modifier = Modifier.size(if (big) 150.dp else 104.dp, if (big) 84.dp else 66.dp),
                    )
                }
            }
        }
        Card(null) { Text(stringResource(R.string.suitcase_hint), style = T.Sub) }
        Spacer(Modifier.height(24.dp))
    }
}

/** Every week spent in someone's flat, newest first; a tap opens the flat again. */
@Composable
fun WeeksScreen(save: Save, hosts: List<Host>, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val ru = LocalContext.current.ru
    val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH)
    Page(stringResource(R.string.weeks), onBack) {
        if (save.past.isEmpty()) Text(stringResource(R.string.weeks_empty), style = T.Sub, modifier = Modifier.padding(top = 12.dp))
        for ((i, st) in save.past.withIndex().reversed()) {
            val h = hosts.firstOrNull { it.id == st.host } ?: continue
            val look = Looks.of(h.id)
            Column(
                Modifier.fillMaxWidth().padding(top = 10.dp).background(C.card, RoundedCornerShape(8.dp)).border(1.dp, C.cardEdge, RoundedCornerShape(8.dp))
                    .clickable { onOpen(i) }.padding(14.dp),
            ) {
                Text(h.stamp, style = T.Eyebrow.copy(color = look.stickers[0]))
                Text(look.at.of(ru).replaceFirstChar { it.uppercase() } + ", " + h.city.of(ru), style = T.Heading)
                Text(LocalDate.ofEpochDay(st.start).format(fmt) + " · " + pluralStringResource(R.plurals.done_things, st.done.size, st.done.size), style = T.Small)
                st.gift?.let { g ->
                    val parts = listOf(g.dish, g.film, g.song).filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) Text(stringResource(R.string.you_left, parts.joinToString(", ")), style = T.Italic, modifier = Modifier.padding(top = 4.dp))
                    if (g.note.isNotBlank()) Text("«" + g.note + "»", style = T.Note.copy(fontSize = T.Note.fontSize * 0.85f), modifier = Modifier.padding(top = 2.dp))
                }
                Text(stringResource(R.string.open_again), style = T.Button.copy(color = C.red), modifier = Modifier.padding(top = 6.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** How many of the four things of each day were done in a stay. */
fun Stay.doneCount() = done.size
