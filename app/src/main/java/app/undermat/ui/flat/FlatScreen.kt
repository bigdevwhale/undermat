package app.undermat.ui.flat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.undermat.BuildConfig
import app.undermat.R
import app.undermat.container
import app.undermat.data.Host
import app.undermat.data.Kind
import app.undermat.data.Look
import app.undermat.data.Stay
import app.undermat.ru
import app.undermat.ui.components.Btn
import app.undermat.ui.theme.C
import app.undermat.ui.theme.T
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

/** What's open over the flat. */
sealed interface Open {
    data class Note(val spot: Spot) : Open
    data class Guest(val i: Int) : Open
    data object Evening : Open
    data object Farewell : Open
}

/**
 * The host's flat. [stay] is the current week, or a past one opened again ([live] false): then every day
 * is open and nothing is marked read. [clock] pins the host's time (for screenshots).
 */
@Composable
fun FlatScreen(
    host: Host,
    look: Look,
    stay: Stay,
    live: Boolean,
    today: Long,
    suitcase: Int,
    onBoard: () -> Unit,
    onSuitcase: () -> Unit,
    onWeeks: () -> Unit,
    onSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
    clock: ZonedDateTime? = null,
    first: Open? = null,
    firstDay: Int? = null,
) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val dayNow = stay.day(today)
    var day by remember(stay.host, stay.start) { mutableStateOf(firstDay ?: dayNow) }
    var open by remember { mutableStateOf(first) }
    var local by remember { mutableStateOf(clock ?: ZonedDateTime.now(look.zone)) }
    val weather by c.weather.collectAsState()
    LaunchedEffect(host.id, clock) {
        if (clock != null) return@LaunchedEffect
        while (true) {
            runCatching { c.refreshWeather(host.id) }
            local = ZonedDateTime.now(look.zone)
            delay(20_000)
        }
    }
    // A neighbour comes by on days 2, 4 and 6, a little while after the guest looks in.
    val guest = (day / 2 - 1).takeIf { day % 2 == 0 && it in host.neighbours.indices && it !in stay.guests }
    var arrived by remember(day) { mutableStateOf(clock != null) }
    LaunchedEffect(day, guest) { if (guest != null && !arrived) { delay(6_000); arrived = true } }
    val visiting = guest != null && arrived && live

    val d = host.days[day - 1]
    val unread = if (live) Spot.entries.filter { !stay.isRead(day, it.id) }.toSet() else emptySet()

    BackHandler(enabled = open != null) { open = null }

    fun show(o: Open) {
        open = o
        if (o is Open.Note && live) c.write { store.read(day, o.spot.id) }
    }

    Box(Modifier.fillMaxSize().background(look.wall)) {
        Column(Modifier.fillMaxSize()) {
            Header(host, look, local, day, dayNow, unread.size, ru, onBack, onDay = { day = it }, onBoard, onWeeks, onSettings, live)
            Interior(
                host, look, local, weather[host.id]?.sky, unread, basket = visiting && look.basket, still = BuildConfig.SHOTS || clock != null,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onSpot = { show(Open.Note(it)) },
                onBasket = { guest?.let { show(Open.Guest(it)) } },
            )
            Footer(look, ru, suitcase, lastDay = live && dayNow == Stay.DAYS && day == Stay.DAYS, onSuitcase, onEvening = { show(Open.Evening) }, onLeave = { show(Open.Farewell) })
        }
        AnimatedVisibility(visiting && !look.basket && open == null, enter = slideInVertically { -it } + fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            val g = host.neighbours[guest ?: 0]
            Row(
                Modifier.statusBarsPadding().padding(top = 96.dp, start = 24.dp, end = 24.dp).shadow(6.dp, RoundedCornerShape(20.dp))
                    .background(C.ink, RoundedCornerShape(20.dp)).clickable { guest?.let { show(Open.Guest(it)) } }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.knock), style = T.Button.copy(color = C.gold))
                Spacer(Modifier.width(8.dp))
                Text(g.who.of(ru), style = T.Sub.copy(color = C.paper))
            }
        }
        when (val o = open) {
            is Open.Note -> Note(host, look, d, day, o.spot, local, weather[host.id], stay, live, today, onClose = { open = null }, onLeave = { show(Open.Farewell) })
            is Open.Guest -> Guest(host, o.i, onClose = { open = null }, onTake = {
                c.write { store.guest(o.i, host.id, host.neighbours[o.i].sticker) }
                open = null
            })
            Open.Evening -> Evening(host, look, day, onClose = { open = null })
            Open.Farewell -> Farewell(host, look, onClose = { open = null }, onDone = { gift ->
                open = null
                c.write { store.leave(today, gift, "") }
            })
            null -> Unit
        }
    }
}

