package com.example.intelligentfish.data.net

import com.example.intelligentfish.data.model.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 通信协议定义 — 对齐 STM32 bsp_esp01s.c
 *
 * 帧格式:
 * HEAD(0xAA) | CMD(1B) | LEN(1B) | PAYLOAD(NB) | SUM(1B) | END(0x55)
 *
 * 校验和 = CMD + LEN + 所有 payload 字节之和，取低8位
 */
object Protocol {

    const val HEADER: Byte = 0xAA.toByte()
    const val END: Byte = 0x55.toByte()

    // 命令字 — MCU → APP
    const val CMD_SENSOR_DATA: Byte = 0x01
    const val CMD_DEVICE_STATUS: Byte = 0x02
    const val CMD_ALARM_EVENT: Byte = 0x03

    // 命令字 — APP → MCU
    const val CMD_QUERY_STATUS: Byte = 0x10
    const val CMD_SWITCH_MODE: Byte = 0x11
    const val CMD_CONTROL_RELAY: Byte = 0x12
    const val CMD_SYNC_THRESHOLDS: Byte = 0x13
    const val CMD_TRIGGER_FEED: Byte = 0x14
    const val CMD_SET_ALARM: Byte = 0x15
    const val CMD_SET_FEED_INTERVAL: Byte = 0x16
    const val CMD_LED_CONTROL: Byte = 0x20

    // 最小帧: HEAD(1) + CMD(1) + LEN(1) + SUM(1) + END(1) = 5
    private const val MIN_FRAME_SIZE = 5

    /**
     * 从缓冲区中提取完整帧数据
     * @return Pair<提取的帧字节数组, 剩余缓冲区>，如果没有完整帧则返回 null
     */
    fun extractFrame(buffer: ByteArray): Pair<ByteArray, ByteArray>? {
        // 查找帧头 0xAA
        val headerIdx = buffer.indexOfFirst { it == HEADER.toInt().toByte() }
        if (headerIdx < 0) return null

        // 需要至少 HEAD(1) + CMD(1) + LEN(1) = 3 字节才能判断帧长
        if (buffer.size < headerIdx + 3) return null

        val payloadLen = buffer[headerIdx + 2].toInt() and 0xFF

        // 完整帧: HEAD(1) + CMD(1) + LEN(1) + PAYLOAD(N) + SUM(1) + END(1)
        val frameSize = 3 + payloadLen + 2
        if (buffer.size < headerIdx + frameSize) return null

        val frame = buffer.copyOfRange(headerIdx, headerIdx + frameSize)

        // 校验帧尾
        if (frame[frameSize - 1] != END) {
            // 帧尾不对，跳过这个帧头继续搜索
            val remaining = buffer.copyOfRange(headerIdx + 1, buffer.size)
            return extractFrame(remaining)
        }

        // 校验校验和: CMD + LEN + payload
        val expectedChecksum = frame[frameSize - 2]
        val calculatedChecksum = calculateChecksum(frame, 1, 2 + payloadLen)
        if (expectedChecksum != calculatedChecksum) {
            val remaining = buffer.copyOfRange(headerIdx + 1, buffer.size)
            return extractFrame(remaining)
        }

        val remaining = buffer.copyOfRange(headerIdx + frameSize, buffer.size)
        return Pair(frame, remaining)
    }

    private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Byte {
        var sum = 0
        for (i in offset until offset + length) {
            sum = (sum + (data[i].toInt() and 0xFF)) and 0xFF
        }
        return sum.toByte()
    }

    // ========== 解析 MCU → APP 帧 ==========

    /**
     * 解析传感器数据帧 CMD 0x01 (LEN=14)
     * offset 0-3:   WaterTemp    float LE
     * offset 4-7:   PH_Value     float LE
     * offset 8:     WaterLevel   uint8 (0~100%)
     * offset 9-10:  OxygenLevel  uint16 BE
     * offset 11:    RunMode      uint8 (0=auto, 1=manual)
     * offset 12-13: FeedCountdown uint16 BE (秒)
     */
    fun parseSensorData(frame: ByteArray): SensorData {
        val payloadOffset = 3 // HEAD(1) + CMD(1) + LEN(1)

        // float LE
        val waterTemp = ByteBuffer.wrap(frame, payloadOffset, 4)
            .order(ByteOrder.LITTLE_ENDIAN).float
        val phValue = ByteBuffer.wrap(frame, payloadOffset + 4, 4)
            .order(ByteOrder.LITTLE_ENDIAN).float

        // uint8
        val waterLevel = frame[payloadOffset + 8].toInt() and 0xFF

        // uint16 BE
        val oxygenLevel = ByteBuffer.wrap(frame, payloadOffset + 9, 2)
            .order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF

        // uint8
        val runMode = frame[payloadOffset + 11].toInt() and 0xFF

        // uint16 BE (MCU int16_t: may be negative, read as signed Short)
        val feedCountdown = ByteBuffer.wrap(frame, payloadOffset + 12, 2)
            .order(ByteOrder.BIG_ENDIAN).short.toInt()

        return SensorData(
            waterTemp = waterTemp,
            phValue = phValue,
            waterLevel = waterLevel,
            oxygenLevel = oxygenLevel,
            runMode = runMode,
            feedCountdown = feedCountdown
        )
    }

