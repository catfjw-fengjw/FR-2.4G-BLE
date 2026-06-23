import React from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  Activity,
  Antenna,
  BatteryMedium,
  Bluetooth,
  CheckCircle2,
  Clipboard,
  Cpu,
  Gauge,
  Hexagon,
  ListRestart,
  Play,
  Power,
  Radio,
  RotateCcw,
  Search,
  Settings2,
  ShieldAlert,
  Square,
  TerminalSquare,
  Thermometer,
  Zap
} from "lucide-react";

const MODES = [
  { label: "模式 1", value: 0x31 },
  { label: "模式 2", value: 0x32 },
  { label: "模式 3", value: 0x33 },
  { label: "模式 4", value: 0x34 },
  { label: "模式 5", value: 0x35 },
  { label: "模式 6", value: 0x36 },
  { label: "模式 7", value: 0x37 },
  { label: "模式 8", value: 0x38 },
  { label: "模式 9", value: 0x39 }
];

const ACTIONS = [
  { label: "待机", value: 0x3a, tone: "neutral" },
  { label: "开启加热", value: 0x3b, tone: "heat" },
  { label: "关闭加热", value: 0x3c, tone: "cool" },
  { label: "关机", value: 0x3d, tone: "danger" }
];

const STRENGTHS = [
  { key: "vibration", label: "震动强度", byte: 24, max: 100 },
  { key: "slap", label: "拍打强度", byte: 25, max: 100 },
  { key: "suction", label: "吮吸强度", byte: 26, max: 100 },
  { key: "electric", label: "电击强度", byte: 28, max: 100 },
  { key: "heat", label: "加热强度", byte: 29, max: 100 }
];

const INITIAL_LEVELS = {
  vibration: 42,
  slap: 30,
  suction: 56,
  clip: 3,
  electric: 0,
  heat: 36
};

const DEVICES = [
  { id: "LX_DX001", mac: "01:01:01:00:00:01", rssi: -46, battery: 86, lastSeen: "刚刚" },
  { id: "LX_DX014", mac: "01:01:02:00:00:0E", rssi: -63, battery: 72, lastSeen: "4 秒前" },
  { id: "LX_DX108", mac: "01:03:08:00:00:6C", rssi: -71, battery: 48, lastSeen: "12 秒前" }
];

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, Number(value)));
}

function toHex(value) {
  return value.toString(16).toUpperCase().padStart(2, "0");
}

function idToBytes(deviceId) {
  const normalized = deviceId.padEnd(8, "0").slice(0, 8);
  return Array.from(normalized).map((char) => char.charCodeAt(0));
}

function isDeviceIdValid(deviceId) {
  return /^LX_DX[0-9A-Z]{3}$/.test(deviceId);
}

function buildControlPacket({ deviceId, mode, levels }) {
  const bytes = Array(42).fill(0);
  bytes[0] = 0x42;
  bytes[1] = 0x25;
  bytes[2] = 0x00;
  bytes[3] = 0x00;
  bytes[4] = 0x00;
  bytes[5] = 0x00;
  bytes[6] = 0x00;
  bytes[7] = 0x00;
  bytes[8] = 0x02;
  bytes[9] = 0x01;
  bytes[10] = 0x06;
  bytes[11] = 0x0b;
  bytes[12] = 0x09;

  idToBytes(deviceId).forEach((value, index) => {
    bytes[13 + index] = value;
  });

  bytes[21] = mode;
  bytes[22] = bytes.slice(13, 22).reduce((sum, value) => sum + value, 0) & 0xff;
  bytes[23] = clamp(levels.vibration, 0, 100);
  bytes[24] = clamp(levels.slap, 0, 100);
  bytes[25] = clamp(levels.suction, 0, 100);
  bytes[26] = clamp(levels.clip, 0, 6);
  bytes[27] = clamp(levels.electric, 0, 100);
  bytes[28] = clamp(levels.heat, 0, 100);
  bytes[39] = 0x55;
  bytes[40] = 0x55;
  bytes[41] = 0x55;
  return bytes;
}