@Composable
private fun Header(
    host: Host, look: Look, local: ZonedDateTime, day: Int, dayNow: Int, unread: Int, ru: Boolean, onBack: (() -> Unit)?,
    onDay: (Int) -> Unit, onBoard: () -> Unit, onWeeks: () -> Unit, onSettings: () -> Unit, live: Boolean,
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(look.wall).statusBarsPadding().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) Text("←", style = T.Title, modifier = Modifier.clickable(onClick = onBack).padding(end = 10.dp))
            Box(
                Modifier.size(40.dp).background(look.stickers[0], CircleShape).border(2.dp, C.paper, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(host.name.of(ru).first().toString(), style = T.Note.copy(color = Color.White, fontWeight = FontWeight.Bold)) }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(look.at.of(ru).replaceFirstChar { it.uppercase() }, style = T.Heading, maxLines = 1)
                Text(host.city.of(ru) + ", " + host.address.of(ru), style = T.Small.copy(color = darkOn(look)), maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.there_now), style = T.Small.copy(color = darkOn(look)))
                Text("%02d:%02d".format(local.hour, local.minute), style = T.Title.copy(fontSize = T.Heading.fontSize))
            }
            Box {
                Text("⋮", style = T.Title, modifier = Modifier.clickable { menu = true }.padding(horizontal = 10.dp, vertical = 2.dp))
                DropdownMenu(menu, { menu = false }, containerColor = C.paper) {
                    DropdownMenuItem({ Text(stringResource(R.string.menu_board), style = T.Body) }, { menu = false; onBoard() })
                    DropdownMenuItem({ Text(stringResource(R.string.menu_weeks), style = T.Body) }, { menu = false; onWeeks() })
                    DropdownMenuItem({ Text(stringResource(R.string.menu_settings), style = T.Body) }, { menu = false; onSettings() })
                }
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            for (n in 1..Stay.DAYS) {
                val open = n <= dayNow
                val on = n == day
                Box(
                    Modifier.size(30.dp)
                        .background(if (on) C.ink else if (open) C.paper2 else Color.Transparent, CircleShape)
                        .border(1.dp, if (on) C.ink else if (open) Color(0xFFD3B27A) else C.faint.copy(alpha = 0.6f), CircleShape)
                        .clickable(enabled = open) { onDay(n) },
                    contentAlignment = Alignment.Center,
                ) { Text("$n", style = T.Sub.copy(color = if (on) C.paper else if (open) C.ink else C.faint)) }
            }
            Spacer(Modifier.weight(1f))
            if (live) Text(
                if (unread > 0) pluralStringResource(R.plurals.notes_left, unread) else stringResource(R.string.all_read),
                style = T.Small.copy(color = darkOn(look)), modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
private fun pluralStringResource(id: Int, n: Int) = androidx.compose.ui.res.pluralStringResource(id, n, n)

/** A muted text colour that still reads on this flat's wall. */
fun darkOn(look: Look): Color = if (look.wall.luminance() < 0.35f) Color(0xFFF3E7CC) else C.ink.copy(alpha = 0.7f)

private fun Color.luminance() = 0.2126f * red + 0.7152f * green + 0.0722f * blue

@Composable
private fun Footer(look: Look, ru: Boolean, suitcase: Int, lastDay: Boolean, onSuitcase: () -> Unit, onEvening: () -> Unit, onLeave: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(look.wood).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FootBtn(Modifier.weight(1f), onSuitcase) {
            Text(stringResource(R.string.suitcase), style = T.Body)
            if (suitcase > 0) {
                Spacer(Modifier.width(6.dp))
                Text("$suitcase", style = T.Small.copy(color = Color.White), modifier = Modifier.background(C.red, RoundedCornerShape(10.dp)).padding(horizontal = 7.dp, vertical = 1.dp))
            }
        }
        FootBtn(Modifier.weight(1f), onEvening) { Text(stringResource(R.string.evening_in, look.inCity.of(ru)), style = T.Body, maxLines = 1) }
        if (lastDay) FootBtn(Modifier.weight(0.8f), onLeave, dark = true) { Text(stringResource(R.string.leave), style = T.Body.copy(color = C.paper)) }
    }
}

@Composable
private fun FootBtn(modifier: Modifier, onClick: () -> Unit, dark: Boolean = false, content: @Composable () -> Unit) {
    Row(
        modifier.shadow(1.dp, RoundedCornerShape(14.dp)).background(if (dark) C.ink else C.paper, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/** The host's signature at the end of a handwritten note. */
@Composable
fun Signed(text: String, sign: String) {
    Text(text, style = T.Note, modifier = Modifier.padding(top = 10.dp))
    Text(sign, style = T.Note, modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
}

/** Labels for marking something done; past participles, so they fit any guest. */
@Composable
fun doneLabel(k: Kind, done: Boolean): String = stringResource(
    when (k) {
        Kind.KITCHEN -> if (done) R.string.done_kitchen else R.string.do_kitchen
        Kind.TV -> if (done) R.string.done_tv else R.string.do_tv
        Kind.RECORD -> if (done) R.string.done_record else R.string.do_record
        Kind.BOOKS -> if (done) R.string.done_books else R.string.do_books
    },
)

