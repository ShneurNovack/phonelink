package com.phonelink.app.net

import android.content.Context
import android.content.Intent
import com.phonelink.app.feature.AlarmsTimers
import com.phonelink.app.feature.DeviceState
import com.phonelink.app.feature.FindPhone
import com.phonelink.app.feature.MediaVolume
import com.phonelink.app.feature.Speaker
import com.phonelink.app.feature.SystemOps
import com.phonelink.app.feature.access.ScreenControlService
import com.phonelink.app.feature.comms.CallBlock
import com.phonelink.app.feature.comms.CallsSms
import com.phonelink.app.feature.media.MediaFiles
import com.phonelink.app.feature.media.Screenshots
import com.phonelink.app.feature.notif.MediaSessions
import com.phonelink.app.feature.ocr.Ocr
import com.phonelink.app.feature.sys.SettingsControl
import com.phonelink.app.feature.sys.ShizukuShell
import com.phonelink.app.feature.notif.NotifListener
import com.phonelink.app.feature.sensors.ActivityTracker
import com.phonelink.app.feature.sensors.Sensors
import com.phonelink.app.ui.PopupActivity
import com.phonelink.app.util.ActionLog
import org.json.JSONObject

/** Routes an incoming command to the right feature and returns a result JSON. */
class CommandDispatcher(
    private val ctx: Context,
    private val speaker: Speaker
) {
    fun handle(cmd: String, p: JSONObject): JSONObject {
        val result = when (cmd) {
            // --- phase 1 ---
            "ping" -> JSONObject().put("pong", true)
            "get_state", "status" -> DeviceState.collect(ctx)
            "ring_phone" -> FindPhone.ring(ctx, p.optInt("seconds", 15))
            "stop_ring" -> FindPhone.stopRing()
            "flashlight" -> FindPhone.flashlight(ctx, p.optBoolean("on", true))
            "vibrate" -> FindPhone.vibrate(ctx, p.optLong("ms", 800))
            "set_volume" -> MediaVolume.setVolume(ctx, p.optString("stream", "media"), p.optInt("level", 50))
            "media_control" -> mediaControl(p.optString("action", "toggle"))
            "now_playing" -> MediaSessions.nowPlaying(ctx)
            "set_alarm", "alarm_add" -> AlarmsTimers.setAlarm(ctx, p.getInt("hour"), p.getInt("minute"), p.optString("label"), p.opt("days"), p.optBoolean("vibrate", true))
            "set_timer" -> AlarmsTimers.setTimer(ctx, p.getInt("seconds"), p.optString("label"))
            "show_alarms" -> AlarmsTimers.showAlarms(ctx)
            "open_app" -> AlarmsTimers.openApp(ctx, p.getString("package"))
            "open_url" -> AlarmsTimers.openUrl(ctx, p.getString("url"))
            "speak" -> speaker.speak(p.getString("text"))
            "show_popup" -> showPopup(p)

            // --- notifications ---
            "notif_list" -> NotifListener.list(p.optBoolean("includeOngoing", false))
            "notif_reply" -> NotifListener.reply(ctx, p.getString("key"), p.getString("text"))
            "notif_action" -> NotifListener.tapAction(ctx, p.getString("key"), p.getInt("index"))
            "notif_dismiss" -> NotifListener.dismiss(p.getString("key"))

            // --- calls + sms ---
            "call_log" -> CallsSms.callLog(ctx, p.optInt("limit", 20))
            "place_call" -> CallsSms.placeCall(ctx, p.getString("number"))
            "answer_call" -> CallsSms.answerCall(ctx)
            "end_call" -> CallsSms.endCall(ctx)
            "call_state" -> CallsSms.callState(ctx)
            "sms_recent" -> CallsSms.smsRecent(ctx, p.optInt("limit", 20))
            "sms_thread" -> CallsSms.smsThread(ctx, p.getString("address"), p.optInt("limit", 20))
            "send_sms" -> CallsSms.sendSms(ctx, p.getString("to"), p.getString("text"))

            // --- sensors ---
            "bluetooth" -> Sensors.bluetooth(ctx)
            "activity" -> ActivityTracker.current()
            "screen_time" -> Sensors.screenTime(ctx)
            "location" -> Sensors.location(ctx)

            // --- screen control (accessibility) ---
            "screenshot" -> ScreenControlService.screenshot(p.optInt("maxWidth", 900), p.optInt("quality", 55))
            "screen_dump" -> ScreenControlService.dump()
            "tap" -> ScreenControlService.tap(p.getInt("x"), p.getInt("y"))
            "long_press" -> ScreenControlService.longPress(p.getInt("x"), p.getInt("y"))
            "swipe" -> ScreenControlService.swipe(p.getInt("x1"), p.getInt("y1"), p.getInt("x2"), p.getInt("y2"), p.optLong("ms", 300))
            "scroll" -> ScreenControlService.scroll(p.optString("direction", "down"))
            "input_text" -> ScreenControlService.inputText(p.getString("text"))
            "tap_text" -> ScreenControlService.tapText(p.getString("text"))
            "global_action" -> ScreenControlService.global(p.getString("action"))

            // --- screenshots (auto-OCR log) ---
            "shots_list" -> Screenshots.list(ctx, p.optInt("limit", 20))
            "shots_read" -> Screenshots.read(ctx, p.optLong("id", 0))
            "shots_search" -> Screenshots.search(ctx, p.getString("query"))

            // --- media + files + ocr ---
            "media_list" -> MediaFiles.listMedia(ctx, p.optString("type", "images"), p.optInt("limit", 30), p.optString("query").ifBlank { null })
            "photo_view" -> MediaFiles.viewPhoto(ctx, p.getLong("id"), p.optInt("maxWidth", 900))
            "files_list" -> MediaFiles.listDir(p.optString("path").ifBlank { null })
            "ocr_image" -> JSONObject().put("text", Ocr.fromFile(p.getString("path")))

            // --- settings + shell (Shizuku) ---
            "setting_get" -> SettingsControl.get(ctx, p.optString("namespace", "secure"), p.getString("key"))
            "setting_put" -> SettingsControl.put(ctx, p.optString("namespace", "secure"), p.getString("key"), p.getString("value"))
            "toggle" -> SettingsControl.quickToggle(p.getString("name"), p.getBoolean("on"))
            "shell" -> ShizukuShell.exec(p.getString("command"))
            "shizuku_status" -> JSONObject().put("available", ShizukuShell.available()).put("granted", ShizukuShell.granted())

            // --- call screening ---
            "block_number" -> CallBlock.block(ctx, p.getString("number"))
            "unblock_number" -> CallBlock.unblock(ctx, p.getString("number"))
            "list_blocked" -> CallBlock.listBlocked(ctx)
            "screened_calls" -> CallBlock.recent(ctx)

            // --- system ---
            "list_apps" -> SystemOps.listApps(ctx, p.optBoolean("onlyLaunchable", true))
            "set_clipboard" -> SystemOps.setClipboard(ctx, p.getString("text"))
            "get_clipboard" -> SystemOps.getClipboard(ctx)
            "set_dnd" -> SystemOps.setDnd(ctx, p.getString("mode"))
            "set_ringer" -> SystemOps.setRinger(ctx, p.getString("mode"))

            else -> JSONObject().put("error", "unknown command: $cmd")
        }
        val ok = !result.has("error")
        // Keep screenshots out of the on-phone log preview (too big); log the action only.
        ActionLog.add(ctx, cmd, if (cmd == "screenshot") "captured" else summarize(p), ok)
        return result
    }

    private fun mediaControl(action: String): JSONObject {
        return if (NotifListener.isConnected()) MediaSessions.control(ctx, action)
        else MediaVolume.mediaControl(ctx, action)
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
