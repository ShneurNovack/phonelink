package com.phonelink.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phonelink.app.util.Prefs

/** Restarts the link after a reboot if the owner left it on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED &&
            Prefs.enabled(context) && Prefs.isConfigured(context)) {
            PhoneLinkService.start(context)
        }
    }
}
