# RF 广播协议包规格书 V1.6

| 文档名称 | RF 广播协议包规格书 |
| :-- | :-- |
| 协议版本 | V1.6 |
| 适用范围 | 设备端与控制端通过 RF / BLE 广播进行状态上报与控制指令下发 |
| 设备端 | 玩具设备 |
| 控制端 | 手机 APP |

## 1. 概述

本文档定义设备端与控制端之间的 RF / BLE 广播协议包格式、字段含义、校验方式及蓝牙广播实现要求。

协议交互分为两类：

1. 设备端发送广播包：设备端周期性广播当前状态，供控制端扫描和识别。
2. 控制端发送广播包：控制端通过广播方式下发模式等控制指令。

V1.6 采用 BLE Legacy Advertising 可合法解析的 AdvData 结构：

```text
Complete Local Name + Manufacturer Specific Data
```

完整空中包仍按 42 字节描述：

```text
2 字节 PDU Header + 6 字节 AdvA + 31 字节 AdvData + 3 字节 CRC = 42 字节
```

## 2. 术语和基本规则

### 2.1 术语定义

| 术语 | 定义 |
| :-- | :-- |
| 设备端 | 玩具设备。 |
| 控制端 | 手机 APP 端。 |
| AdvA | BLE 广播地址，占 6 字节，由设备端或蓝牙控制器生成。 |
| AdvData | BLE Legacy Advertising Data，占 31 字节。 |
| AD Structure | BLE 广播数据结构，格式为 Length + AD Type + Value。 |
| Complete Local Name | AD Type `0x09`，完整设备名称字段。 |
| Manufacturer Specific Data | AD Type `0xFF`，厂商自定义数据字段。 |
| CRC | BLE 链路层 CRC，占 3 字节。 |

### 2.2 BLE Length 字段规则

BLE AD Structure 的 Length 字段表示 Length 后面还有多少字节，不包含 Length 自身。

例如：

```text
09 09 4C 58 30 30 30 30 30 31
```

含义为：

```text
09 = 后面还有 9 字节
09 = AD Type: Complete Local Name
4C 58 30 30 30 30 30 31 = 名称内容 LX000001
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
```

其中：

```text
37 字节 = 6 字节 AdvA + 31 字节 AdvData
```

### 3.2 广播通道

广播在 BLE 广播信道 37、38、39 上发送。

### 3.3 广播间隔

| 场景 | 要求 |
| :-- | :-- |
| 设备端状态上报 | 默认每 5 秒发送一次。 |
| 控制端指令下发 | 默认每 1 秒发送一次；调试或控制场景可按实际需要调整。 |

### 3.4 Android APP 兼容说明

1. Android 标准 BLE API 可以生成 `Complete Local Name` 和 `Manufacturer Specific Data`。
2. Android APP 不能直接指定真实 AdvA，也不能直接指定 BLE CRC。
3. Android 11 标准 BLE API 不能强制生成 Flags 字段 `02 01 06`。
4. 如需在 Android 14 及以上尝试生成 discoverable Flags，可使用 `AdvertisingSetParameters.Builder.setDiscoverable(true)`，但实际抓包仍以手机蓝牙协议栈为准。
5. 本协议 V1.6 不强制要求 AdvData 中包含 Flags 字段。

## 4. 数据包总则

### 4.1 包长度

本协议包固定长度为 42 字节。

### 4.2 字节编号

本文表格采用 `Byte1~Byte42` 描述协议字节位置；代码示例采用 C 数组下标 `tx_buff[0]~tx_buff[41]`。两者对应关系如下：

```text
ByteN = tx_buff[N - 1]
```

### 4.3 V1.6 通用包结构

| 字节位置 | 字段 | 固定值 / 范围 | 说明 |
| :--: | :-- | :--: | :-- |
| Byte1 | 广播类型 | `0x42` | ADV_NONCONN_IND。 |
| Byte2 | 有效数据长度 | `0x25` | PDU payload 长度 37 字节。 |
| Byte3~Byte8 | AdvA | 由设备/平台生成 | BLE 广播地址。 |
| Byte9 | 名称段长度 | `0x09` | 后续 9 字节。 |
| Byte10 | 名称字段类型 | `0x09` | Complete Local Name。 |
| Byte11~Byte12 | 固定名称前缀 | `0x4C,0x58` | ASCII `LX`。 |
| Byte13~Byte18 | 设备 ID | 6 字节 ASCII | 方案商代号、设备代号、客户代码、序列号。 |
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
| Byte30~Byte39 | 保留 | `0x00` | 预留扩展字段。 |
| Byte40~Byte42 | CRC | 链路层生成 | 示例中可预填 `0x55,0x55,0x55`。 |

### 4.4 AdvData 分段

V1.6 的 31 字节 AdvData 由 2 个合法 AD Structure 组成。

#### 4.4.1 Complete Local Name

```text
09 09 4C 58 [6 字节设备 ID]
```

总长度 10 字节。

#### 4.4.2 Manufacturer Specific Data

