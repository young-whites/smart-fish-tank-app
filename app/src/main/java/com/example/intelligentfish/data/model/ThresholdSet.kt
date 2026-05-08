package com.example.intelligentfish.data.model

// 默认值与 MCU main.c 初始化值保持一致
data class ThresholdSet(
    var tempLower: Float = 20.0f,
    var tempUpper: Float = 35.0f,
    var oxygenLevelMax: Int = 12,       // uint16, MCU 值域 0~20
    var phLower: Float = 4.0f,
    var phUpper: Float = 8.0f,
    var waterLevelMin: Int = 50,        // uint8
    var waterLevelMax: Int = 70         // uint8
)
