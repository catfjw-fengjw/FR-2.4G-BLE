# RF 广播协议包规格书 V1.7

| 文档名称 | RF 广播协议包规格书 |
| :-- | :-- |
| 协议版本 | V1.7 |
| 更新日期 | 2026-06-30 |
| 适用范围 | 设备端与控制端通过 RF / BLE 广播进行状态上报与控制指令下发 |
| 设备端 | 玩具设备 |
| 控制端 | 手机 APP |

## 1. 概述

本协议定义设备端与控制端之间的 RF / BLE 广播包格式、字段含义、设备 ID 识别规则、状态上报和控制指令下发要求。

协议交互分为两类：

1. 控制端发送广播包：手机 APP 通过广播方式向设备端下发模式、强度等控制指令。
2. 设备端发送广播包：设备端周期性广播当前状态，供控制端扫描、识别和显示。

V1.7 继续采用 BLE Legacy Advertising 可合法解析的 AdvData 结构：

```text
Complete Local Name + Manufacturer Specific Data
```

完整空中包固定按 42 字节描述：

```text
2 字节 PDU Header + 6 字节 AdvA + 31 字节 AdvData + 3 字节 CRC = 42 字节
```

## 2. 术语和基本规则

### 2.1 术语定义

| 术语 | 定义 |
| :-- | :-- |
| 设备端 | 被控制的玩具设备。 |
| 控制端 | 手机 APP。 |
| AdvA | BLE 广播地址，占 6 字节，由设备端或蓝牙控制器生成。 |
| AdvData | BLE Legacy Advertising Data，占 31 字节。 |
| AD Structure | BLE 广播数据结构，格式为 Length + AD Type + Value。 |
| Complete Local Name | AD Type `0x09`，完整设备名称字段。 |
| Manufacturer Specific Data | AD Type `0xFF`，厂商自定义数据字段。 |
| CRC | BLE 链路层 CRC，占 3 字节。 |

### 2.2 BLE Length 字段规则

BLE AD Structure 的 Length 字段表示 Length 后面还有多少字节，不包含 Length 自身。

示例：

```text
09 09 4C 58 31 31 31 31 31 31
```

含义：

```text
09 = 后面还有 9 字节
09 = AD Type: Complete Local Name
4C 58 31 31 31 31 31 31 = 名称内容 LX111111
```

因此该 AD Structure 总字节数为 10 字节。

## 3. 蓝牙广播实现要求

### 3.1 广播类型

本协议采用非连接广播方式传输控制和状态数据。

PDU Header 目标值：

```text
Byte1 = 0x42
Byte2 = 0x25
```

说明：

```text
0x42 = ADV_NONCONN_IND
0x25 = PDU payload 长度 37 字节
37 字节 = 6 字节 AdvA + 31 字节 AdvData
```

### 3.2 广播通道

广播在 BLE 广播信道 37、38、39 上发送。

### 3.3 广播间隔

| 场景 | 要求 |
| :-- | :-- |
| 设备端状态上报 | 默认每 5 秒发送一次，可按设备功耗和响应要求调整。 |
| 控制端指令下发 | 默认周期发送；调试或控制场景可按实际需要提高发送频率。 |

### 3.4 Android APP 兼容说明

1. Android 标准 BLE API 可以生成 `Complete Local Name` 和 `Manufacturer Specific Data`。
2. Android APP 不能直接指定真实 AdvA，也不能直接指定 BLE CRC。
3. Android 11 标准 BLE API 不能强制生成 Flags 字段 `02 01 06`。
4. 实际抓包中的 AdvA、CRC 和部分链路层字段以手机蓝牙控制器输出为准。

## 4. 数据包总则

### 4.1 包长度

协议包固定长度为 42 字节。

### 4.2 字节编号

本文表格采用 `Byte1~Byte42` 描述协议字节位置；代码示例采用 C 数组下标 `TXbufData[0]~TXbufData[41]`。两者对应关系如下：

