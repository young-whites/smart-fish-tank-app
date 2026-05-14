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

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errorMessage: SharedFlow<String> = _errorMessage

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
                sock.keepAlive = true // Enable TCP keepalive for stability
                sock.soTimeout = 0
                // Increase receive buffer for better throughput
                sock.receiveBufferSize = 8192
                socket = sock
                inputStream = sock.getInputStream()
                outputStream = sock.getOutputStream()
                reconnectAttempt = 0
                Log.d(TAG, "TCP connected to $savedHost:$savedPort")
                Log.d(TAG, "Socket isConnected=${sock.isConnected} isClosed=${sock.isClosed}")
                _connectionState.emit(true)
                receiveLoop()
            } catch (e: Exception) {
                Log.e(TAG, "TCP connection failed: ${e.javaClass.simpleName}: ${e.message}")
                _errorMessage.emit("TCP连接失败: ${e.message}")
                _connectionState.emit(false)
                cleanup()
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
        scope?.launch(Dispatchers.IO) {
            try {
                if (outputStream == null) {
                    Log.e(TAG, "Send failed: outputStream is null (not connected)")
                    _errorMessage.emit("Not connected")
                    return@launch
                }
                outputStream?.write(frame)
                outputStream?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Send failed: ${e.javaClass.simpleName}: ${e.message}")
                _errorMessage.emit("Send failed: ${e.message ?: "connection lost"}")
            }
        }
    }

    private suspend fun receiveLoop() {
        val buffer = ByteArray(4096)
        var leftover = ByteArray(0)

        Log.d(TAG, "Receive loop started")
        try {
            while (currentCoroutineContext().isActive) {
                val bytesRead = try {
                    inputStream?.read(buffer) ?: -1
                } catch (e: java.net.SocketTimeoutException) {
                    Log.d(TAG, "Read timeout, continuing")
                    continue
                } catch (e: Exception) {
                    Log.e(TAG, "Read exception: ${e.javaClass.simpleName}: ${e.message}")
                    break
                }

                if (bytesRead < 0) {
                    Log.d(TAG, "Server closed connection (EOF)")
                    break
                }

                if (bytesRead > 0) {
                    Log.d(TAG, "Received $bytesRead bytes")
                    val combined = leftover + buffer.copyOf(bytesRead)
                    leftover = processBuffer(combined)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Receive loop outer exception: ${e.message}")
        }
        Log.d(TAG, "Receive loop ended, emitting disconnected")
        _connectionState.emit(false)
        cleanup()
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
