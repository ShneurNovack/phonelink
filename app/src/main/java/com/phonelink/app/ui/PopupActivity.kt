package com.phonelink.app.ui

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.phonelink.app.PhoneLinkService
import com.phonelink.app.R
import org.json.JSONArray

/** Full-screen-over-lock message from Rory, optionally with answer buttons. */
class PopupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popup)

        val message = intent.getStringExtra("message") ?: ""
        val promptId = intent.getStringExtra("promptId")
        val buttonsJson = intent.getStringExtra("buttons")

        findViewById<android.widget.TextView>(R.id.popupMessage).text = message
        val row = findViewById<LinearLayout>(R.id.popupButtons)

        val buttons = parseButtons(buttonsJson)
        if (buttons.isEmpty()) {
            // Plain notice: single OK button that just dismisses.
            addButton(row, "OK", null, promptId)
        } else {
            for ((label, value) in buttons) addButton(row, label, value, promptId)
        }
    }

    private fun parseButtons(json: String?): List<Pair<String, String>> {
        if (json.isNullOrBlank()) return emptyList()
        val out = ArrayList<Pair<String, String>>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(o.getString("label") to o.optString("value", o.getString("label")))
        }
        return out
    }

    private fun addButton(row: LinearLayout, label: String, value: String?, promptId: String?) {
        val b = MaterialButton(this)
        b.text = label
        b.textSize = 16f
        b.isAllCaps = false
        b.cornerRadius = 0
        b.elevation = 0f
        b.stateListAnimator = null
        b.insetTop = 0
        b.insetBottom = 0
        b.setOnClickListener {
            if (promptId != null && value != null) {
                PhoneLinkService.answerPrompt(promptId, value)
            }
            finish()
        }
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        )
        if (row.childCount > 0) lp.topMargin = dp(10)
        row.addView(b, lp)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
