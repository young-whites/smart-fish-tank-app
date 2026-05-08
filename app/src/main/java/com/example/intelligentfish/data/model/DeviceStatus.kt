package com.example.intelligentfish.data.model

data class DeviceStatus(
    val relayHeat: Boolean = false,
    val relayFill: Boolean = false,
    val relayDrain: Boolean = false,
    val relayOxygen: Boolean = false,
    val alarmEnable: Boolean = false,
    val feeding: Boolean = false
)

enum class WorkMode(val label: String) {
    AUTO("自动"),
    MANUAL("手动")
}
