package app.undermat.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.undermat.R
import app.undermat.container
import app.undermat.data.Looks
import app.undermat.data.Settings
import app.undermat.ui.board.BoardScreen
import app.undermat.ui.components.Btn
import app.undermat.ui.components.Paper
import app.undermat.ui.door.DoorScreen
import app.undermat.ui.flat.FlatScreen
import app.undermat.ui.settings.SettingsScreen
import app.undermat.ui.suitcase.SuitcaseScreen
import app.undermat.ui.suitcase.WeeksScreen
import app.undermat.ui.theme.C
import app.undermat.ui.theme.T
import app.undermat.work.Reminders

sealed interface Layer {
    data object Board : Layer
    data object Suitcase : Layer
    data object Weeks : Layer
    data object Settings : Layer
    data class Past(val i: Int) : Layer
}

@Composable
fun UndermatRoot(settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val save by c.store.state.collectAsStateWithLifecycle()
    var today by remember { mutableStateOf(c.today()) }
    LaunchedEffect(Unit) { c.store.load() }
    LifecycleResumeEffect(Unit) {
        today = c.today()
        c.write { Reminders.seen(ctx) }
        onPauseOrDispose { }
    }
    if (!settings.onboarded) { Welcome(); return }
    val s = save ?: run { Box(Modifier.fillMaxSize().background(C.paper)); return }

    val layers = remember { mutableStateListOf<Layer>() }
    fun push(l: Layer) = layers.add(l)
    fun pop() { if (layers.isNotEmpty()) layers.removeAt(layers.lastIndex) }
    BackHandler(enabled = layers.isNotEmpty()) { pop() }

    val stay = s.stay
    val host = stay?.let { c.host(it.host) }
    Box(Modifier.fillMaxSize()) {
        when {
            stay == null || host == null -> BoardScreen(
                c.hosts, s, today, onTake = { h -> c.write { store.take(h.id, today) } }, onHome = null,
                onSuitcase = { push(Layer.Suitcase) }, onSettings = { push(Layer.Settings) }, onWeeks = { push(Layer.Weeks) },
            )
            !stay.entered -> DoorScreen(
                host, Looks.of(host.id),
                onEnter = { c.write { store.stay { it.copy(entered = true) } } },
                onBack = { push(Layer.Board) },
            )
            else -> FlatScreen(
                host, Looks.of(host.id), stay, live = true, today = today, suitcase = s.stickers.size,
                onBoard = { push(Layer.Board) }, onSuitcase = { push(Layer.Suitcase) }, onWeeks = { push(Layer.Weeks) }, onSettings = { push(Layer.Settings) },
            )
        }
        when (val l = layers.lastOrNull()) {
            Layer.Board -> BoardScreen(
                c.hosts, s, today, onTake = { h -> layers.clear(); c.write { store.take(h.id, today) } }, onHome = { pop() },
                onSuitcase = { push(Layer.Suitcase) }, onSettings = { push(Layer.Settings) }, onWeeks = { push(Layer.Weeks) },
            )
            Layer.Suitcase -> SuitcaseScreen(s, c.hosts, onBack = { pop() })
            Layer.Weeks -> WeeksScreen(s, c.hosts, onBack = { pop() }, onOpen = { push(Layer.Past(it)) })
            Layer.Settings -> SettingsScreen(settings, stay != null, onBack = { pop() })
            is Layer.Past -> {
                val past = s.past.getOrNull(l.i)
                val h = past?.let { c.host(it.host) }
                if (past != null && h != null) FlatScreen(
                    h, Looks.of(h.id), past, live = false, today = today, suitcase = s.stickers.size,
                    onBoard = { push(Layer.Board) }, onSuitcase = { push(Layer.Suitcase) }, onWeeks = { pop() }, onSettings = { push(Layer.Settings) },
                    onBack = { pop() },
                ) else pop()
            }
            null -> Unit
        }
    }
}

/** The first page: what the app is, in the hand of someone who's lent you their flat. */
@Composable
private fun Welcome() {
    val c = LocalContext.current.container
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { c.write { settings.update { it.copy(onboarded = true) } } }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFE8C590)).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.app_name), style = T.Display)
        Text(stringResource(R.string.w_sub), style = T.Body)
        Paper {
            Text(stringResource(R.string.w_note), style = T.Note)
            Text(stringResource(R.string.w_sign), style = T.Note, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
        Text(stringResource(R.string.w_how), style = T.Sub)
        Btn(stringResource(R.string.w_go), Modifier.fillMaxWidth()) {
            if (Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            else c.write { settings.update { it.copy(onboarded = true) } }
        }
    }
}
