package com.phonelink.app.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.phonelink.app.PhoneLinkService
import com.phonelink.app.R
import com.phonelink.app.util.ActionLog
import com.phonelink.app.util.Prefs

class MainActivity : AppCompatActivity() {

    private val logListener = { runOnUiThread { refreshLog() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val url = findViewById<TextInputEditText>(R.id.urlInput)
        val key = findViewById<TextInputEditText>(R.id.keyInput)
        url.setText(Prefs.wsUrl(this))
        key.setText(Prefs.deviceKey(this))

        val sw = findViewById<MaterialSwitch>(R.id.enabledSwitch)
        sw.isChecked = Prefs.enabled(this)
        sw.setOnCheckedChangeListener { _, checked ->
            Prefs.setEnabled(this, checked)
            if (checked) PhoneLinkService.start(this) else PhoneLinkService.stop(this)
            refreshStatus()
        }

        findViewById<android.view.View>(R.id.saveButton).setOnClickListener {
            Prefs.setWsUrl(this, url.text?.toString() ?: "")
            Prefs.setDeviceKey(this, key.text?.toString() ?: "")
            if (Prefs.enabled(this)) {
                PhoneLinkService.stop(this)
                PhoneLinkService.start(this)
            }
            refreshStatus()
        }

        findViewById<android.view.View>(R.id.permOverlay).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        findViewById<android.view.View>(R.id.permBattery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        findViewById<android.view.View>(R.id.permDnd).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        findViewById<android.view.View>(R.id.permNotif).setOnClickListener {
            val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(i)
        }
        findViewById<TextView>(R.id.clearLog).setOnClickListener {
            ActionLog.clear(this); refreshLog()
        }
    }

    override fun onResume() {
        super.onResume()
        ActionLog.addListener(logListener)
        refreshStatus(); refreshPerms(); refreshLog()
    }

    override fun onPause() {
        super.onPause()
        ActionLog.removeListener(logListener)
    }

    private fun refreshStatus() {
        val s = when {
            !Prefs.isConfigured(this) -> "Not paired yet. Enter the Worker URL and device key below."
            !Prefs.enabled(this) -> "Turned off. Flip the switch to let Rory connect."
            else -> "On. Phone Link keeps a live link to Rory in the background."
        }
        findViewById<TextView>(R.id.statusLine).text = s
    }

    private fun refreshPerms() {
        val overlay = Settings.canDrawOverlays(this)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dnd = nm.isNotificationPolicyAccessGranted
        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val battery = pm.isIgnoringBatteryOptimizations(packageName)
        fun mark(b: Boolean) = if (b) "granted" else "needed"
        findViewById<TextView>(R.id.permStatus).text =
            "Appear on top: ${mark(overlay)}\n" +
            "Do Not Disturb control: ${mark(dnd)}\n" +
            "Ignore battery optimization: ${mark(battery)}"
    }

    private fun refreshLog() {
        val entries = ActionLog.recent(this)
        val tv = findViewById<TextView>(R.id.logView)
        tv.text = if (entries.isEmpty()) "No activity yet"
        else entries.joinToString("\n") {
            "${ActionLog.fmt(it.time)}  ${if (it.ok) "OK " else "ERR"}  ${it.action}"
        }
    }
}
