package app.undermat.ui.flat

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.undermat.R
import app.undermat.container
import app.undermat.data.Day
import app.undermat.data.Gift
import app.undermat.data.Holiday
import app.undermat.data.Host
import app.undermat.data.Item
import app.undermat.data.Kind
import app.undermat.data.Look
import app.undermat.data.Stay
import app.undermat.domain.Sky
import app.undermat.domain.WeatherNow
import app.undermat.ru
import app.undermat.ui.Outside
import app.undermat.ui.Voice
import app.undermat.ui.components.Btn
import app.undermat.ui.components.Card
import app.undermat.ui.components.Field
import app.undermat.ui.components.NoteSheet
import app.undermat.ui.components.PaperDialog
import app.undermat.ui.theme.C
import app.undermat.ui.theme.T
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@Composable
private fun Tag(text: String) = Text(text.uppercase(), style = T.Eyebrow)

@Composable
fun Note(
    host: Host, look: Look, d: Day, day: Int, spot: Spot, local: ZonedDateTime, weather: WeatherNow?, stay: Stay, live: Boolean, today: Long,
    onClose: () -> Unit, onLeave: () -> Unit,
) {
    val ctx = LocalContext.current
    val ru = ctx.ru
    val sign = host.sign.of(ru)
    val tag = stringResource(R.string.tag_day, place(spot), day)
    NoteSheet(onClose) {
        Tag(tag)
        when (spot) {
            Spot.KITCHEN -> ItemNote(host, d.kitchen, day, stay, live)
            Spot.TV -> ItemNote(host, d.tv, day, stay, live)
            Spot.RECORD -> ItemNote(host, d.record, day, stay, live)
            Spot.BOOKS -> ItemNote(host, d.books, day, stay, live)
            Spot.FRIDGE -> Fridge(host, d)
            Spot.RULES -> {
                Text(stringResource(R.string.rules_title), style = T.Title, modifier = Modifier.padding(top = 4.dp))
                Signed(d.rule.of(ru), sign)
                Card(stringResource(R.string.rules_card)) { Text(stringResource(R.string.rules_hint), style = T.Sub) }
            }
            Spot.DRAWER -> {
                Text(stringResource(R.string.letter_title), style = T.Title, modifier = Modifier.padding(top = 4.dp))
                Signed(d.letter.of(ru), sign)
                if (live && day == Stay.DAYS && stay.left == null) {
                    Btn(stringResource(R.string.leave_gift), Modifier.fillMaxWidth().padding(top = 14.dp), onClick = onLeave)
                }
            }
            Spot.WORD -> {
                Text(host.word, style = T.Big, modifier = Modifier.padding(top = 4.dp))
                Text(stringResource(R.string.word_sub), style = T.Italic)
                Text(host.wordText.of(ru), style = T.Note, modifier = Modifier.padding(top = 10.dp))
            }
            Spot.WINDOW -> Window(host, look, local, weather)
            Spot.CALENDAR -> Calendar(host, local.toLocalDate())
        }
    }
}

@Composable
private fun place(spot: Spot) = stringResource(
    when (spot) {
        Spot.KITCHEN -> R.string.p_kitchen
        Spot.TV -> R.string.p_tv
        Spot.RECORD -> R.string.p_record
        Spot.BOOKS -> R.string.p_books
        Spot.FRIDGE -> R.string.p_fridge
        Spot.RULES -> R.string.p_rules
        Spot.DRAWER -> R.string.p_drawer
        Spot.WORD -> R.string.p_word
        Spot.WINDOW -> R.string.p_window
        Spot.CALENDAR -> R.string.p_calendar
    },
)

