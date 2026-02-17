# CPUSpeed Architecture Documentation

## Version 1.1.0 - Modern Kotlin + Jetpack Compose

### Overview
CPUSpeed has been completely rewritten to use modern Android development practices, moving from Flutter to pure Kotlin with Jetpack Compose.

## Technology Stack

### Core Technologies
- **Language**: Kotlin 2.3.0
- **UI Framework**: Jetpack Compose
- **Design System**: Material 3
- **Build Tool**: Gradle 8.9 with Kotlin DSL
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)

### Dependencies Management
The project uses a **version catalog** (`gradle/libs.versions.toml`) for centralized dependency management:
- Jetpack Compose BOM 2024.12.01
- AndroidX Core KTX 1.15.0
- AndroidX Lifecycle 2.8.7
- Activity Compose 1.9.3

## Architecture Pattern: MVVM with Enhanced Root Layer

### Layer Separation

```
┌─────────────────────────────────────┐
│         UI Layer (Compose)          │
│      MainActivity.kt                │
│  - Composable functions             │
│  - UI state observation             │
│  - User interactions                │
└──────────────┬──────────────────────┘
               │ observes StateFlow
               ▼
┌─────────────────────────────────────┐
│      ViewModel Layer                │
│      CPUViewModel.kt                │
│  - UI state management              │
│  - Business logic coordination      │
│  - Coroutines & StateFlow           │
└──────────────┬──────────────────────┘
               │ uses
               ▼
┌─────────────────────────────────────┐
│       Manager Layer                 │
│      CPUManager.kt                  │
│  - CPU operations                   │
│  - Governor management (prepared)   │
│  - Power management (prepared)      │
└──────────────┬──────────────────────┘
               │ uses
               ▼
┌─────────────────────────────────────┐
│       Root Execution Layer          │
│      RootManager.kt                 │
│  - Root command execution           │
│  - File operations with root        │
│  - Error handling & logging         │
└─────────────────────────────────────┘
```

## File Structure

### Core Files

#### `RootManager.kt` (New)
Enhanced root command execution layer:
- **Root Detection**: Checks for `su`, `busybox`, and `magisk` binaries
- **Command Execution**: Single commands or batch execution
- **File Operations**: Read/write files with root privileges
- **Error Handling**: Proper exit code checking and error propagation
- **Logging**: Comprehensive logging for debugging

Key Methods:
- `isRootAvailable(): Boolean`
- `executeCommand(command: String): Result<String>`
- `executeCommands(commands: List<String>): Result<Unit>`
- `readFile(path: String): Result<String>`
- `writeFile(path: String, content: String): Result<Unit>`
- `fileExists(path: String): Boolean`
- `setPermissions(path: String, permissions: String): Result<Unit>`

#### `CPUManager.kt` (Enhanced)
Manages CPU operations with future extensibility:
- **CPU Frequency Control**: Read and set min/max frequencies
- **Governor Support**: Read current/available governors (prepared for control)
- **Performance Parameters**: Qualcomm MSM-specific parameters
- **Device Compatibility**: Validates CPU frequency interface
- **Future-Ready**: Structure prepared for power management features

Key Methods:
- `isDeviceRooted(): Boolean`
- `getCPUInfo(): CPUInfo` - Returns comprehensive CPU information including governor data
- `setCPUSpeed(maxSpeed: Int, minSpeed: Int): Result<Unit>`
- `setCPUGovernor(governor: String): Result<Unit>` - Prepared for future use

New CPUInfo Fields:
- `currentGovernor: String?` - Current CPU governor
- `availableGovernors: List<String>?` - Available governors for switching

#### `CPUViewModel.kt`
Manages UI state and coordinates business logic:
- **State Management**: Uses StateFlow for reactive UI updates
- **Async Operations**: Leverages Kotlin Coroutines for background work
- **State Types**:
  - `Loading`: Initial device check
  - `Success`: Device supported, ready to use
  - `Error`: Device not supported or not rooted

Key Properties:
- `uiState: StateFlow<UIState>`
- `maxSpeed: StateFlow<Float>`
- `minSpeed: StateFlow<Float>`

Key Methods:
- `updateMaxSpeed(value: Float)`
- `updateMinSpeed(value: Float)`
- `applyCPUSpeed(onSuccess: () -> Unit, onError: (String) -> Unit)`

#### `MainActivity.kt`
Pure Jetpack Compose UI implementation:
- **Composable Screens**:
  - `CPUSpeedApp`: Main app container with Scaffold
  - `CPUControlScreen`: CPU frequency control interface
  - `ErrorScreen`: Error display for unsupported devices
  - `SliderCard`: Reusable slider component
  - `WelcomeDialog`: First-launch warning
  - `AboutDialog`: App information and links

Features:
- Material 3 design
- Dynamic color scheme
- Reactive UI updates via StateFlow observation
- Edge-to-edge display support

## Build Configuration (Modern KTS Approach)

### Root `build.gradle.kts`
```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    }
}
```

### `app/build.gradle.kts`
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.yihengquan.cpuspeed"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 26
        targetSdk = 35
        versionCode = 1100
        versionName = "1.1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

