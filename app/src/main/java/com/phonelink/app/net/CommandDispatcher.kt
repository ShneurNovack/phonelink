package com.phonelink.app.net

import android.content.Context
import android.content.Intent
import com.phonelink.app.feature.AlarmsTimers
import com.phonelink.app.feature.DeviceState
import com.phonelink.app.feature.FindPhone
import com.phonelink.app.feature.MediaVolume
import com.phonelink.app.feature.Speaker
import com.phonelink.app.ui.PopupActivity
import com.phonelink.app.util.ActionLog
import org.json.JSONObject

/** Routes an incoming command to the right feature and returns a result JSON. */
class CommandDispatcher(
    private val ctx: Context,
    private val speaker: Speaker
) {
    /** Returns the result payload, or throws with a message to send as an error. */
    fun handle(cmd: String, p: JSONObject): JSONObject {
        val result = when (cmd) {
            "ping" -> JSONObject().put("pong", true)
            "get_state", "status" -> DeviceState.collect(ctx)
            "ring_phone" -> FindPhone.ring(ctx, p.optInt("seconds", 15))
            "stop_ring" -> FindPhone.stopRing()
            "flashlight" -> FindPhone.flashlight(ctx, p.optBoolean("on", true))
            "vibrate" -> FindPhone.vibrate(ctx, p.optLong("ms", 800))
            "set_volume" -> MediaVolume.setVolume(ctx, p.optString("stream", "media"), p.optInt("level", 50))
            "media_control" -> MediaVolume.mediaControl(ctx, p.optString("action", "toggle"))
            "set_alarm" -> AlarmsTimers.setAlarm(ctx, p.getInt("hour"), p.getInt("minute"), p.optString("label"))
            "set_timer" -> AlarmsTimers.setTimer(ctx, p.getInt("seconds"), p.optString("label"))
            "open_app" -> AlarmsTimers.openApp(ctx, p.getString("package"))
            "open_url" -> AlarmsTimers.openUrl(ctx, p.getString("url"))
            "speak" -> speaker.speak(p.getString("text"))
            "show_popup" -> showPopup(p)
            else -> JSONObject().put("error", "unknown command: $cmd")
        }
        val ok = !result.has("error")
        ActionLog.add(ctx, cmd, summarize(p), ok)
        return result
    }

    private fun showPopup(p: JSONObject): JSONObject {
        val i = Intent(ctx, PopupActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("message", p.optString("message"))
            if (p.has("promptId")) putExtra("promptId", p.getString("promptId"))
            if (p.has("buttons")) putExtra("buttons", p.getJSONArray("buttons").toString())
        }
        ctx.startActivity(i)
        return JSONObject().put("shown", true)
    }

    private fun summarize(p: JSONObject): String {
        val s = p.toString()
        return if (s.length > 120) s.substring(0, 117) + "..." else s
    }
}
