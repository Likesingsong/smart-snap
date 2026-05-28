# SmartSnap - 智能拍照识物

一款 Android 应用，用户拍照后自动识别照片中最明显的 2-3 个物品，展示物品名称和置信度。若无法识别则提示"识别失败"。

## 核心功能

- 📷 调用系统相机拍照
- 🤖 本地离线识别物品（ML Kit）
- 📊 显示置信度最高的 2-3 个物品名称
- ⚠️ 识别失败时友好提示

## 技术栈

| 模块 | 技术方案 | 版本 |
|------|---------|------|
| 开发语言 | Kotlin | 1.9+ |
| 最低 SDK | Android 7.0 | API 24 |
| 编译 SDK | Android 14 | API 34 |
| UI 框架 | Jetpack Compose | 1.5+ |
| 架构模式 | MVVM | - |
| 相机 | CameraX | 1.3.1 |
| 物体检测 | ML Kit Object Detection | 17.0.1 |
| 图像分类 | ML Kit Image Labeling | 17.0.7 |
| 异步处理 | Kotlin Coroutines | 1.7.3 |
| 依赖注入 | Hilt | 2.48 |
| 日志 | Timber | 5.0.1 |

## 项目结构

```
app/src/main/java/com/example/smartsnap/
├── MainApplication.kt              # Application 入口（Hilt + Timber）
├── MainActivity.kt                  # Activity 入口（Compose）
├── camera/
│   └── CameraManager.kt             # CameraX 封装（预览/拍照/资源释放）
├── data/
│   ├── config/
│   │   └── RecognitionConfig.kt     # 识别参数配置
│   └── repository/
│       └── ObjectRecognitionRepository.kt  # 识别引擎
├── di/
│   └── AppModule.kt                 # Hilt 依赖注入
├── domain/
│   └── model/
│       └── RecognizedItem.kt        # 识别结果数据类
├── ui/
│   ├── screen/
│   │   └── CameraScreen.kt          # 主界面 Composable
│   ├── state/
│   │   └── RecognitionUiState.kt    # UI 状态定义
│   ├── theme/
│   │   └── Theme.kt                 # Material3 主题
│   └── viewmodel/
│       └── CameraViewModel.kt       # ViewModel
└── util/
    ├── BitmapUtils.kt               # 图片工具类
    └── PermissionUtils.kt           # 权限工具类
```

## 识别流水线

```
用户点击拍照
    │
    ▼
CameraX 获取图像 (ImageProxy)
    │
    ▼
压缩至 1024px 宽 → InputImage
    │
    ▼
ObjectDetector 检测物体 ──── 获得检测框列表
    │
    ▼ (遍历每个检测框)
裁剪检测区域 → ImageLabeler 分类
    │
    ▼
ResultProcessor 处理结果
    │ (过滤、排序、取 Top 2-3)
    ▼
更新 UI 状态
    ├─ 成功 ── 显示物品名称 + 置信度
    └─ 失败 ── 显示"识别失败"
```

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android 设备（API 24+，需后置摄像头）

## 构建与运行

### 1. 克隆项目

```bash
git clone git@github.com:Likesingsong/smart-snap.git
cd smart-snap
```

### 2. 命令行构建

```bash
./gradlew assembleDebug
```

生成的 Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

### 3. 安装到设备

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Android Studio 运行

用 Android Studio 打开项目，连接设备后点击 Run 即可。

## 权限说明

| 权限 | 用途 | 时机 |
|------|------|------|
| `CAMERA` | 拍照识别物品 | 进入拍照界面时请求 |

## 注意事项

- ML Kit 基础模型首次使用时会自动下载，需要网络连接
- 模型下载完成后可完全离线使用
- Debug APK 使用 debug 签名，仅用于测试

## License

MIT
