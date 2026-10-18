package com.phonelink.app.net

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps a WebSocket open to the Worker. Reconnects with backoff.
 * This is the only channel Rory uses to reach the phone - no ADB involved.
 */
class WsClient(
    private val url: String,
    private val onOpen: () -> Unit,
    private val onClosed: () -> Unit,
    private val onText: (JSONObject) -> Unit
) {
    private val tag = "PhoneLinkWs"
    private val client = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Volatile private var ws: WebSocket? = null
    private val running = AtomicBoolean(false)
    private var backoffMs = 1000L

    fun start() {
        if (running.getAndSet(true)) return
        connect()
    }

    fun stop() {
        running.set(false)
        ws?.close(1000, "stopped")
        ws = null
    }

    fun send(obj: JSONObject): Boolean = ws?.send(obj.toString()) ?: false

    private fun connect() {
        if (!running.get()) return
        val req = Request.Builder().url(url).build()
        ws = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "open")
                backoffMs = 1000L
                onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try { onText(JSONObject(text)) } catch (e: Exception) { Log.w(tag, "bad msg", e) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onClosed()
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "failure: ${t.message}")
                onClosed()
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!running.get()) return
        val delay = backoffMs
        backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
        Thread {
            try { Thread.sleep(delay) } catch (_: InterruptedException) {}
            if (running.get()) connect()
        }.start()
    }
}
