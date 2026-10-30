package app.undermat.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.undermat.MainActivity
import app.undermat.R
import app.undermat.container
import app.undermat.data.Looks
import app.undermat.localized
import app.undermat.ru
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Once an hour: is there a new day's letter in the flat, and is it the guest's hour to hear about it? */
class LetterWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Reminders.check(applicationContext)
        return Result.success()
    }
}

object Reminders {
    private const val CHANNEL = "letters"

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<LetterWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("letters", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    private fun allowed(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** The app is open: today's letters are seen, nothing to tell about them later. */
    suspend fun seen(context: Context) {
        val c = context.container
        val st = c.store.load().stay ?: return
        val day = st.day(c.today())
        if (st.entered && st.told < day) c.store.stay { it.copy(told = day) }
    }

    suspend fun check(context: Context) {
        val c = context.container
        val set = c.settings.now()
        val st = c.store.load().stay ?: return
        if (!st.entered) return
        val day = st.day(c.today())
        if (day <= st.told) return
        if (!set.notify || LocalTime.now().hour < set.hour || !allowed(context)) return
        val host = c.host(st.host) ?: return
        val r = context.localized()
        val ru = r.ru
        show(context, r.getString(R.string.n_title, host.name.of(ru), day), r.getString(R.string.n_text, Looks.of(host.id).at.of(ru)))
        c.store.stay { it.copy(told = day) }
    }

    private fun show(context: Context, title: String, text: String) {
        val m = context.getSystemService(NotificationManager::class.java)
        if (m.getNotificationChannel(CHANNEL) == null) {
            m.createNotificationChannel(NotificationChannel(CHANNEL, context.localized().getString(R.string.channel), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_note)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(1, n)
    }
}