```text
ByteN = TXbufData[N - 1]
```

### 4.3 V1.7 通用包结构

| 字节位置 | 字段 | 固定值 / 范围 | 说明 |
| :--: | :-- | :--: | :-- |
| Byte1 | 广播类型 | `0x42` | ADV_NONCONN_IND。 |
| Byte2 | 有效数据长度 | `0x25` | PDU payload 长度 37 字节。 |
| Byte3~Byte8 | AdvA | 设备端或平台生成 | BLE 广播地址。 |
| Byte9 | 名称段长度 | `0x09` | 后续 9 字节。 |
| Byte10 | 名称字段类型 | `0x09` | Complete Local Name。 |
| Byte11~Byte12 | 固定名称前缀 | 控制端 `LX` / 设备端 `LY` | 控制端下发使用 `LX`，设备端回复使用 `LY`。 |
| Byte13~Byte18 | 设备 ID | 6 字节 ASCII | 设备 FLASH 内固化的 6 字节令牌。 |
| Byte19 | 厂商段长度 | `0x14` | 后续 20 字节。 |
| Byte20 | 厂商字段类型 | `0xFF` | Manufacturer Specific Data。 |
| Byte21~Byte22 | Company ID | 默认 `0x00,0x00` | 小端格式，当前暂用 `0x0000`。 |
| Byte23 | 模式定义 | 见 6.2 | 控制端下发模式或设备端状态模式。 |
| Byte24 | 震动强度 | `0x00~0x64` | 0~100。 |
| Byte25 | 拍打强度 | `0x00~0x64` | 0~100。 |
| Byte26 | 吮吸强度 | `0x00~0x64` | 0~100。 |
| Byte27 | 夹吸档位 | `0x00~0x06` | 0~6 档。 |
| Byte28 | 电击强度 | `0x00~0x64` | 0~100。 |
| Byte29 | 加热强度 | `0x00~0x64` | 0~100。 |
| Byte30~Byte31 | 保留 | `0x00` | 预留扩展字段。 |
| Byte32 | 电量 | `0x00~0x64` | 设备端状态上报电量百分比，控制端下发时可填 `0x00`。 |
| Byte33~Byte39 | 保留 | `0x00` | 预留扩展字段。 |
| Byte40~Byte42 | CRC | 链路层生成 | 示例中可预填 `0x55,0x55,0x55`。 |

### 4.4 AdvData 分段

V1.7 的 31 字节 AdvData 由 2 个合法 AD Structure 组成。

#### 4.4.1 Complete Local Name

控制端下发包：

```text
09 09 4C 58 [6 字节设备 ID]
```

设备端回复/状态包：

```text
09 09 4C 59 [6 字节设备 ID]
```

总长度为 10 字节。

#### 4.4.2 Manufacturer Specific Data

```text
14 FF [Company ID 2 字节] [模式 1 字节] [6 字节强度] [保留 2 字节] [电量 1 字节] [保留 7 字节]
```

总长度为 21 字节。

#### 4.4.3 AdvData 长度校验

```text
10 字节 Complete Local Name + 21 字节 Manufacturer Specific Data = 31 字节 AdvData
```

## 5. 设备 ID 规则

### 5.1 设备 ID 定义

设备 ID 为 6 字节 ASCII 字符串，出厂时固化在设备 FLASH 内。该 ID 相当于控制端和设备端之间的识别令牌。

控制端 APP 输入的设备 ID 必须与受控设备 FLASH 内固化 ID 一致。设备端收到控制端广播后，第一时间比较名称字段中 `LX` 后面的 6 字节设备 ID；只有 ID 一致时，才继续解析模式和强度字段。

### 5.2 前缀规则

| 方向 | 固定前缀 | Complete Local Name 示例 | 说明 |
| :-- | :--: | :-- | :-- |
| 控制端下发到设备端 | `LX` | `LX111111` | 控制端发送命令使用。 |
| 设备端回复/状态上报 | `LY` | `LY111111` | 设备端广播状态使用。 |