@Composable
private fun ItemNote(host: Host, it: Item, day: Int, stay: Stay, live: Boolean) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    var where by remember { mutableStateOf(false) }
    val save by c.store.state.collectAsState()
    val done = live && (save?.stay?.isDone(day, it.kind) ?: stay.isDone(day, it.kind))
    Text(it.title.of(ru), style = T.Title, modifier = Modifier.padding(top = 4.dp))
    val meta = listOf(it.orig, it.by.of(ru), it.year).filter { s -> s.isNotBlank() && s != it.title.of(ru) }.joinToString(" · ")
    if (meta.isNotBlank()) Text(meta, style = T.Italic)
    Signed(it.note.of(ru), host.sign.of(ru))
    if (it.kind == Kind.KITCHEN) {
        Card(stringResource(R.string.need)) { for (line in it.ingr.of(ru)) Text("•  $line", style = T.Body) }
        val swaps = it.swaps.of(ru)
        if (swaps.isNotEmpty()) Card(stringResource(R.string.swaps)) {
            for ((a, b) in swaps) Text(a + "  →  " + b, style = T.Body)
        }
    }
    if (it.why.of(ru).isNotBlank()) Card(stringResource(R.string.why)) { Text(it.why.of(ru), style = T.Body) }
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (it.kind == Kind.KITCHEN) {
            val list = it.title.of(ru) + "\n" + it.ingr.of(ru).joinToString("\n") { s -> "— $s" }
            Btn(stringResource(R.string.shopping), Modifier.weight(1f), primary = false) { Outside.share(ctx, list) }
        } else Btn(stringResource(R.string.where), Modifier.weight(1f), primary = false) { where = true }
        if (live) Btn(doneLabel(it.kind, done), Modifier.weight(1f), done = done) {
            c.write { store.done(day, it.kind, host.id, it.sticker) }
            Toast.makeText(ctx, ctx.getString(R.string.sticker_in, it.sticker), Toast.LENGTH_SHORT).show()
        }
    }
    if (where) PaperDialog({ where = false }) {
        Text(stringResource(R.string.where_title), style = T.Heading)
        Text(Outside.query(it, ru), style = T.Italic)
        for (w in Outside.places(it, ru)) {
            Text(w.name, style = T.Body.copy(textDecoration = TextDecoration.Underline), modifier = Modifier.fillMaxWidth().clickable { Outside.open(ctx, w.url); where = false }.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun Fridge(host: Host, d: Day) {
    val ctx = LocalContext.current
    val ru = ctx.ru
    val voice = remember { Voice(ctx) }
    DisposableEffect(Unit) { onDispose { voice.close() } }
    Text(stringResource(R.string.fridge_title), style = T.Title, modifier = Modifier.padding(top = 4.dp))
    Card(stringResource(R.string.fact)) { Text(d.fact.of(ru), style = T.Body) }
    Card(stringResource(R.string.phrase)) {
        Text(d.phrase.say, style = T.Big)
        if (d.phrase.latin.isNotBlank()) Text(d.phrase.latin, style = T.Italic)
        Text(d.phrase.text.of(ru), style = T.Body, modifier = Modifier.padding(top = 4.dp))
    }
    Btn(stringResource(R.string.say_it), Modifier.fillMaxWidth().padding(top = 14.dp), primary = false) {
        if (!voice.say(d.phrase.say, host.lang)) Toast.makeText(ctx, R.string.no_voice, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun Window(host: Host, look: Look, local: ZonedDateTime, weather: WeatherNow?) {
    val ru = LocalContext.current.ru
    val here = ZonedDateTime.now()
    val diff = ((local.offset.totalSeconds - here.offset.totalSeconds) / 3600.0).let { if (it == it.roundToInt().toDouble()) it.roundToInt().toString() else "%.1f".format(it) }
    val ahead = local.offset.totalSeconds - here.offset.totalSeconds
    Text(stringResource(R.string.window_title), style = T.Title, modifier = Modifier.padding(top = 4.dp))
    val time = "%02d:%02d".format(local.hour, local.minute)
    val shift = when {
        ahead > 0 -> stringResource(R.string.ahead, diff)
        ahead < 0 -> stringResource(R.string.behind, diff.removePrefix("-"))
        else -> stringResource(R.string.same_time)
    }
    Card(stringResource(R.string.time_there, host.city.of(ru), time) + " · " + shift) { Text(host.hourText(local.hour).of(ru), style = T.Body) }
    Card(stringResource(R.string.weather)) {
        Text(
            if (weather != null) skyWord(weather.sky) + ", " + "%+d°".format(weather.temp.roundToInt()) + (if (weather.wind > 25) ", " + stringResource(R.string.windy) else "")
            else stringResource(R.string.no_weather),
            style = T.Body,
        )
    }
}

@Composable
private fun skyWord(s: Sky) = stringResource(
    when (s) {
        Sky.CLEAR -> R.string.sky_clear
        Sky.CLOUDY -> R.string.sky_cloudy
        Sky.OVERCAST -> R.string.sky_overcast
        Sky.FOG -> R.string.sky_fog
        Sky.DRIZZLE -> R.string.sky_drizzle
        Sky.RAIN -> R.string.sky_rain
        Sky.STORM -> R.string.sky_storm
        Sky.SNOW -> R.string.sky_snow
        Sky.SLEET -> R.string.sky_sleet
    },
)

/** The host's holidays from [today] on, nearest first. */
fun upcoming(host: Host, today: LocalDate): List<Pair<Holiday, LocalDate>> = host.holidays.mapNotNull { h ->
    runCatching {
        val d = LocalDate.of(today.year, h.m, h.d)
        h to (if (d.isBefore(today)) d.plusYears(1) else d)
    }.getOrNull()
}.sortedBy { it.second }

@Composable
private fun Calendar(host: Host, today: LocalDate) {
    val ru = LocalContext.current.ru
    val next = upcoming(host, today)
    val fmt = DateTimeFormatter.ofPattern("d MMMM", if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH)
    val (h, date) = next.firstOrNull() ?: return
    val days = ChronoUnit.DAYS.between(today, date).toInt()
    Text(if (days == 0) stringResource(R.string.today_is, h.name.of(ru)) else h.name.of(ru), style = T.Title, modifier = Modifier.padding(top = 4.dp))
    Text(date.format(fmt) + if (days > 0) " · " + pluralStringResource(R.plurals.in_days, days, days) else "", style = T.Italic)
    Card(stringResource(R.string.how_here, host.city.of(ru))) { Text(h.text.of(ru), style = T.Body) }
    if (next.size > 1) Card(stringResource(R.string.next_holidays)) {
        for ((hh, dd) in next.drop(1).take(3)) Text(dd.format(fmt) + " — " + hh.name.of(ru), style = T.Body)
    }
}

@Composable
fun Guest(host: Host, i: Int, onClose: () -> Unit, onTake: () -> Unit) {
    val ru = LocalContext.current.ru
    val g = host.neighbours[i]
    NoteSheet(onClose) {
        Tag(stringResource(R.string.guest_tag))
        Text(g.gift.of(ru), style = T.Title, modifier = Modifier.padding(top = 4.dp))
        Signed(g.note.of(ru), "— " + g.who.of(ru))
        Card(stringResource(R.string.how_live)) { Text(g.how.of(ru), style = T.Body) }
        Btn(stringResource(R.string.take_gift), Modifier.fillMaxWidth().padding(top = 14.dp), onClick = onTake)
    }
}

@Composable
fun Evening(host: Host, look: Look, day: Int, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val ru = ctx.ru
    val d = host.days[day - 1]
    val facts = (day downTo 1).map { host.days[it - 1].fact.of(ru) }.take(3)
    val title = stringResource(R.string.evening_in, look.inCity.of(ru))
    val dish = stringResource(R.string.e_table)
    val music = stringResource(R.string.e_music)
    val film = stringResource(R.string.e_film)
    val toast = stringResource(R.string.e_toast)
    NoteSheet(onClose) {
        Tag(stringResource(R.string.e_tag))
        Text(title, style = T.Title, modifier = Modifier.padding(top = 4.dp))
        Card(dish) { Text(d.kitchen.title.of(ru), style = T.Heading); Text(d.kitchen.orig, style = T.Italic) }
        Card(music) { Text(d.record.title.of(ru), style = T.Heading); Text(d.record.by.of(ru), style = T.Italic) }
        Card(film) { Text(d.tv.title.of(ru), style = T.Heading); Text(listOf(d.tv.by.of(ru), d.tv.year).filter { it.isNotBlank() }.joinToString(" · "), style = T.Italic) }
        Card(toast) { Text(host.toast, style = T.Big); Text(host.toastText.of(ru), style = T.Body) }
        Card(stringResource(R.string.e_talk)) { for (f in facts) Text("•  $f", style = T.Body, modifier = Modifier.padding(bottom = 4.dp)) }
        Btn(stringResource(R.string.share), Modifier.fillMaxWidth().padding(top = 14.dp), primary = false) {
            Outside.share(
                ctx,
                "$title\n\n$dish: ${d.kitchen.title.of(ru)} (${d.kitchen.orig})\n$music: ${d.record.title.of(ru)} — ${d.record.by.of(ru)}\n" +
                    "$film: ${d.tv.title.of(ru)}\n$toast: ${host.toast}\n\n" + facts.joinToString("\n") { "• $it" },
            )
        }
    }
}

@Composable
fun Farewell(host: Host, look: Look, onClose: () -> Unit, onDone: (Gift) -> Unit) {
    val ru = LocalContext.current.ru
    var dish by remember { mutableStateOf("") }
    var film by remember { mutableStateOf("") }
    var song by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    NoteSheet(onClose) {
        Tag(stringResource(R.string.f_tag))
        Text(stringResource(R.string.f_title, host.name.of(ru)), style = T.Title, modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(R.string.f_text), style = T.Sub, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(dish, { dish = it }, stringResource(R.string.f_dish))
            Field(film, { film = it }, stringResource(R.string.f_film))
            Field(song, { song = it }, stringResource(R.string.f_song))
            Field(note, { note = it }, stringResource(R.string.f_note, host.name.of(ru)), lines = 3)
        }
        Btn(stringResource(R.string.f_go), Modifier.fillMaxWidth().padding(top = 14.dp)) { onDone(Gift(dish.trim(), film.trim(), song.trim(), note.trim())) }
        Text(stringResource(R.string.f_hint, look.at.of(ru)), style = T.Small, modifier = Modifier.padding(top = 8.dp))
    }
}