```text
14 FF [Company ID 2 字节] [模式 1 字节] [6 字节强度] [10 字节保留]
```

总长度 21 字节。

#### 4.4.3 AdvData 长度校验

```text
10 字节 Complete Local Name + 21 字节 Manufacturer Specific Data = 31 字节 AdvData
```

## 5. 设备 ID 格式

### 5.1 名称字段格式

名称字段固定为：

```text
LX + 6 字节设备 ID
```

示例：

```text
LX000001 = 4C 58 30 30 30 30 30 31
```

### 5.2 6 字节设备 ID

| 字节位置 | Byte13 | Byte14 | Byte15 | Byte16~Byte18 |
| :--: | :--: | :--: | :--: | :--: |
| 含义 | 方案商代号 | 设备代号 | 客户代码 | 序列号 |
| 示例 | `0x30` | `0x30` | `0x30` | `0x30,0x30,0x31` |

默认示例设备 ID：

```text
000001
```

完整名称：

```text
LX000001
```

## 6. 控制端发送广播包

### 6.1 包格式

控制端发送广播包用于向设备端下发控制指令，包长固定为 42 字节。

| 字节位置 | 字段 | 取值 / 范围 | 说明 |
| :--: | :-- | :--: | :-- |
| Byte1 | 广播类型 | `0x42` | ADV_NONCONN_IND。 |
| Byte2 | 有效数据长度 | `0x25` | PDU payload 长度 37 字节。 |
| Byte3~Byte8 | AdvA | 平台生成 | 手机 APP 不能保证指定。 |
| Byte9~Byte18 | Complete Local Name | 见 4.4.1 | 设备名称与 ID。 |
| Byte19~Byte39 | Manufacturer Specific Data | 见 4.4.2 | 模式与预留字段。 |
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
| `0x3A` | 待机模式 | 设备进入待机。 |
| `0x3B` | 开启加热 | 仅影响加热状态。 |
| `0x3C` | 关闭加热 | 仅影响加热状态。 |
| `0x3D` | 关机模式 | 设备进入关机流程。 |

### 6.3 校验说明

V1.6 当前不启用 APP 端广播校验码。Manufacturer Specific Data 中包含模式字段和 6 个控制强度字段，其余字段按预留处理，默认填 `0x00`。

如后续需要恢复协议层校验，可在 Byte30~Byte39 预留字段中定义新的校验字节。

### 6.4 控制端发送包示例

```c
uint8_t tx_buff[42] = {0};
uint8_t mode = 0x31;  // 示例：模式 1

tx_buff[0] = 0x42;  // Byte1: 广播类型，ADV_NONCONN_IND
tx_buff[1] = 0x25;  // Byte2: PDU payload 长度 37 字节

// Byte3~Byte8: AdvA 示例。真实 Android 手机由蓝牙控制器生成。
tx_buff[2] = 0xA5;
tx_buff[3] = 0xA5;
tx_buff[4] = 0xA5;
tx_buff[5] = 0xA5;
tx_buff[6] = 0xA5;
tx_buff[7] = 0xA5;

// Byte9~Byte18: Complete Local Name，结构总长度 10 字节。
tx_buff[8]  = 0x09;  // Length: 后面 9 字节
tx_buff[9]  = 0x09;  // AD Type: Complete Local Name
tx_buff[10] = 0x4C;  // L
tx_buff[11] = 0x58;  // X
tx_buff[12] = 0x30;  // 方案商代号
tx_buff[13] = 0x30;  // 设备代号
tx_buff[14] = 0x30;  // 客户代码
tx_buff[15] = 0x30;  // 序列号
tx_buff[16] = 0x30;  // 序列号
tx_buff[17] = 0x31;  // 序列号

// Byte19~Byte39: Manufacturer Specific Data，结构总长度 21 字节。
tx_buff[18] = 0x14;  // Length: 后面 20 字节
tx_buff[19] = 0xFF;  // AD Type: Manufacturer Specific Data
tx_buff[20] = 0x00;  // Company ID LSB，当前暂用 0x0000
tx_buff[21] = 0x00;  // Company ID MSB，当前暂用 0x0000
tx_buff[22] = mode;  // Byte23: 模式定义

// Byte24~Byte29: 控制强度
tx_buff[23] = 0x00;  // Byte24: 震动强度
tx_buff[24] = 0x00;  // Byte25: 拍打强度
tx_buff[25] = 0x00;  // Byte26: 吮吸强度
tx_buff[26] = 0x00;  // Byte27: 夹吸档位
tx_buff[27] = 0x00;  // Byte28: 电击强度
tx_buff[28] = 0x00;  // Byte29: 加热强度

// Byte30~Byte39: 保留字段
tx_buff[29] = 0x00;  // Byte30: 预留
tx_buff[30] = 0x00;  // Byte31: 预留
tx_buff[31] = 0x00;  // Byte32: 预留
tx_buff[32] = 0x00;  // Byte33: 预留
tx_buff[33] = 0x00;  // Byte34: 预留
tx_buff[34] = 0x00;  // Byte35: 预留
tx_buff[35] = 0x00;  // Byte36: 预留
tx_buff[36] = 0x00;  // Byte37: 预留
tx_buff[37] = 0x00;  // Byte38: 预留
tx_buff[38] = 0x00;  // Byte39: 预留

// Byte40~Byte42: CRC 预填充值，最终发送前由链路层替换为 CRC 结果
tx_buff[39] = 0x55;
tx_buff[40] = 0x55;
tx_buff[41] = 0x55;
```

