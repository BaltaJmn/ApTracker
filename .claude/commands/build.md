# Build Commands

## Android Debug
```bash
./gradlew :composeApp:assembleDebug
```

## Android Release
```bash
./gradlew :composeApp:assembleRelease
```

## iOS
Build from Xcode using the `iosApp/` directory, or:
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Clean
```bash
./gradlew clean
```
