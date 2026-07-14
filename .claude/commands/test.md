# Test Commands

## All unit tests
```bash
./gradlew :composeApp:testDebugUnitTest
```

## Single test class
```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.baltajmn.aptracker.ClassName"
```

## Single test method
```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.baltajmn.aptracker.ClassName.methodName"
```