注意：`LX` 和 `LY` 只是方向前缀，后面的 6 字节设备 ID 必须一致。

### 5.3 示例设备 ID

示例设备 ID：

```text
111111
```

控制端完整名称：

```text
LX111111 = 4C 58 31 31 31 31 31 31
```

设备端完整名称：

```text
LY111111 = 4C 59 31 31 31 31 31 31
```

## 6. 控制端发送广播包

### 6.1 包格式

控制端发送广播包用于向设备端下发控制指令，包长固定为 42 字节。

| 字节位置 | 字段 | 取值 / 范围 | 说明 |
| :--: | :-- | :--: | :-- |
| Byte1 | 广播类型 | `0x42` | ADV_NONCONN_IND。 |
| Byte2 | 有效数据长度 | `0x25` | PDU payload 长度 37 字节。 |
| Byte3~Byte8 | AdvA | 平台生成 | 手机 APP 不能保证指定。 |
| Byte9~Byte18 | Complete Local Name | `LX + 设备ID` | 控制端下发使用 `LX` 前缀。 |
| Byte19~Byte39 | Manufacturer Specific Data | 见 4.4.2 | 模式、强度与预留字段。 |
| Byte40~Byte42 | CRC | 链路层生成 | 示例占位 `55 55 55`。 |

### 6.2 模式定义

| 模式值 | 对应模式 | 说明 |
| :--: | :-- | :-- |
| `0x31` | 模式 1 | 常规模式。 |
| `0x32` | 模式 2 | 常规模式。 |
| `0x33` | 模式 3 | 常规模式。 |
| `0x34` | 模式 4 | 常规模式。 |
| `0x35` | 模式 5 | 常规模式。 |
| `0x36` | 模式 6 | 常规模式。 |
| `0x37` | 模式 7 | 常规模式。 |
| `0x38` | 模式 8 | 常规模式。 |
| `0x39` | 模式 9 | 常规模式。 |
| `0x3A` | 休眠 | 设备进入休眠。 |
| `0x3B` | 开启 | 开启加热或指定控制状态。 |
| `0x3C` | 关闭 | 关闭加热或指定控制状态。 |
| `0x3D` | 停机 | 设备进入停机流程。 |

### 6.3 控制端发送包示例

```c
uint8_t TXbufData[42] = {0};
uint8_t TempWorkMode = 0x31;  // 示例：模式 1

TXbufData[0] = 0x42;  // Byte1: 广播类型，ADV_NONCONN_IND
TXbufData[1] = 0x25;  // Byte2: PDU payload 长度 37 字节

// Byte3~Byte8: AdvA 示例。真实 Android 手机由蓝牙控制器生成。
TXbufData[2] = 0xA5;
TXbufData[3] = 0xA5;
TXbufData[4] = 0xA5;
TXbufData[5] = 0xA5;
TXbufData[6] = 0xA5;
TXbufData[7] = 0xA5;

// Byte9~Byte18: Complete Local Name = LX111111
TXbufData[8]  = 0x09;  // Length: 后面 9 字节
TXbufData[9]  = 0x09;  // AD Type: Complete Local Name
TXbufData[10] = 0x4C;  // L
TXbufData[11] = 0x58;  // X
TXbufData[12] = 0x31;  // 设备 ID
TXbufData[13] = 0x31;  // 设备 ID
TXbufData[14] = 0x31;  // 设备 ID
TXbufData[15] = 0x31;  // 设备 ID
TXbufData[16] = 0x31;  // 设备 ID
TXbufData[17] = 0x31;  // 设备 ID

// Byte19~Byte39: Manufacturer Specific Data
TXbufData[18] = 0x14;  // Length: 后面 20 字节
TXbufData[19] = 0xFF;  // AD Type: Manufacturer Specific Data
TXbufData[20] = 0x00;  // Company ID LSB，当前暂用 0x0000
TXbufData[21] = 0x00;  // Company ID MSB，当前暂用 0x0000
TXbufData[22] = TempWorkMode;  // Byte23: 模式定义

TXbufData[23] = 0x00;  // Byte24: 震动强度
TXbufData[24] = 0x00;  // Byte25: 拍打强度
TXbufData[25] = 0x00;  // Byte26: 吮吸强度
TXbufData[26] = 0x00;  // Byte27: 夹吸档位
TXbufData[27] = 0x00;  // Byte28: 电击强度
TXbufData[28] = 0x00;  // Byte29: 加热强度
TXbufData[29] = 0x00;  // Byte30: 预留
TXbufData[30] = 0x00;  // Byte31: 预留
TXbufData[31] = 0x00;  // Byte32: 电量，控制端下发时可填 0
TXbufData[32] = 0x00;  // Byte33: 预留
TXbufData[33] = 0x00;  // Byte34: 预留
TXbufData[34] = 0x00;  // Byte35: 预留
TXbufData[35] = 0x00;  // Byte36: 预留
TXbufData[36] = 0x00;  // Byte37: 预留
TXbufData[37] = 0x00;  // Byte38: 预留
TXbufData[38] = 0x00;  // Byte39: 预留

TXbufData[39] = 0x55;
TXbufData[40] = 0x55;
TXbufData[41] = 0x55;
```

