package app.undermat.ui.flat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.undermat.data.Look
import app.undermat.domain.Sky
import app.undermat.domain.Sun
import java.time.ZonedDateTime

/**
 * The light in the host's flat right now: the sky in the window, how dark the room is, where the sun or
 * moon hangs. Worked out from the real sunrise and sunset in the host's city.
 */
class Light(val top: Color, val bottom: Color, val dark: Float, val day: Boolean, val arc: Float) {
    /** The lamp is on and the city's windows are lit. */
    val lamps get() = dark > 0.12f

    companion object {
        private class Key(val top: Long, val bottom: Long, val dark: Float)
        private val NIGHT = Key(0x0D1630, 0x252A52, 0.52f)
        private val DAWN = Key(0xF3A37A, 0xF7D6A0, 0.16f)
        private val DAY = Key(0x6FB1E0, 0xCFE8F5, 0f)
        private val SUNSET = Key(0xE8774F, 0xF6C27A, 0.08f)
        private val DUSK = Key(0x3B3A6B, 0xC46A6A, 0.3f)

        private fun col(hex: Long) = Color(0xFF000000 or hex)

        fun at(local: ZonedDateTime, look: Look, sky: Sky?): Light {
            val m = local.hour * 60 + local.minute
            val sun = Sun.times(local.toLocalDate(), look.lat, look.lon, look.zone)
            val rise = sun?.rise ?: 360
            val set = sun?.set ?: 1080
            val keys = listOf(
                rise - 70 to NIGHT, rise - 15 to DAWN, rise + 45 to DAY, set - 60 to DAY, set to SUNSET, set + 40 to DUSK, set + 80 to NIGHT,
            )
            var top: Color
            var bottom: Color
            var dark: Float
            if (m <= keys.first().first || m >= keys.last().first) {
                top = col(NIGHT.top); bottom = col(NIGHT.bottom); dark = NIGHT.dark
            } else {
                val i = keys.indexOfLast { it.first <= m }
                val (m0, k0) = keys[i]
                val (m1, k1) = keys[i + 1]
                val t = if (m1 == m0) 0f else (m - m0).toFloat() / (m1 - m0)
                top = lerp(col(k0.top), col(k1.top), t)
                bottom = lerp(col(k0.bottom), col(k1.bottom), t)
                dark = k0.dark + (k1.dark - k0.dark) * t
            }
            if (sky != null && sky.grey) {
                val grey = if (dark > 0.3f) Color(0xFF2A2D38) else Color(0xFF9AA3AD)
                top = lerp(top, grey, 0.6f); bottom = lerp(bottom, grey, 0.45f)
                dark = (dark + 0.06f).coerceAtMost(0.56f)
            }
            val day = m in rise..set
            val arc = if (day) (m - rise).toFloat() / (set - rise).coerceAtLeast(1)
            else ((m - set + 1440) % 1440).toFloat() / (1440 - (set - rise)).coerceAtLeast(1)
            return Light(top, bottom, dark, day, arc.coerceIn(0f, 1f))
        }
    }
}
