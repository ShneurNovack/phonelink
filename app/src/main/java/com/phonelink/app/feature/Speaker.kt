package com.phonelink.app.feature

import android.content.Context
import android.speech.tts.TextToSpeech
import org.json.JSONObject
import java.util.Locale

/** Text-to-speech so Rory can talk out loud on the phone. */
class Speaker(ctx: Context) {
    @Volatile private var ready = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts.language = Locale.US
        }
    }

    fun speak(text: String): JSONObject {
        if (!ready) return JSONObject().put("error", "tts not ready")
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "phonelink")
        return JSONObject().put("spoken", text)
    }

    fun shutdown() = runCatching { tts.shutdown() }
}
