package com.example.intelligentfish.data.model

data class AlarmEvent(
    val alarmType: Int,
    val alarmStatus: Int  // 0=解除, 1=触发
) {
    val typeName: String get() = when (alarmType) {
        0x01 -> "温度过低"
        0x02 -> "温度过高"
        0x03 -> "水位过低"
        0x04 -> "水位过高"
        0x05 -> "溶氧量过低"
        0x06 -> "PH过低"
        0x07 -> "PH过高"
        else -> "未知报警"
    }

    val message: String get() = "$typeName ${if (alarmStatus == 1) "触发" else "解除"}"
}
