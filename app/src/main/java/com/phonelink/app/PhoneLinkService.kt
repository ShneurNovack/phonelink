package com.phonelink.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.phonelink.app.feature.Speaker
import com.phonelink.app.feature.alarm.AlarmScheduler
import com.phonelink.app.feature.sensors.ActivityTracker
import com.phonelink.app.net.CommandDispatcher
import com.phonelink.app.net.WsClient
import com.phonelink.app.ui.MainActivity
import com.phonelink.app.util.Prefs
import java.util.concurrent.Executors
import org.json.JSONObject

/** Foreground service that holds the live link to the Worker and runs Rory's commands. */
class PhoneLinkService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private val work = Executors.newSingleThreadExecutor()
    private lateinit var speaker: Speaker
    private lateinit var dispatcher: CommandDispatcher
    private var ws: WsClient? = null
    @Volatile private var connected = false

    companion object {
        private const val CHANNEL = "phonelink_status"
        private const val NOTIF_ID = 1
        const val ACTION_RESTART = "com.phonelink.app.RESTART"

        @Volatile private var instance: PhoneLinkService? = null

        fun start(ctx: Context) {
            val i = Intent(ctx, PhoneLinkService::class.java)
            ContextCompatStartForeground(ctx, i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, PhoneLinkService::class.java))
        }

        /** Called by PopupActivity when the owner taps an answer button. */
        fun answerPrompt(promptId: String, value: String) {
            instance?.sendOut(JSONObject()
                .put("type", "promptAnswer")
                .put("promptId", promptId)
                .put("value", value))
        }

        private fun ContextCompatStartForeground(ctx: Context, i: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        speaker = Speaker(this)
        dispatcher = CommandDispatcher(this, speaker)
        createChannel()
        startForegroundSafely("Starting...")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Prefs.enabled(this) && Prefs.isConfigured(this)) {
            AlarmScheduler.rescheduleAll(this)
            runCatching { ActivityTracker.start(this) }
            KeepAlive.schedule(this)
            ensureConnected()
        } else {
            KeepAlive.cancel(this)
            updateNotification(if (!Prefs.isConfigured(this)) "Not paired yet" else "Turned off")
        }
        return START_STICKY
    }

    /** User swiped the app out of recents: keep the link alive by relaunching the service. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (Prefs.enabled(this) && Prefs.isConfigured(this)) {
            val restart = Intent(this, PhoneLinkService::class.java)
            val pi = PendingIntent.getForegroundService(
                this, 1, restart,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            am.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 1000, pi)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun ensureConnected() {
        if (ws != null) return
        val base = Prefs.wsUrl(this).trimEnd('/')
        val key = Prefs.deviceKey(this)
        val name = android.net.Uri.encode("${Build.MANUFACTURER} ${Build.MODEL}")
        val url = "$base/device?key=${android.net.Uri.encode(key)}&name=$name"
        updateNotification("Connecting...")
        ws = WsClient(
            url = url,
            onOpen = { connected = true; main.post { updateNotification("Connected to Rory") } },
            onClosed = { connected = false; main.post { updateNotification("Reconnecting...") } },
            onText = { msg -> work.execute { onCommand(msg) } }
        ).also { it.start() }
    }

    private fun onCommand(msg: JSONObject) {
        val id = msg.optString("id", "")
        val cmd = msg.optString("cmd", "")
        if (cmd.isEmpty()) return
        val params = msg.optJSONObject("params") ?: JSONObject()
        try {
            val result = dispatcher.handle(cmd, params)
            sendOut(JSONObject().put("id", id).put("ok", true).put("result", result))
        } catch (e: Exception) {
            sendOut(JSONObject().put("id", id).put("ok", false).put("error", e.message ?: "error"))
        }
    }

    private fun sendOut(obj: JSONObject) {
        ws?.send(obj)
    }

    override fun onDestroy() {
        ws?.stop(); ws = null
        speaker.shutdown()
        instance = null
        super.onDestroy()
    }

    // --- notification ---

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL, "Phone Link status", NotificationManager.IMPORTANCE_LOW)
        ch.setShowBadge(false)
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("Phone Link")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_link)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    private fun startForegroundSafely(text: String) {
        val n = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }
}
