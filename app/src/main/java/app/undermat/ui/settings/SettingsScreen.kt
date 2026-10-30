package app.undermat.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.undermat.BuildConfig
import app.undermat.R
import app.undermat.container
import app.undermat.data.Settings
import app.undermat.ui.components.Btn
import app.undermat.ui.components.CommunityLinks
import app.undermat.ui.components.Page
import app.undermat.ui.theme.C
import app.undermat.ui.theme.T
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(s: Settings, hasStay: Boolean, onBack: () -> Unit) {
    val c = LocalContext.current.container
    var hour by remember { mutableFloatStateOf(s.hour.toFloat()) }
    Page(stringResource(R.string.menu_settings), onBack) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.s_notify), style = T.Heading)
                Text(stringResource(R.string.s_notify_hint), style = T.Small)
            }
            Switch(
                s.notify, { on -> c.write { settings.update { it.copy(notify = on) } } },
                colors = SwitchDefaults.colors(checkedTrackColor = C.ink, checkedThumbColor = C.paper, uncheckedTrackColor = C.paper2, uncheckedBorderColor = C.ink, uncheckedThumbColor = C.ink),
            )
        }
        Text(stringResource(R.string.s_hour, hour.roundToInt()), style = T.Body, modifier = Modifier.padding(top = 10.dp))
        Slider(
            hour, { hour = it }, valueRange = 7f..23f, steps = 15, enabled = s.notify,
            onValueChangeFinished = { c.write { settings.update { it.copy(hour = hour.roundToInt()) } } },
            colors = SliderDefaults.colors(thumbColor = C.ink, activeTrackColor = C.ink, inactiveTrackColor = C.paper2),
        )
        Divider()

        Text(stringResource(R.string.s_language), style = T.Heading)
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            for ((tag, label) in listOf("" to stringResource(R.string.s_lang_system), "ru" to "Русский", "en" to "English")) {
                Btn(label, primary = current == tag) {
                    AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
                }
            }
        }
        Divider()

        if ((BuildConfig.DEBUG || BuildConfig.DEMO) && hasStay) {
            Text(stringResource(R.string.s_debug), style = T.Heading)
            Btn(stringResource(R.string.s_next_day), Modifier.padding(top = 6.dp), primary = false) { c.write { store.stay { it.copy(start = it.start - 1) } } }
            Divider()
        }
        Text(stringResource(R.string.community), style = T.Heading, modifier = Modifier.padding(bottom = 6.dp))
        CommunityLinks()
        Divider()
        Text(stringResource(R.string.s_credits), style = T.Small)
        Text("v" + BuildConfig.VERSION_NAME, style = T.Small)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.fillMaxWidth().padding(vertical = 14.dp), color = C.cardEdge)
