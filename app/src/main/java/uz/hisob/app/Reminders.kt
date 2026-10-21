package uz.hisob.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

object Reminders {
    const val CHANNEL = "payments"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Payment reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminders the day before and on the day a credit or bill is due"
                }
            )
        }
    }

    /** Schedule the next daily check at the reminder hour. */
    fun schedule(ctx: Context) {
        val hour = Store.load(ctx).remindHour
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    fun check(ctx: Context) {
        val d = Store.load(ctx)
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val lines = mutableListOf<String>()
        var total = 0L
        fun scan(day: LocalDate, label: String) {
            val ym = YearMonth.from(day)
            d.monthItems(ym).forEach { it ->
                val st = d.status(it, ym, today)
                if (st.due == day && st.st != St.PAID) {
                    val how = if (it.auto) "bank takes it" else "pay it"
                    lines += "$label: ${it.title()} ${fmt(it.amount)} (${how})"
                    total += it.amount
                }
            }
        }
        scan(today, "Today")
        scan(tomorrow, "Tomorrow")
        if (lines.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (lines.size == 1) lines[0].substringBefore(" (") else "${lines.size} payments coming up, ${fmt(total)} ${d.currency}"
        val style = NotificationCompat.InboxStyle()
        lines.forEach { style.addLine(it) }
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Make sure your card has enough money.")
            .setStyle(style)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(7, n) } catch (_: SecurityException) { }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Reminders.check(context)
        Reminders.schedule(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) Reminders.schedule(context)
    }
}