    /**
     * 解析设备状态帧 CMD 0x02 (LEN=5)
     * offset 0: relay_state uint8 (bit0=加热 bit1=加水 bit2=排水 bit3=增氧)
     * offset 1: Alarm_Enable uint8 (0/1)
     * offset 2: Feeding uint8 (0/1)
     * offset 3: Reserved
     * offset 4: Reserved
     */
    fun parseDeviceStatus(frame: ByteArray): DeviceStatus {
        val payloadOffset = 3
        val relayState = frame[payloadOffset].toInt() and 0xFF
        return DeviceStatus(
            relayHeat = (relayState and 0x01) != 0,
            relayFill = (relayState and 0x02) != 0,
            relayDrain = (relayState and 0x04) != 0,
            relayOxygen = (relayState and 0x08) != 0,
            alarmEnable = frame[payloadOffset + 1].toInt() != 0,
            feeding = frame[payloadOffset + 2].toInt() != 0
        )
    }

    /**
     * 解析报警事件帧 CMD 0x03 (LEN=2)
     * offset 0: alarm_type uint8
     * offset 1: alarm_status uint8 (0=解除, 1=触发)
     */
    fun parseAlarmEvent(frame: ByteArray): AlarmEvent {
        val payloadOffset = 3
        val alarmType = frame[payloadOffset].toInt() and 0xFF
        val alarmStatus = frame[payloadOffset + 1].toInt() and 0xFF
        return AlarmEvent(
            alarmType = alarmType,
            alarmStatus = alarmStatus
        )
    }

    // ========== APP → MCU 发送帧构造 ==========

    /**
     * HEAD(0xAA) | CMD(1B) | LEN(1B) | PAYLOAD(NB) | SUM(1B) | END(0x55)
     */
    private fun buildFrame(cmd: Byte, data: ByteArray = byteArrayOf()): ByteArray {
        val payloadLen = data.size
        val frameSize = 3 + payloadLen + 2 // HEAD+CMD+LEN + payload + SUM+END
        val frame = ByteArray(frameSize)
        var idx = 0

        frame[idx++] = HEADER
        frame[idx++] = cmd
        frame[idx++] = payloadLen.toByte()
        data.forEach { frame[idx++] = it }

        // 校验和 = CMD + LEN + payload
        val checksum = calculateChecksum(frame, 1, 2 + payloadLen)
        frame[idx++] = checksum
        frame[idx] = END

        return frame
    }

    /** CMD 0x10 查询状态 (LEN=0) */
    fun buildQueryStatusFrame(): ByteArray = buildFrame(CMD_QUERY_STATUS)

    /** CMD 0x11 切换模式 (LEN=1) */
    fun buildSwitchModeFrame(mode: WorkMode): ByteArray =
        buildFrame(CMD_SWITCH_MODE, byteArrayOf(if (mode == WorkMode.AUTO) 0.toByte() else 1.toByte()))

    /** CMD 0x12 控制继电器 (LEN=2): relay_id + state */
    fun buildControlRelayFrame(relayId: Int, state: Boolean): ByteArray =
        buildFrame(CMD_CONTROL_RELAY, byteArrayOf(relayId.toByte(), if (state) 1.toByte() else 0.toByte()))

    /** CMD 0x13 设置阈值 (LEN=20) */
    fun buildSyncThresholdsFrame(thresholds: ThresholdSet): ByteArray {
        val buf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
        buf.putFloat(thresholds.tempLower)
        buf.putFloat(thresholds.tempUpper)
        // OxygenLevel_Max uint16 BE
        buf.put((thresholds.oxygenLevelMax shr 8).toByte())
        buf.put((thresholds.oxygenLevelMax and 0xFF).toByte())
        buf.putFloat(thresholds.phLower)
        buf.putFloat(thresholds.phUpper)
        buf.put(thresholds.waterLevelMin.toByte())
        buf.put(thresholds.waterLevelMax.toByte())
        return buildFrame(CMD_SYNC_THRESHOLDS, buf.array())
    }

    /** CMD 0x14 触发喂食 (LEN=0) */
    fun buildTriggerFeedFrame(): ByteArray = buildFrame(CMD_TRIGGER_FEED)

    /** CMD 0x15 设置报警使能 (LEN=1) */
    fun buildSetAlarmFrame(enabled: Boolean): ByteArray =
        buildFrame(CMD_SET_ALARM, byteArrayOf(if (enabled) 1.toByte() else 0.toByte()))

    /** CMD 0x16 设置喂食间隔 (LEN=2): seconds uint16 BE */
    fun buildSetFeedIntervalFrame(seconds: Int): ByteArray {
        val data = byteArrayOf(
            (seconds shr 8).toByte(),
            (seconds and 0xFF).toByte()
        )
        return buildFrame(CMD_SET_FEED_INTERVAL, data)
    }

    /** CMD 0x20 控制LED (LEN=2): led_id(0=LED1,1=LED2) + state(0=OFF,1=ON) */
    fun buildLedControlFrame(ledId: Int, on: Boolean): ByteArray =
        buildFrame(CMD_LED_CONTROL, byteArrayOf(ledId.toByte(), if (on) 1.toByte() else 0.toByte()))
}