控制端示例完整包：

```text
42 25 A5 A5 A5 A5 A5 A5 09 09 4C 58 31 31 31 31 31 31 14 FF 00 00 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 55 55 55
```

## 7. 设备端发送广播包

设备端发送广播包用于上报设备状态。设备端使用固化在 FLASH 内的 6 字节设备 ID，并使用 `LY` 前缀广播自身状态。

### 7.1 包格式

| 字节位置 | 字段 | 取值 / 范围 | 说明 |
| :--: | :-- | :--: | :-- |
| Byte1 | 广播类型 | `0x42` | ADV_NONCONN_IND。 |
| Byte2 | 有效数据长度 | `0x25` | PDU payload 长度 37 字节。 |
| Byte3~Byte8 | AdvA | 设备端生成 | 设备端广播地址。 |
| Byte9~Byte18 | Complete Local Name | `LY + 设备ID` | 设备端回复/状态上报使用 `LY` 前缀。 |
| Byte19~Byte39 | Manufacturer Specific Data | 见 4.4.2 | 状态模式、强度、电量与预留字段。 |
| Byte40~Byte42 | CRC | 链路层生成 | 示例占位 `55 55 55`。 |

### 7.2 设备端发送包示例

```c
uint8_t TXbufData[42] = {0};
uint8_t TempWorkMode = 0x31;  // 示例：设备当前模式

TXbufData[0] = 0x42;  // 广播类型：非连接广播
TXbufData[1] = 0x25;  // 有效数据长度

// 固定 MAC 地址
TXbufData[2] = 0xB1;
TXbufData[3] = 0xB2;
TXbufData[4] = 0xB3;
TXbufData[5] = 0xB4;
TXbufData[6] = 0xB5;
TXbufData[7] = 0xC1;

TXbufData[8]  = 0x09;  // 名称段长度 9
TXbufData[9]  = 0x09;  // 类型：设备名称
TXbufData[10] = 0x4C;  // L
TXbufData[11] = 0x59;  // Y
TXbufData[12] = 0x31;  // 设备 ID
TXbufData[13] = 0x31;  // 设备 ID
TXbufData[14] = 0x31;  // 设备 ID
TXbufData[15] = 0x31;  // 设备 ID
TXbufData[16] = 0x31;  // 设备 ID
TXbufData[17] = 0x31;  // 设备 ID

TXbufData[18] = 0x14;  // Length: 后面 20 字节
TXbufData[19] = 0xFF;  // AD Type: Manufacturer Specific Data
TXbufData[20] = 0x00;  // Company ID LSB，当前暂用 0x0000
TXbufData[21] = 0x00;  // Company ID MSB，当前暂用 0x0000
TXbufData[22] = TempWorkMode;  // 模式

TXbufData[23] = 0x00;  // 震动强度
TXbufData[24] = 0x00;  // 拍打强度
TXbufData[25] = 0x00;  // 吮吸强度
TXbufData[26] = 0x00;  // 夹吸档位
TXbufData[27] = 0x00;  // 电击强度
TXbufData[28] = 0x00;  // 加热强度
TXbufData[29] = 0x00;  // 预留
TXbufData[30] = 0x00;  // 预留
TXbufData[31] = 0x64;  // 电量
TXbufData[32] = 0x00;  // 预留
TXbufData[33] = 0x00;  // 预留
TXbufData[34] = 0x00;  // 预留
TXbufData[35] = 0x00;  // 预留
TXbufData[36] = 0x00;  // 预留
TXbufData[37] = 0x00;  // 预留
TXbufData[38] = 0x00;  // 预留

// 最后 3 字节填 0x55，留给 CRC 计算
TXbufData[39] = 0x55;
TXbufData[40] = 0x55;
TXbufData[41] = 0x55;
```

