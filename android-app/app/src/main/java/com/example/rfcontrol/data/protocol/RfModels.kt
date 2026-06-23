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
    val deviceId: String,
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
    Mode1("模式 1", 0x31),
    Mode2("模式 2", 0x32),
    Mode3("模式 3", 0x33),
    Mode4("模式 4", 0x34),
    Mode5("模式 5", 0x35),
    Mode6("模式 6", 0x36),
    Mode7("模式 7", 0x37),
    Mode8("模式 8", 0x38),
    Mode9("模式 9", 0x39),
    Standby("待机", 0x3A),
    HeatOn("开启加热", 0x3B),
    HeatOff("关闭加热", 0x3C),
    PowerOff("关机", 0x3D)
}