function buildDevicePacket(device, battery) {
  const bytes = buildControlPacket({
    deviceId: device.id,
    mode: 0,
    levels: { vibration: 0, slap: 0, suction: 0, clip: 0, electric: 0, heat: 0 }
  });
  const mac = device.mac.split(":").map((part) => parseInt(part, 16));
  mac.forEach((value, index) => {
    bytes[2 + index] = value;
  });
  bytes[21] = 0x00;
  bytes[22] = mac.reduce((sum, value) => sum + value, 0) & 0xff;
  bytes[23] = battery;
  return bytes;
}

function packetHex(bytes) {
  return bytes.map(toHex).join(" ");
}

function timestamp() {
  return new Date().toLocaleTimeString("zh-CN", { hour12: false });
}

function App() {
  const [tab, setTab] = useState("console");
  const [deviceId, setDeviceId] = useState("LX_DX001");
  const [selectedMode, setSelectedMode] = useState(0x31);
  const [levels, setLevels] = useState(INITIAL_LEVELS);
  const [isAdvertising, setIsAdvertising] = useState(false);
  const [isScanning, setIsScanning] = useState(true);
  const [activeDevice, setActiveDevice] = useState(DEVICES[0]);
  const [battery, setBattery] = useState(86);
  const [rssi, setRssi] = useState(-46);
  const [lastRxAt, setLastRxAt] = useState("等待收包");
  const [txCount, setTxCount] = useState(0);
  const [logs, setLogs] = useState([
    { type: "info", text: "原型已加载，当前使用模拟广播链路。" },
    { type: "warn", text: "42 字节为空中包预览；真机广播使用 Byte9~Byte39 的 31 字节 AdvData。" }
  ]);
  const txTickRef = useRef(0);
  const batteryRef = useRef(battery);
  const rssiRef = useRef(rssi);

  const packetBytes = useMemo(
    () => buildControlPacket({ deviceId, mode: selectedMode, levels }),
    [deviceId, selectedMode, levels]
  );
  const hexPacket = useMemo(() => packetHex(packetBytes), [packetBytes]);
  const validId = isDeviceIdValid(deviceId);

  function addLog(type, text) {
    setLogs((prev) => [{ type, text: `${timestamp()}  ${text}` }, ...prev].slice(0, 80));
  }

  function updateStrength(key, value) {
    setLevels((prev) => ({ ...prev, [key]: clamp(value, 0, key === "clip" ? 6 : 100) }));
  }

  function selectMode(value, label) {
    setSelectedMode(value);
    addLog("tx", `选择 ${label}，Byte22=${toHex(value)}，Byte23=${toHex(buildControlPacket({ deviceId, mode: value, levels })[22])}`);
  }

  function startAdvertising() {
    if (!validId) {
      addLog("error", "设备 ID 格式错误，示例格式：LX_DX001。");
      return;
    }
    setIsAdvertising(true);
    addLog("ok", "开始模拟广播，目标间隔 10ms。");
  }

  function stopAdvertising() {
    setIsAdvertising(false);
    addLog("info", "已停止模拟广播。");
  }

  function bindDevice(device) {
    setActiveDevice(device);
    setDeviceId(device.id);
    setBattery(device.battery);
    setRssi(device.rssi);
    setLastRxAt("刚刚");
    setTab("console");
    addLog("ok", `已绑定设备 ${device.id} / ${device.mac}。`);
  }

  async function copyPacket() {
    await navigator.clipboard?.writeText(hexPacket);
    addLog("ok", "已复制当前 42 字节空中包十六进制预览。");
  }

  function simulateReceive() {
    const nextBattery = clamp(batteryRef.current + (Math.random() > 0.5 ? 1 : -1), 30, 100);
    const nextRssi = clamp(rssiRef.current + Math.round(Math.random() * 6 - 3), -82, -35);
    const rxPacket = packetHex(buildDevicePacket(activeDevice, nextBattery));
    batteryRef.current = nextBattery;
    rssiRef.current = nextRssi;
    setBattery(nextBattery);
    setRssi(nextRssi);
    setLastRxAt(timestamp());
    addLog("rx", `收到设备端状态包，电量 ${nextBattery}%，RSSI ${nextRssi} dBm。${rxPacket.slice(0, 48)} ...`);
  }

  useEffect(() => {
    if (!isAdvertising) return undefined;
    const interval = window.setInterval(() => {
      txTickRef.current += 1;
      setTxCount((count) => count + 1);
      if (txTickRef.current % 100 === 0) {
        addLog("tx", `持续广播中，累计发送 ${txTickRef.current} 包。Byte22=${toHex(selectedMode)} Byte23=${toHex(packetBytes[22])}`);
      }
    }, 10);
    return () => window.clearInterval(interval);
  }, [isAdvertising, selectedMode, packetBytes]);

  useEffect(() => {
    const interval = window.setInterval(() => {
      if (isScanning) simulateReceive();
    }, 5000);
    return () => window.clearInterval(interval);
  }, [isScanning, activeDevice]);

  const tabs = [
    { key: "console", label: "控制台", icon: Radio },
    { key: "devices", label: "设备扫描", icon: Search },
    { key: "debug", label: "协议调试", icon: TerminalSquare }
  ];

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">RF2.4G pseudo BLE control prototype</p>
          <h1>RF2.4G 控制端</h1>
        </div>
        <div className="header-actions">
          <StatusPill active={isScanning} icon={Bluetooth} label={isScanning ? "扫描中" : "扫描暂停"} />
          <StatusPill active={isAdvertising} icon={Antenna} label={isAdvertising ? "广播中" : "广播停止"} />
        </div>
      </header>

      <nav className="tab-bar" aria-label="主页面">
        {tabs.map(({ key, label, icon: Icon }) => (
          <button key={key} className={tab === key ? "tab active" : "tab"} onClick={() => setTab(key)}>
            <Icon size={18} />
            <span>{label}</span>
          </button>
        ))}
      </nav>

      {tab === "console" && (
        <ConsoleView
          deviceId={deviceId}
          setDeviceId={setDeviceId}
          validId={validId}
          selectedMode={selectedMode}
          levels={levels}
          isAdvertising={isAdvertising}
          battery={battery}
          rssi={rssi}
          lastRxAt={lastRxAt}
          txCount={txCount}
          packetBytes={packetBytes}
          hexPacket={hexPacket}
          onStart={startAdvertising}
          onStop={stopAdvertising}
          onMode={selectMode}
          onStrength={updateStrength}
          onCopy={copyPacket}
          logs={logs}
        />
      )}

      {tab === "devices" && (
        <DevicesView
          devices={DEVICES}
          activeDevice={activeDevice}
          isScanning={isScanning}
          setIsScanning={setIsScanning}
          bindDevice={bindDevice}
          deviceId={deviceId}
          setDeviceId={setDeviceId}
          bindManual={() => bindDevice({ id: deviceId, mac: "00:00:00:00:00:00", rssi: -58, battery: 80, lastSeen: "手动" })}
          validId={validId}
        />
      )}

      {tab === "debug" && (
        <DebugView
          bytes={packetBytes}
          hexPacket={hexPacket}
          logs={logs}
          clearLogs={() => setLogs([])}
          copyPacket={copyPacket}
          simulateReceive={simulateReceive}
          simulateSend={() => addLog("tx", `手动模拟发送：${hexPacket}`)}
        />
      )}
    </main>
  );
}