示例完整包：

```text
42 25 A5 A5 A5 A5 A5 A5 09 09 4C 58 30 30 30 30 30 31 14 FF 00 00 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 55 55 55
```

## 7. 设备端发送广播包

设备端发送广播包用于上报设备状态。V1.6 设备端发送包与控制端发送包采用相同 BLE AdvData 结构。

### 7.1 包格式

| 字节位置 | 字段 | 取值 / 范围 | 说明 |
| :--: | :-- | :--: | :-- |
| Byte1 | 广播类型 | `0x42` | ADV_NONCONN_IND。 |
| Byte2 | 有效数据长度 | `0x25` | PDU payload 长度 37 字节。 |
| Byte3~Byte8 | AdvA | 设备生成 | 设备端广播地址。 |
| Byte9~Byte18 | Complete Local Name | 见 4.4.1 | 设备名称与 ID。 |
| Byte19~Byte39 | Manufacturer Specific Data | 见 4.4.2 | 状态模式与预留字段。 |
| Byte40~Byte42 | CRC | 链路层生成 | 示例占位 `55 55 55`。 |

### 7.2 设备端发送包示例

```c
uint8_t tx_buff[42] = {0};
uint8_t mode = 0x31;  // 示例：设备当前模式

tx_buff[0] = 0x42;
tx_buff[1] = 0x25;

// Byte3~Byte8: 设备端 AdvA 示例
tx_buff[2] = 0xA5;
tx_buff[3] = 0xA5;
tx_buff[4] = 0xA5;
tx_buff[5] = 0xA5;
tx_buff[6] = 0xA5;
tx_buff[7] = 0xA5;

// Byte9~Byte18: Complete Local Name = LX000001
tx_buff[8]  = 0x09;
tx_buff[9]  = 0x09;
tx_buff[10] = 0x4C;
tx_buff[11] = 0x58;
tx_buff[12] = 0x30;
tx_buff[13] = 0x30;
tx_buff[14] = 0x30;
tx_buff[15] = 0x30;
tx_buff[16] = 0x30;
tx_buff[17] = 0x31;

// Byte19~Byte39: Manufacturer Specific Data
tx_buff[18] = 0x14;
tx_buff[19] = 0xFF;
tx_buff[20] = 0x00;
tx_buff[21] = 0x00;
tx_buff[22] = mode;

tx_buff[23] = 0x00;  // 震动强度
tx_buff[24] = 0x00;  // 拍打强度
tx_buff[25] = 0x00;  // 吮吸强度
tx_buff[26] = 0x00;  // 夹吸档位
tx_buff[27] = 0x00;  // 电击强度
tx_buff[28] = 0x00;  // 加热强度
tx_buff[29] = 0x00;  // 预留
tx_buff[30] = 0x00;  // 预留
tx_buff[31] = 0x00;  // 预留
tx_buff[32] = 0x00;  // 预留
tx_buff[33] = 0x00;  // 预留
tx_buff[34] = 0x00;  // 预留
tx_buff[35] = 0x00;  // 预留
tx_buff[36] = 0x00;  // 预留
tx_buff[37] = 0x00;  // 预留
tx_buff[38] = 0x00;  // 预留

tx_buff[39] = 0x55;
tx_buff[40] = 0x55;
tx_buff[41] = 0x55;
```

## 8. 接收端解析建议

接收端建议按 BLE AD Structure 解析 Byte9~Byte39：

1. 从 AdvData 开始读取 AD Structure。
2. 找到 `AD Type = 0x09` 的 Complete Local Name。
3. 校验名称是否以 `LX` 开头，并读取后续 6 字节设备 ID。
4. 找到 `AD Type = 0xFF` 的 Manufacturer Specific Data。
5. 校验 Company ID，读取模式字段。
6. 按设备 ID 判断数据是否属于目标设备。

示例解析目标：

```text
09 09 4C 58 30 30 30 30 30 31
14 FF 00 00 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
```

## 9. 版本记录

| 版本 | 日期 | 说明 |
| :--: | :--: | :-- |
| V1.6 | 2026-06-30 | 明确 Company ID 两字节、Byte24~Byte29 六个控制强度字段，控制端和设备端示例均逐字节展开。 |
| V1.5 | 2026-06-30 | 改为合法 BLE AD Structure：Complete Local Name + Manufacturer Specific Data。 |
| V1.4 | 2026-06-29 | 原始 42 字节 RF 广播包定义。 |

