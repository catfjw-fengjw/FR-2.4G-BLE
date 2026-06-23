# RF2.4G 控制端 Android Demo

这是按 `web-prototype` 原型实现的 Android 原生 UI Demo。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Mock RF Transport

## 功能

- 控制台：设备 ID、广播状态、电量、RSSI、模式矩阵、强度控制、42 字节空中包预览、事件日志。
- 设备扫描：模拟设备列表、绑定设备、手动输入设备 ID。
- 协议调试：Byte1~Byte42 字段表、校验信息、模拟发送/接收、复制包、清空日志。
- 协议层：控制端 42 字节空中包预览、31 字节 AdvData 提取、设备端状态包解析、Byte23 应用层校验码、链路层 CRC 字段说明。
- 传输层：`RfTransport` 接口、`MockRfTransport` 当前实现、`BleLegacyRfTransport` 后续真实 BLE 广播占位。

## 协议长度说明

协议里的 42 字节是完整空中包长度：

```text
2 字节 BLE 广播 PDU 头 + 6 字节广播 MAC + 31 字节 AdvData + 3 字节链路层 CRC = 42 字节
```

Android 应用层不应把 42 字节整体塞进广播数据。真实手机发广播时，应用层需要处理的是 Byte9~Byte39，即 31 字节 Legacy AdvData；PDU 头、广播地址和 CRC 由 BLE 控制器/链路层处理。

注意：Android 公共 `BluetoothLeAdvertiser` API 通常通过 AD Structure 组合广播数据，不提供“直接写入原始 31 字节 AdvData”的通用接口。接真实硬件前，需要确认设备端固件是否接受 Android API 生成的 AD 结构；如必须逐字节完全一致，可能需要厂商 RF 模块或芯片 SDK 支持。

## 构建

在 Android Studio 打开 `android-app` 目录即可同步工程。

命令行构建示例：

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\fengjianwu\AppData\Local\Android\Sdk'
& 'C:\Users\fengjianwu\.gradle\wrapper\dists\gradle-9.1.0-bin\9agqghryom9wkf8r80qlhnts3\gradle-9.1.0\bin\gradle.bat' assembleDebug
```

Debug APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 测试

Windows 中文路径下，AGP/JUnit 可能出现 test class 加载问题。可临时映射 ASCII 盘符后运行：

```powershell
subst R: 'C:\Users\fengjianwu\Documents\蓝牙RF协议\android-app'
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\fengjianwu\AppData\Local\Android\Sdk'
Push-Location 'R:\'
& 'C:\Users\fengjianwu\.gradle\wrapper\dists\gradle-9.1.0-bin\9agqghryom9wkf8r80qlhnts3\gradle-9.1.0\bin\gradle.bat' testDebugUnitTest
Pop-Location
subst R: /D
```

已验证：

- `assembleDebug` 成功。
- `testDebugUnitTest` 在 ASCII 盘符映射下成功。