function StatusPill({ active, icon: Icon, label }) {
  return (
    <span className={active ? "status-pill active" : "status-pill"}>
      <Icon size={16} />
      {label}
    </span>
  );
}

function Metric({ label, value, icon: Icon, tone = "default" }) {
  return (
    <div className={`metric ${tone}`}>
      <Icon size={18} />
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
    </div>
  );
}

function ConsoleView(props) {
  const {
    deviceId,
    setDeviceId,
    validId,
    selectedMode,
    levels,
    isAdvertising,
    battery,
    rssi,
    lastRxAt,
    txCount,
    packetBytes,
    hexPacket,
    onStart,
    onStop,
    onMode,
    onStrength,
    onCopy,
    logs
  } = props;

  return (
    <section className="console-grid">
      <div className="main-stack">
        <section className="panel status-panel">
          <div className="device-input">
            <label htmlFor="deviceId">目标设备 ID</label>
            <input id="deviceId" value={deviceId} onChange={(event) => setDeviceId(event.target.value.toUpperCase())} />
            <span className={validId ? "field-state ok" : "field-state error"}>{validId ? "格式有效" : "格式错误"}</span>
          </div>
          <div className="metrics-row">
            <Metric label="电量" value={`${battery}%`} icon={BatteryMedium} tone="green" />
            <Metric label="RSSI" value={`${rssi} dBm`} icon={Activity} tone="amber" />
            <Metric label="最近收包" value={lastRxAt} icon={Radio} />
            <Metric label="发送计数" value={txCount.toLocaleString()} icon={Gauge} />
          </div>
        </section>

        <section className="panel">
          <PanelTitle icon={Cpu} title="模式控制" subtitle="Byte22 模式字段" />
          <div className="mode-grid">
            {MODES.map((mode) => (
              <button
                key={mode.value}
                className={selectedMode === mode.value ? "mode-button active" : "mode-button"}
                onClick={() => onMode(mode.value, mode.label)}
              >
                <span>{mode.label}</span>
                <code>0x{toHex(mode.value)}</code>
              </button>
            ))}
          </div>
          <div className="action-row">
            {ACTIONS.map((action) => (
              <button
                key={action.value}
                className={`action-button ${action.tone} ${selectedMode === action.value ? "active" : ""}`}
                onClick={() => onMode(action.value, action.label)}
              >
                {action.label}
                <code>0x{toHex(action.value)}</code>
              </button>
            ))}
          </div>
        </section>

        <section className="panel">
          <PanelTitle icon={Settings2} title="强度控制" subtitle="Byte24~Byte29 控制字段" />
          <div className="slider-grid">
            {STRENGTHS.map((item) => (
              <StrengthSlider key={item.key} item={item} value={levels[item.key]} onChange={(value) => onStrength(item.key, value)} />
            ))}
            <div className="clip-control">
              <div className="slider-heading">
                <span>夹吸档位</span>
                <code>Byte27 = 0x{toHex(levels.clip)}</code>
              </div>
              <div className="segment-row">
                {Array.from({ length: 7 }, (_, index) => (
                  <button key={index} className={levels.clip === index ? "segment active" : "segment"} onClick={() => onStrength("clip", index)}>
                    {index}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </section>
      </div>

      <aside className="side-stack">
        <section className="panel send-panel">
          <PanelTitle icon={Antenna} title="广播发送" subtitle="模拟 10ms 重复发送" />
          <div className="send-actions">
            <button className="primary-button" onClick={isAdvertising ? onStop : onStart}>
              {isAdvertising ? <Square size={18} /> : <Play size={18} />}
              {isAdvertising ? "停止广播" : "开始广播"}
            </button>
            <button className="secondary-button" onClick={onCopy}>
              <Clipboard size={18} />
              复制包
            </button>
          </div>
          <div className="warning-line">
            <ShieldAlert size={16} />
            真机广播使用 Byte9~Byte39 的 31 字节 AdvData；头/MAC/CRC 由链路层处理。
          </div>
        </section>

        <PacketPreview bytes={packetBytes} hexPacket={hexPacket} />
        <LogPanel logs={logs.slice(0, 9)} compact />
      </aside>
    </section>
  );
}

function PanelTitle({ icon: Icon, title, subtitle }) {
  return (
    <div className="panel-title">
      <Icon size={20} />
      <div>
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
    </div>
  );
}

function StrengthSlider({ item, value, onChange }) {
  return (
    <div className="strength-control">
      <div className="slider-heading">
        <span>{item.label}</span>
        <code>Byte{item.byte} = 0x{toHex(value)}</code>
      </div>
      <div className="slider-line">
        <input type="range" min="0" max={item.max} value={value} onChange={(event) => onChange(event.target.value)} />
        <strong>{value}</strong>
      </div>
    </div>
  );
}

function PacketPreview({ bytes, hexPacket }) {
  const groups = [];
  for (let index = 0; index < bytes.length; index += 7) {
    groups.push(bytes.slice(index, index + 7));
  }

  return (
    <section className="panel packet-panel">
      <PanelTitle icon={Hexagon} title="发送包预览" subtitle="42 字节空中包 HEX" />
      <div className="hex-grid">
        {groups.map((group, groupIndex) => (
          <div key={groupIndex} className="hex-row">
            <span>{String(groupIndex * 7 + 1).padStart(2, "0")}</span>
            <code>{group.map(toHex).join(" ")}</code>
          </div>
        ))}
      </div>
      <div className="field-strip">
        <span>Byte22 0x{toHex(bytes[21])}</span>
        <span>Byte23 0x{toHex(bytes[22])}</span>
        <span>Byte40~42 55 55 55</span>
      </div>
      <textarea readOnly value={hexPacket} aria-label="当前发送包十六进制" />
    </section>
  );
}

function LogPanel({ logs, compact = false }) {
  return (
    <section className={compact ? "panel log-panel compact" : "panel log-panel"}>
      <PanelTitle icon={TerminalSquare} title="事件日志" subtitle="发送 / 接收 / 校验 / 异常" />
      <div className="log-list">
        {logs.length === 0 && <p className="empty">暂无日志</p>}
        {logs.map((log, index) => (
          <div key={`${log.text}-${index}`} className={`log-line ${log.type}`}>
            <span>{log.type.toUpperCase()}</span>
            <p>{log.text}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

function DevicesView({ devices, activeDevice, isScanning, setIsScanning, bindDevice, deviceId, setDeviceId, bindManual, validId }) {
  return (
    <section className="page-grid">
      <div className="panel">
        <PanelTitle icon={Search} title="模拟设备扫描" subtitle="设备端每 5s 状态广播" />
        <div className="toolbar-row">
          <button className={isScanning ? "primary-button" : "secondary-button"} onClick={() => setIsScanning(!isScanning)}>
            {isScanning ? <Square size={18} /> : <Play size={18} />}
            {isScanning ? "暂停扫描" : "开始扫描"}
          </button>
          <span className="hint">列表数据为原型模拟，用于确认绑定流程。</span>
        </div>
        <div className="device-list">
          {devices.map((device) => (
            <button
              key={device.id}
              className={activeDevice.id === device.id ? "device-row active" : "device-row"}
              onClick={() => bindDevice(device)}
            >
              <div>
                <strong>{device.id}</strong>
                <span>{device.mac}</span>
              </div>
              <span>{device.rssi} dBm</span>
              <span>{device.battery}%</span>
              <span>{device.lastSeen}</span>
              {activeDevice.id === device.id && <CheckCircle2 size={18} />}
            </button>
          ))}
        </div>
      </div>

      <div className="panel manual-panel">
        <PanelTitle icon={Radio} title="手动绑定" subtitle="设备 ID 作为核心匹配条件" />
        <label htmlFor="manualDevice">设备 ID</label>
        <input id="manualDevice" value={deviceId} onChange={(event) => setDeviceId(event.target.value.toUpperCase())} />
        <p className={validId ? "validation ok" : "validation error"}>{validId ? "格式有效，可绑定。" : "请输入 LX_DX001 这类 8 位 ID。"}</p>
        <button className="primary-button" onClick={bindManual} disabled={!validId}>
          绑定手动设备
        </button>
      </div>
    </section>
  );
}

function DebugView({ bytes, hexPacket, logs, clearLogs, copyPacket, simulateReceive, simulateSend }) {
  const fields = bytes.map((value, index) => ({
    byte: index + 1,
    value,
    name: fieldName(index + 1),
    source: fieldSource(index + 1)
  }));

  return (
    <section className="debug-grid">
      <div className="panel">
        <PanelTitle icon={ListRestart} title="协议字段表" subtitle="Byte1~Byte42 当前发送包" />
        <div className="debug-actions">
          <button className="primary-button" onClick={simulateSend}>
            <Antenna size={18} />
            模拟发送
          </button>
          <button className="secondary-button" onClick={simulateReceive}>
            <Radio size={18} />
            模拟接收
          </button>
          <button className="secondary-button" onClick={copyPacket}>
            <Clipboard size={18} />
            复制包
          </button>
          <button className="ghost-button" onClick={clearLogs}>
            <RotateCcw size={18} />
            清空日志
          </button>
        </div>
        <div className="field-table">
          <div className="field-head">
            <span>字节</span>
            <span>值</span>
            <span>字段</span>
            <span>来源</span>
          </div>
          {fields.map((field) => (
            <div key={field.byte} className={highlightByte(field.byte) ? "field-row highlight" : "field-row"}>
              <span>Byte{field.byte}</span>
              <code>0x{toHex(field.value)}</code>
              <span>{field.name}</span>
              <span>{field.source}</span>
            </div>
          ))}
        </div>
      </div>
      <div className="side-stack">
        <section className="panel checksum-panel">
          <PanelTitle icon={Zap} title="校验信息" subtitle="协议层校验结果" />
          <Metric label="APP 校验码" value={`0x${toHex(bytes[22])}`} icon={CheckCircle2} tone="green" />
          <Metric label="设备校验示例" value="MAC & 0xFF" icon={Cpu} />
          <Metric label="CRC 状态" value="链路层处理" icon={ShieldAlert} tone="red" />
          <div className="mono-block">{hexPacket}</div>
        </section>
        <LogPanel logs={logs} />
      </div>
    </section>
  );
}

function fieldName(byte) {
  if (byte === 1) return "广播类型";
  if (byte === 2) return "有效数据长度";
  if (byte >= 3 && byte <= 8) return "控制端 MAC";
  if (byte >= 9 && byte <= 11) return "BLE Flags";
  if (byte === 12) return "名称段长度";
  if (byte === 13) return "名称字段类型";
  if (byte >= 14 && byte <= 21) return "设备端 ID";
  if (byte === 22) return "模式定义";
  if (byte === 23) return "APP 校验码";
  if (byte === 24) return "震动强度";
  if (byte === 25) return "拍打强度";
  if (byte === 26) return "吮吸强度";
  if (byte === 27) return "夹吸档位";
  if (byte === 28) return "电击强度";
  if (byte === 29) return "加热强度";
  if (byte >= 30 && byte <= 39) return "保留";
  return "链路层 CRC";
}

function fieldSource(byte) {
  if (byte === 22) return "模式按钮";
  if (byte === 23) return "Byte14~22 求和";
  if (byte >= 24 && byte <= 29) return "强度控件";
  if (byte >= 14 && byte <= 21) return "设备 ID 输入";
  if (byte >= 40 && byte <= 42) return "链路层处理";
  return "协议固定值";
}

function highlightByte(byte) {
  return byte === 22 || byte === 23 || (byte >= 24 && byte <= 29) || byte >= 40;
}

export default App;
