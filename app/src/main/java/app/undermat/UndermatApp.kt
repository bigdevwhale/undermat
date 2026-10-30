package app.undermat

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.undermat.data.DemoSeed
import app.undermat.data.Host
import app.undermat.data.Looks
import app.undermat.data.SettingsStore
import app.undermat.data.Store
import app.undermat.domain.Weather
import app.undermat.domain.WeatherNow
import app.undermat.work.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class AppContainer(context: Context) {
    val app = context.applicationContext
    val settings = SettingsStore(app)
    val store = Store(File(app.filesDir, "stays.json"))
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Every flat on the board, in board order. */
    val hosts: List<Host> by lazy {
        Looks.order.mapNotNull { id -> runCatching { app.assets.open("hosts/$id.json").use { Host.parse(it) } }.getOrNull() }
    }

    fun host(id: String): Host? = hosts.firstOrNull { it.id == id }

    private val _weather = MutableStateFlow<Map<String, WeatherNow>>(emptyMap())
    /** The weather outside each host's window, as last fetched this session. */
    val weather: StateFlow<Map<String, WeatherNow>> = _weather

    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    suspend fun refreshWeather(id: String) {
        val w = _weather.value[id]
        if (w != null && System.currentTimeMillis() - w.at < Weather.FRESH_MS) return
        val look = Looks.all[id] ?: return
        val now = runCatching { withContext(Dispatchers.IO) { Weather.fetch(look.lat, look.lon) } }.getOrNull() ?: return
        _weather.value = _weather.value + (id to now)
    }

    /** Only for screenshots: a sky that doesn't depend on the network. */
    fun setWeather(id: String, w: WeatherNow) {
        _weather.value = _weather.value + (id to w)
    }

    fun today(): Long = LocalDate.now().toEpochDay()
}

class UndermatApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@UndermatApp, container)
            container.store.load()
        }
        runCatching { Reminders.schedule(this) }
    }
}

val Context.container: AppContainer get() = (applicationContext as UndermatApp).container

/** Resources in the in-app language, which AppCompat doesn't localise outside activities before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
