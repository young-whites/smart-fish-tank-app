package com.example.intelligentfish.data.net

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import android.util.Log
import java.net.Socket

class TcpClient {

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var receiveJob: Job? = null
    private var scope: CoroutineScope? = null

    // 自动重连相关
    private var savedHost: String = ""
    private var savedPort: Int = 8080
    private var reconnectAttempt: Int = 0
    private val maxReconnectAttempts = 5
    private var manualDisconnect = false

    companion object {
        private const val TAG = "TcpClient"
    }

    private val _incomingFrames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val incomingFrames: SharedFlow<ByteArray> = _incomingFrames

    private val _connectionState = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState

    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    fun connect(host: String, port: Int = 8080, coroutineScope: CoroutineScope) {
        disconnect()
        scope = coroutineScope
        savedHost = host
        savedPort = port
        reconnectAttempt = 0
        manualDisconnect = false
        doConnect(coroutineScope)
    }

    private fun doConnect(coroutineScope: CoroutineScope) {
        receiveJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                val sock = Socket()
                sock.connect(InetSocketAddress(savedHost, savedPort), 5000)
                sock.tcpNoDelay = true
                sock.keepAlive = false
                sock.soTimeout = 0  // No read timeout - block indefinitely
                socket = sock
                inputStream = sock.getInputStream()
                outputStream = sock.getOutputStream()
                reconnectAttempt = 0 // 连接成功，重置重试计数
                Log.d(TAG, "TCP connected to $savedHost:$savedPort")
                _connectionState.emit(true)
                receiveLoop()
            } catch (e: Exception) {
                Log.e(TAG, "TCP connection failed: ${e.message}")
                Log.d(TAG, "TCP receive loop ended")
            _connectionState.emit(false)
                cleanup()
                // 连接失败，尝试自动重连
                if (!manualDisconnect) {
                    scheduleReconnect()
                }
            }
        }
    }

    /**
     * 指数退避自动重连：最多5次，间隔 2s/4s/8s/16s/32s
     */
    private fun scheduleReconnect() {
        if (reconnectAttempt >= maxReconnectAttempts) {
            reconnectAttempt = 0
            return
        }
        val delayMs = 2000L * (1 shl reconnectAttempt) // 2, 4, 8, 16, 32
        reconnectAttempt++
        scope?.launch(Dispatchers.IO) {
            delay(delayMs)
            if (!manualDisconnect && scope != null) {
                doConnect(scope!!)
            }
        }
    }

    fun disconnect() {
        manualDisconnect = true
        receiveJob?.cancel()
        receiveJob = null
        cleanup()
        scope?.launch { _connectionState.emit(false) }
    }

    fun sendFrame(frame: ByteArray) {
        try {
            outputStream?.write(frame)
            outputStream?.flush()
        } catch (_: Exception) {
            disconnect()
        }
    }

    private suspend fun receiveLoop() {
        val buffer = ByteArray(4096)
        var leftover = ByteArray(0)

        while (currentCoroutineContext().isActive && isConnected) {
            try {
                val bytesRead = inputStream?.read(buffer) ?: -1
                if (bytesRead < 0) break

                // 合并上次剩余数据
                val combined = leftover + buffer.copyOf(bytesRead)
                leftover = processBuffer(combined)
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) break
            }
        }
        _connectionState.emit(false)
        cleanup()
        // 连接断开后尝试自动重连
        if (!manualDisconnect) {
            scheduleReconnect()
        }
    }

    private suspend fun processBuffer(buffer: ByteArray): ByteArray {
        var remaining = buffer
        while (true) {
            val result = Protocol.extractFrame(remaining) ?: break
            _incomingFrames.emit(result.first)
            remaining = result.second
        }
        return remaining
    }

    private fun cleanup() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        socket = null
    }
}
