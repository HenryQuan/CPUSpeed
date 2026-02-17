# CPUSpeed v1.1.0
Set CPU speed easily for rooted Android devices using a modern, native Kotlin + Jetpack Compose app.

## Overview
CPUSpeed is a simple, lightweight app that allows you to control your Android device's CPU frequency. This can help you optimize battery life (by underclocking) or improve performance (by ensuring maximum frequencies).

## Version 1.1.0 - Complete Rewrite
The app has been completely rewritten from the ground up using modern Android development practices:

### Technology Stack
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Build System**: Gradle 8.9 with Kotlin DSL (.kts)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)

### Modern Architecture
```
├── CPUManager.kt       - Handles all CPU operations with proper error handling
├── CPUViewModel.kt     - MVVM ViewModel with StateFlow and Coroutines
└── MainActivity.kt     - Pure Jetpack Compose UI with Material 3 design
```

### Key Features
- ✅ **Modern UI**: Built with Jetpack Compose and Material 3 design system
- ✅ **Reactive**: Uses Kotlin Flows for reactive state management
- ✅ **Error Handling**: Comprehensive device compatibility checks
- ✅ **Root Detection**: Automatic detection with clear error messages
- ✅ **Safe**: Warning dialogs to prevent accidental device damage
- ✅ **Lightweight**: Pure Kotlin implementation, no Flutter overhead

### Build Configuration
The project uses state-of-the-art Gradle configuration:
- Kotlin DSL build scripts (`.gradle.kts`)
- Version catalog for centralized dependency management (`libs.versions.toml`)
- Jetpack Compose BOM for consistent Compose versions
- Modern Gradle best practices

## Download
- [Google Play](https://play.google.com/store/apps/details?id=com.yihengquan.cpuspeed)
- [GitHub Releases](https://github.com/HenryQuan/CPUSpeed/releases/latest)

## Requirements
- **Rooted Android device** (SU or Busybox)
- Android 8.0 (API 26) or higher
- Compatible CPU frequency scaling interface

## Building from Source
```bash
cd cpuspeed
./gradlew assembleDebug
```

## Legacy Versions
Previous implementations are preserved in the `legacy/` directory:
- `legacy/flutter/` - Flutter + Native hybrid implementation (v1.0.x)
- `legacy/native/` - Original pure Kotlin implementation with XML layouts
- `legacy/flutter-module/` - Flutter UI module

## Motivation
This app was created because other CPU control apps were either paid or didn't work on certain devices. CPUSpeed aims to provide a simple, free, and open-source solution for controlling CPU frequencies on rooted Android devices.

### Use Cases
- **Battery Life**: Underclock your CPU to extend battery life
- **Performance**: Ensure your CPU runs at maximum frequency for demanding tasks
- **Thermal Management**: Reduce CPU frequency to prevent overheating
- **Emulation**: Fine-tune CPU performance for gaming and emulation

## ⚠️ Warning
- **Underclocking** may cause your device to freeze or shut down
- **Overclocking** may cause excessive heat and rapid battery drain
- This app modifies system files and requires root access
- Use at your own risk!

## Contributing
Contributions are welcome! Please feel free to submit issues or pull requests.

## License
This project is open source. See the LICENSE file for details.

## Privacy
See [Privacy Policy](Privacy%20Policy.md) for information about data handling.