设备端示例完整包：

```text
42 25 B1 B2 B3 B4 B5 C1 09 09 4C 59 31 31 31 31 31 31 14 FF 00 00 31 00 00 00 00 00 00 00 00 64 00 00 00 00 00 00 00 55 55 55
```

## 8. 接收端解析建议

### 8.1 设备端接收控制端包

设备端接收控制端广播包时，建议按以下步骤解析：

1. 从 AdvData 开始读取 AD Structure。
2. 找到 `AD Type = 0x09` 的 Complete Local Name。
3. 校验名称前缀是否为 `LX`。
4. 读取 `LX` 后面的 6 字节设备 ID，并与本机 FLASH 内固化 ID 比较。
5. ID 一致时，继续读取 `AD Type = 0xFF` 的 Manufacturer Specific Data。
6. 解析 Company ID、模式和六个强度字段。
7. ID 不一致时，立即丢弃该广播包。

### 8.2 控制端接收设备端包

控制端接收设备端广播包时，建议按以下步骤解析：

1. 从 AdvData 开始读取 AD Structure。
2. 找到 `AD Type = 0x09` 的 Complete Local Name。
3. 校验名称前缀是否为 `LY`。
4. 读取 `LY` 后面的 6 字节设备 ID，并与 APP 当前输入的目标设备 ID 比较。
5. ID 一致时，继续读取 `AD Type = 0xFF` 的 Manufacturer Specific Data。
6. 解析模式、强度、电量和预留字段。
7. ID 不一致时，不作为当前目标设备状态处理。

示例解析目标：

```text
控制端下发 Name AD:
09 09 4C 58 31 31 31 31 31 31

设备端回复 Name AD:
09 09 4C 59 31 31 31 31 31 31

Manufacturer AD:
14 FF 00 00 31 00 00 00 00 00 00 00 00 64 00 00 00 00 00 00 00
```

## 9. 版本记录

| 版本 | 日期 | 说明 |
| :--: | :--: | :-- |
| V1.7 | 2026-06-30 | 明确设备 ID 为固化在设备 FLASH 内的 6 字节令牌；控制端下发使用 `LX` 前缀，设备端回复使用 `LY` 前缀；设备端示例 AdvA 为 `B1 B2 B3 B4 B5 C1`，并定义 Byte32 电量字段。 |
| V1.6 | 2026-06-30 | 明确 Company ID 两字节、Byte24~Byte29 六个控制强度字段，控制端和设备端示例均逐字节展开。 |
| V1.5 | 2026-06-30 | 改为合法 BLE AD Structure：Complete Local Name + Manufacturer Specific Data。 |
| V1.4 | 2026-06-29 | 原始 42 字节 RF 广播包定义。 |
