package app.undermat

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.undermat.data.DemoSeed
import app.undermat.data.Looks
import app.undermat.data.Save
import app.undermat.domain.Sky
import app.undermat.domain.WeatherNow
import app.undermat.ui.board.BoardScreen
import app.undermat.ui.door.DoorScreen
import app.undermat.ui.flat.FlatScreen
import app.undermat.ui.flat.Open
import app.undermat.ui.flat.Spot
import app.undermat.ui.suitcase.SuitcaseScreen
import app.undermat.ui.suitcase.WeeksScreen
import app.undermat.ui.theme.C
import app.undermat.ui.theme.UndermatTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.ZonedDateTime

/** The main screens over the demo state, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    private val app get() = ApplicationProvider.getApplicationContext<UndermatApp>()

    private fun save(): Save = runBlocking {
        val c = app.container
        DemoSeed.seedIfEmpty(app, c)
        c.store.reload()
        c.store.load()
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { UndermatTheme { Box(Modifier.fillMaxSize().background(C.paper)) { content() } } }
        rule.mainClock.advanceTimeBy(3_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun flat(name: String, id: String, hour: Int, minute: Int = 10, sky: Sky = Sky.CLEAR, open: Open? = null, day: Int? = null) {
        val s = save()
        val c = app.container
        val look = Looks.of(id)
        c.setWeather(id, WeatherNow(sky, 17.0, 8.0, System.currentTimeMillis(), look.lat, look.lon))
        val host = c.host(id)!!
        val stay = if (s.stay?.host == id) s.stay!! else app.container.store.state.value!!.past.firstOrNull { it.host == id } ?: s.stay!!.copy(host = id)
        val clock = ZonedDateTime.now(look.zone).withMonth(10).withDayOfMonth(30).withHour(hour).withMinute(minute)
        shot(name) {
            FlatScreen(host, look, stay, live = stay.left == null, today = c.today(), suitcase = s.stickers.size, {}, {}, {}, {}, clock = clock, first = open, firstDay = day)
        }
    }

    @Test fun board() = save().let { s -> shot("01-board") { BoardScreen(app.container.hosts, s, app.container.today(), {}, {}, {}, {}, {}) } }
    @Test fun door() = save().let { shot("02-door") { DoorScreen(app.container.host("lisbon")!!, Looks.of("lisbon"), lifted0 = true, onEnter = {}, onBack = {}) } }
    @Test fun naples() = flat("03-naples", "naples", 13)
    @Test fun naplesKitchen() = flat("04-naples-kitchen", "naples", 19, open = Open.Note(Spot.KITCHEN))
    @Test fun naplesFridge() = flat("05-naples-fridge", "naples", 11, open = Open.Note(Spot.FRIDGE))
    @Test fun naplesGuest() = flat("06-naples-guest", "naples", 17, day = 4)
    @Test fun evening() = flat("07-evening", "naples", 20, open = Open.Evening)
    @Test fun suitcase() = save().let { s -> shot("08-suitcase") { SuitcaseScreen(s, app.container.hosts, {}) } }
    @Test fun weeks() = save().let { s -> shot("09-weeks") { WeeksScreen(s, app.container.hosts, {}, {}) } }
    @Test fun kyoto() = flat("10-kyoto", "kyoto", 21, sky = Sky.CLEAR)
    @Test fun oaxaca() = flat("11-oaxaca", "oaxaca", 9)
    @Test fun bergen() = flat("12-bergen", "bergen", 15, sky = Sky.RAIN)
    @Test fun seoul() = flat("13-seoul", "seoul", 18, minute = 0, sky = Sky.CLOUDY)
    @Test fun lyon() = flat("14-lyon", "lyon", 12, sky = Sky.OVERCAST)
    @Test fun tbilisi() = flat("15-tbilisi", "tbilisi", 16)
    @Test fun kolkata() = flat("16-kolkata", "kolkata", 10)
    @Test fun salvador() = flat("17-salvador", "salvador", 14)
    @Test fun lisbon() = flat("18-lisbon", "lisbon", 17, minute = 30)
    @Test fun galway() = flat("19-galway", "galway", 11, sky = Sky.DRIZZLE)
    @Test fun istanbul() = flat("20-istanbul", "istanbul", 22)
    @Test fun window() = flat("21-window", "bergen", 15, sky = Sky.RAIN, open = Open.Note(Spot.WINDOW))
    @Test fun calendar() = flat("22-calendar", "oaxaca", 9, open = Open.Note(Spot.CALENDAR))
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderArtScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() { override val lang = "ru" }
