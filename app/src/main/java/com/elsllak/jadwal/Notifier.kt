package com.elsllak.jadwal

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZoneId

/** Schedules reminders with exact alarms and shows them as notifications. */
object Notifier {
    const val CHANNEL = "reminders"
    private const val MAX = 400

    fun channel(c: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = c.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(CHANNEL, L.t("التنبيهات", "Reminders"), NotificationManager.IMPORTANCE_HIGH)
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
    }

    private fun pi(c: Context, i: Int, title: String?, body: String?, create: Boolean): PendingIntent? {
        val it = Intent(c, AlarmReceiver::class.java).setAction("jadwal.alarm.$i")
        if (title != null) { it.putExtra("t", title); it.putExtra("b", body ?: "") }
        val flags = PendingIntent.FLAG_IMMUTABLE or (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(c, i, it, flags)
    }

    fun canExact(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /** Cancels everything we scheduled, then schedules the current plan. */
    @Synchronized
    fun reschedule(c: Context) {
        val sp = c.getSharedPreferences("jadwal_alarms", Context.MODE_PRIVATE)
        val am = c.getSystemService(AlarmManager::class.java)
        val old = sp.getInt("n", 0)
        for (i in 0 until old) pi(c, i, null, null, false)?.let { am.cancel(it); it.cancel() }
        channel(c)
        val plan = Plan.build(G.s, G.p, System.currentTimeMillis(), ZoneId.systemDefault())
        val n = minOf(plan.size, MAX)
        val exact = canExact(c)
        for (i in 0 until n) {
            val a = plan[i]
            val p = pi(c, i, a.title, a.body, true)!!
            try {
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, a.at, p)
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, a.at, p)
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, a.at, p)
            }
        }
        sp.edit().putInt("n", n).apply()
    }

    fun show(c: Context, title: String, body: String, id: Int = (System.currentTimeMillis() % 100000).toInt()) {
        channel(c)
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(c, CHANNEL) else Notification.Builder(c)
        b.setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(body.lineSequence().firstOrNull() ?: "")
            .setStyle(Notification.BigTextStyle().bigText(body)).setContentIntent(open).setAutoCancel(true)
            .setColor(0xFF0E7C70.toInt())
        try { c.getSystemService(NotificationManager::class.java).notify(id, b.build()) } catch (e: SecurityException) {}
    }

    /** A test reminder 5 seconds from now. */
    fun test(c: Context) {
        channel(c)
        val am = c.getSystemService(AlarmManager::class.java)
        val p = pi(c, 9999, L.t("تنبيه تجريبي", "Test reminder"), L.t("التنبيهات تعمل بشكل سليم.", "Reminders are working."), true)!!
        val at = System.currentTimeMillis() + 5000
        try {
            if (canExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        } catch (e: SecurityException) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p) }
    }

    fun loadAndReschedule(c: Context) {
        Store.load(c)
        reschedule(c)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val t = i.getStringExtra("t") ?: return
        val pending = goAsync()
        try {
            Notifier.show(c, t, i.getStringExtra("b") ?: "")
            // lectures repeat weekly, so plan the next occurrences
            Notifier.loadAndReschedule(c)
        } finally { pending.finish() }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val pending = goAsync()
        try { Notifier.loadAndReschedule(c) } finally { pending.finish() }
    }
}
