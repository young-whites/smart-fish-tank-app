package com.example.intelligentfish.data.model

data class SensorData(
    val waterTemp: Float = 0f,      // 水温 °C
    val phValue: Float = 0f,        // pH 值
    val waterLevel: Int = 0,        // 水位 uint8, 0~100%
    val oxygenLevel: Int = 0,        // 溶氧量 uint16 BE, MCU 值域 0~20
    val runMode: Int = 0,           // 0=自动, 1=手动
    val feedCountdown: Int = 0      // 喂食倒计时 秒
)
