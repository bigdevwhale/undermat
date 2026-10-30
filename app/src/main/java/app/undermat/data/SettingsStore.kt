package app.undermat.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** A quiet note in the evening when a new day's letters are in the flat. */
    val notify: Boolean = true,
    /** The guest's hour for that note. */
    val hour: Int = 19,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        notify = this[NOTIFY] ?: true,
        hour = this[HOUR] ?: 19,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[NOTIFY] = s.notify
            it[HOUR] = s.hour
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NOTIFY = booleanPreferencesKey("notify")
        val HOUR = intPreferencesKey("hour")
    }
}
