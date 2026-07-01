package com.example.rfcontrol.data.protocol

data class StrengthLevels(
    val vibration: Int = 42,
    val slap: Int = 30,
    val suction: Int = 56,
    val clip: Int = 3,
    val electric: Int = 0,
    val heat: Int = 36
)

data class RfDevice(
    val id: String,
    val mac: String,
    val rssi: Int,
    val battery: Int,
    val lastSeen: String
)

data class DeviceStatus(
    val namePrefix: String,
    val deviceId: String,
    val companyId: Int,
    val mode: ControlMode,
    val levels: StrengthLevels,
    val mac: String,
    val battery: Int,
    val rssi: Int,
    val packetHex: String
)

enum class LogType {
    Info,
    Ok,
    Tx,
    Rx,
    Warn,
    Error
}

data class EventLog(
    val type: LogType,
    val message: String
)

enum class ControlMode(val label: String, val value: Int) {
    Mode1("经典", 0x31),
    Mode2("太空", 0x32),
    Mode3("飞机", 0x33),
    Mode4("冲浪", 0x34),
    Mode5("强化", 0x35),
    Mode6("混沌", 0x36),
    Mode7("狂野", 0x37),
    Mode8("麻辣", 0x38),
    Mode9("黑化", 0x39),
    Standby("休眠", 0x3A),
    HeatOn("开启", 0x3B),
    HeatOff("关闭", 0x3C),
    PowerOff("停机", 0x3D)
}
