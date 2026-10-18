package com.phonelink.app.feature

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import org.json.JSONObject

/** Volume control (fully supported) and best-effort media transport keys. */
object MediaVolume {

    private fun stream(name: String): Int = when (name.lowercase()) {
        "ring" -> AudioManager.STREAM_RING
        "alarm" -> AudioManager.STREAM_ALARM
        "call" -> AudioManager.STREAM_VOICE_CALL
        "notification" -> AudioManager.STREAM_NOTIFICATION
        else -> AudioManager.STREAM_MUSIC
    }

    /** level is 0..100 */
    fun setVolume(ctx: Context, streamName: String, level: Int): JSONObject {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val s = stream(streamName)
        val max = am.getStreamMaxVolume(s)
        val target = (level.coerceIn(0, 100) * max + 50) / 100
        am.setStreamVolume(s, target, AudioManager.FLAG_SHOW_UI)
        return JSONObject().put("stream", streamName).put("level", level)
    }

    fun mediaControl(ctx: Context, action: String): JSONObject {
        val code = when (action.lowercase()) {
            "play", "pause", "playpause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "prev", "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> return JSONObject().put("error", "unknown media action: $action")
        }
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        return JSONObject().put("media", action)
    }
}
