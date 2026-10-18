package com.phonelink.app.feature

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import org.json.JSONObject

/** Find-my-phone style actions: ring loudly, flashlight, vibrate. */
object FindPhone {

    @Volatile private var player: MediaPlayer? = null

    /** Rings at full alarm volume for [seconds] even if the phone is on silent, plus vibrate. */
    fun ring(ctx: Context, seconds: Int): JSONObject {
        stopRing()
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
        val uri = RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val mp = MediaPlayer()
        mp.setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        mp.setDataSource(ctx, uri)
        mp.isLooping = true
        mp.prepare()
        mp.start()
        player = mp
        vibrate(ctx, 1500)
        val secs = seconds.coerceIn(1, 120)
        android.os.Handler(ctx.mainLooper).postDelayed({ stopRing() }, secs * 1000L)
        return JSONObject().put("ringing", true).put("seconds", secs)
    }

    fun stopRing(): JSONObject {
        player?.let { runCatching { it.stop(); it.release() } }
        player = null
        return JSONObject().put("ringing", false)
    }

    fun flashlight(ctx: Context, on: Boolean): JSONObject {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull { cid ->
            cm.getCameraCharacteristics(cid)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return JSONObject().put("error", "no flash on this device")
        cm.setTorchMode(id, on)
        return JSONObject().put("flashlight", on)
    }

    fun vibrate(ctx: Context, millis: Long): JSONObject {
        val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createOneShot(millis.coerceIn(100, 5000), VibrationEffect.DEFAULT_AMPLITUDE))
        return JSONObject().put("vibrated", true)
    }
}
