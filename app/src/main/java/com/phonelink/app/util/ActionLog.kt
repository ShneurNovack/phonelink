package com.phonelink.app.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A small persisted ring buffer of everything Rory asked the phone to do, so the
 * owner can see it in the app. Newest first, capped.
 */
object ActionLog {
    private const val FILE = "phonelink_log"
    private const val KEY = "entries"
    private const val MAX = 200
    private val listeners = mutableListOf<() -> Unit>()

    data class Entry(val time: Long, val action: String, val detail: String, val ok: Boolean)

    fun add(ctx: Context, action: String, detail: String, ok: Boolean) {
        val sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val arr = JSONArray(sp.getString(KEY, "[]"))
        val o = JSONObject()
            .put("t", System.currentTimeMillis())
            .put("a", action)
            .put("d", detail)
            .put("ok", ok)
        val out = JSONArray().put(o)
        for (i in 0 until minOf(arr.length(), MAX - 1)) out.put(arr.get(i))
        sp.edit().putString(KEY, out.toString()).apply()
        synchronized(listeners) { listeners.toList() }.forEach { it() }
    }

    fun recent(ctx: Context): List<Entry> {
        val sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val arr = JSONArray(sp.getString(KEY, "[]"))
        val list = ArrayList<Entry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Entry(o.getLong("t"), o.getString("a"), o.optString("d"), o.optBoolean("ok", true)))
        }
        return list
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove(KEY).apply()
        synchronized(listeners) { listeners.toList() }.forEach { it() }
    }

    fun addListener(l: () -> Unit) { synchronized(listeners) { listeners.add(l) } }
    fun removeListener(l: () -> Unit) { synchronized(listeners) { listeners.remove(l) } }

    fun fmt(t: Long): String =
        SimpleDateFormat("MMM d, h:mm:ss a", Locale.US).format(Date(t))
}