### Version Catalog (`gradle/libs.versions.toml`)
Centralized dependency versioning following SOTA practices:
```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.21"
composeBom = "2024.12.01"
# ... other versions

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
# ... other dependencies

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

## Data Flow

### Reading CPU Information
```
User Opens App
       ↓
ViewModel.init()
       ↓
CPUManager.getCPUInfo()
       ↓
Execute shell command: su -c cat /sys/devices/system/cpu/cpu*/cpufreq/*m*_freq
       ↓
Parse output & validate format
       ↓
Return CPUInfo data class
       ↓
ViewModel updates StateFlow
       ↓
UI recomposes with new data
```

### Setting CPU Speed
```
User adjusts slider & clicks Apply
       ↓
ViewModel.applyCPUSpeed()
       ↓
CPUManager.setCPUSpeed(max, min)
       ↓
Generate chmod & echo commands for each core
       ↓
Execute commands via su
       ↓
Return Result<Unit>
       ↓
ViewModel calls success/error callback
       ↓
UI shows Toast notification
```

## Error Handling Strategy

### Device Compatibility
1. **Root Check**: Verify SU binary exists
2. **CPU Interface Check**: Validate frequency files are accessible
3. **Data Format Check**: Ensure expected output format
4. **Graceful Degradation**: Clear error messages for users

### Error Types
- **Not Rooted**: Device doesn't have root access
- **Unsupported**: CPU frequency interface not available
- **Parse Error**: Unexpected data format
- **Command Failure**: Root command execution failed

## Testing Strategy

### Unit Testing
- CPUManager business logic
- CPUViewModel state management
- Data parsing and validation

### Integration Testing
- ViewModel + Manager interaction
- Coroutine execution
- StateFlow updates

### UI Testing
- Compose UI tests
- User interaction flows
- Error state handling

## Security Considerations

1. **Root Access**: Only used for CPU frequency control
2. **Minimal Permissions**: No internet, location, or storage access
3. **User Warnings**: Clear dialogs about risks
4. **No Data Collection**: Complete privacy, no analytics

## Performance Optimizations

1. **Lazy Loading**: ViewModel initialization on demand
2. **Coroutines**: Non-blocking async operations
3. **StateFlow**: Efficient state updates
4. **Compose**: Declarative UI with smart recomposition

## Future Enhancements

### Planned Power Management Features
The architecture is now prepared to support:

#### 1. CPU Governor Control
- Switch between available governors (interactive, performance, powersave, conservative, ondemand)
- Per-core governor selection
- Governor profiles with preset configurations
- Real-time governor switching

#### 2. Power Management
- Thermal monitoring and control
- Battery-aware frequency scaling
- Power consumption estimation
- Idle state management

#### 3. Advanced Frequency Control
- Per-core frequency management (big.LITTLE/DynamIQ support)
- Frequency boost control
- Frequency locking and pinning
- Custom frequency stepping

#### 4. Profiles and Automation
- CPU frequency profiles (Battery Saver, Balanced, Performance, Gaming)
- Scheduled profile switching
- App-based profile triggers
- Battery level-based automatic switching

#### 5. Monitoring and Statistics
- Real-time CPU frequency monitoring
- Temperature monitoring
- Power consumption tracking
- Historical statistics and graphs
- Core usage visualization

### Technical Roadmap

#### Phase 1: Enhanced Root Layer ✅
- [x] Implement RootManager with robust command execution
- [x] Add proper error handling and logging
- [x] Support multiple root binaries
- [x] File operations with root privileges

#### Phase 2: Governor Control (Next)
- [ ] UI for governor selection
- [ ] Governor switching implementation
- [ ] Governor information display
- [ ] Governor profiles

#### Phase 3: Power Features
- [ ] Thermal monitoring integration
- [ ] Power consumption estimation
- [ ] Battery-aware scaling
- [ ] Power profiles

#### Phase 4: Advanced Features
- [ ] Per-core frequency control
- [ ] Real-time monitoring
- [ ] Statistics and graphs
- [ ] Widget support

### Code Structure for Future Features

The codebase is designed to easily accommodate new features:

```kotlin
// Already prepared in CPUManager.kt
fun getCurrentGovernor(): String?
fun getAvailableGovernors(): List<String>?
fun setCPUGovernor(governor: String): Result<Unit>

// Future additions
fun getThermalZones(): List<ThermalZone>
fun getCurrentTemperature(): Float
fun setPowerProfile(profile: PowerProfile): Result<Unit>
fun getCoreFrequencies(): Map<Int, Int>
fun setPerCoreFrequency(core: Int, frequency: Int): Result<Unit>
```

## Migration Guide (from Legacy)

### For Users
- Cleaner, more modern UI
- Better error messages
- Faster and more responsive
- Smaller app size (no Flutter overhead)

### For Developers
- Pure Kotlin codebase
- Modern Android architecture
- Easier to maintain and extend
- Better testing infrastructure
- Standard Android development practices

## Build and Release

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Version Bumping
Update version in `app/build.gradle.kts`:
```kotlin
versionCode = 1100  // Increment by 1
versionName = "1.1.0"  // Semantic versioning
```

## Resources

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material 3 Design](https://m3.material.io/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Gradle Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
