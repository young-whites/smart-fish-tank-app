# 智能鱼缸控制 APP (Intelligent Fish Tank Control)

## 项目简介

基于 Kotlin + Jetpack Compose 开发的 Android 智能鱼缸远程控制应用。通过 TCP Socket 直连 ESP-01S WiFi 模组，实现传感器数据实时显示、继电器远程控制、阈值参数设置、自动喂食触发等功能。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | — | 主开发语言 |
| Jetpack Compose | BOM 2024.x | 声明式 UI 框架 |
| Navigation Compose | 2.8.9 | 页面导航 |
| ViewModel | 2.10.0 | 状态管理 |
| Lifecycle Runtime Compose | 2.10.0 | 生命周期感知的 State 收集 |
| Coroutines / Flow | — | 异步通信与响应式数据流 |
| TCP Socket | java.net | 网络通信 |

### 构建配置

- `compileSdk`: 36
- `minSdk`: 34
- `targetSdk`: 36
- `applicationId`: com.example.intelligentfish

## 功能模块

### 🏠 首页 (Dashboard)

- 实时显示传感器数据：水温、PH 值、水位、空气质量
- 显示设备状态：运行模式、继电器状态、喂食倒计时
- 连接状态指示

### 🎛 控制 (Control)

- 手动/自动模式切换
- 4 路继电器独立控制（加热、加水、排水、增氧）
- 一键触发喂食
- 报警开关控制
- 喂食间隔设置

### ⚙️ 设置 (Settings)

- WiFi 设备扫描与连接
- 阈值参数同步（温度上下限、水位上下限、PH 上下限、空气质量上限）
- 连接管理

### 🧪 测试 (Test)

- TCP 帧日志实时查看
- 手动发送 Hex 帧调试
- 快捷测试命令（Echo、OK）
- 连接/断开控制

## 通信协议

### 连接方式

```
Android APP ──TCP Socket──▶ ESP-01S (TCP Server :8080) ──UART──▶ STM32
```

### 帧格式

```
┌──────┬─────┬─────┬─────────┬─────┬──────┐
│ HEAD │ CMD │ LEN │ PAYLOAD │ SUM │ END  │
│ 0xAA │ 1B  │ 1B  │   NB    │ 1B  │ 0x55 │
└──────┴─────┴─────┴─────────┴─────┴──────┘

校验和 = CMD + LEN + 所有 payload 字节之和 (取低8位)
```

### 命令字

| CMD | 方向 | 说明 |
|-----|------|------|
| 0x01 | MCU→APP | 传感器数据上报（14 字节 payload） |
| 0x02 | MCU→APP | 设备状态上报（5 字节 payload） |
| 0x03 | MCU→APP | 报警事件 |
| 0x10 | APP→MCU | 查询状态 |
| 0x11 | APP→MCU | 切换运行模式（自动/手动） |
| 0x12 | APP→MCU | 控制继电器 |
| 0x13 | APP→MCU | 同步阈值参数 |
| 0x14 | APP→MCU | 触发喂食 |
| 0x15 | APP→MCU | 设置报警开关 |
| 0x16 | APP→MCU | 设置喂食间隔 |
| 0x20 | APP→MCU | LED 控制 |

## 目录结构

```
Intelligent-fish-tank-control/
├── app/src/main/java/com/example/intelligentfish/
│   ├── MainActivity.kt                  # 入口 Activity
│   ├── navigation/
│   │   └── NavGraph.kt                  # 导航图定义（4 个页面）
│   ├── data/
│   │   ├── model/
│   │   │   ├── SensorData.kt            # 传感器数据模型
│   │   │   ├── DeviceStatus.kt          # 设备状态模型
│   │   │   ├── ThresholdSet.kt          # 阈值参数模型
│   │   │   └── AlarmEvent.kt            # 报警事件模型
│   │   ├── net/
│   │   │   ├── Protocol.kt              # 通信协议定义（帧解析/构建）
│   │   │   ├── TcpClient.kt             # TCP 客户端（自动重连）
│   │   │   └── WifiScanner.kt           # WiFi 扫描工具
│   │   └── repository/
│   │       └── FishTankRepository.kt    # 数据仓库层
│   ├── viewmodel/
│   │   └── FishTankViewModel.kt         # 主 ViewModel
│   └── ui/
│       ├── dashboard/
│       │   └── DashboardScreen.kt       # 首页 Composable
│       ├── control/
│       │   └── ControlScreen.kt         # 控制页 Composable
│       ├── settings/
│       │   └── SettingsScreen.kt        # 设置页 Composable
│       ├── test/
│       │   └── TestScreen.kt            # 测试页 Composable
│       ├── components/
│       │   ├── ConnectionBar.kt         # 连接状态栏组件
│       │   └── DataCard.kt              # 数据卡片组件
│       └── theme/
│           ├── Color.kt                 # 颜色定义
│           ├── Theme.kt                 # 主题配置
│           └── Type.kt                  # 字体配置
├── app/src/main/res/                    # 资源文件
├── build.gradle                         # 模块构建配置
├── build.gradle                         # 项目构建配置
└── settings.gradle                      # 项目设置
```

## 编译与运行

### 环境要求

- Android Studio Hedgehog (2024.1) 或更高版本
- JDK 11+
- Android SDK 36
- 设备/模拟器：Android 14 (API 34) 或更高

### 编译步骤

1. 使用 Android Studio 打开项目根目录
2. 等待 Gradle Sync 完成
3. 连接 Android 设备或启动模拟器
4. 点击 Run ▶ 编译并安装

### 使用方法

1. 确保 ESP-01S 已上电并建立 TCP Server (端口 8080)
2. 手机连接到 ESP-01S 所在的 WiFi 网络
3. 打开 APP → 设置页面 → 扫描并连接设备
4. 连接成功后即可在首页查看数据、控制页面操作继电器

## 数据模型

### SensorData（传感器数据）

| 字段 | 类型 | 说明 |
|------|------|------|
| waterTemp | Float | 水温 °C |
| phValue | Float | PH 值 |
| waterLevel | Int | 水位 0~100% |
| airQuality | Int | 空气质量 |

### DeviceStatus（设备状态）

| 字段 | 类型 | 说明 |
|------|------|------|
| mode | WorkMode | 运行模式 (AUTO/MANUAL) |
| relays | List<Boolean> | 4 路继电器状态 |
| feeding | Boolean | 喂食状态 |
| feedCountdown | Int | 喂食倒计时 |
| alarmEnabled | Boolean | 报警使能 |

### ThresholdSet（阈值参数）

| 字段 | 类型 | 说明 |
|------|------|------|
| tempLower | Float | 温度下限 |
| tempUpper | Float | 温度上限 |
| waterLevelMin | Int | 水位下限 % |
| waterLevelMax | Int | 水位上限 % |
| airQualityMax | Int | 空气质量上限 |
| phLower | Float | PH 下限 |
| phUpper | Float | PH 上限 |

## License

本项目仅供学习和毕业设计参考。

---

*最后更新：2026-05-19*
