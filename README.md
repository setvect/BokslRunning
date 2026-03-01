# BokslRunning

현재 프로젝트에서 자주 사용하는 Lint/컴파일/빌드 명령어 모음입니다.

## 환경

- JDK 17
- Gradle Wrapper 사용: `./gradlew`
- Android SDK / Platform Tools (`adb`) 설치

## 로컬 실행

- Android Studio에서 실행 (권장)
```bash
# 1) Android Studio로 프로젝트 열기
# 2) 실행할 에뮬레이터 또는 실기기 선택
# 3) Run 'app' 실행
```

- CLI로 설치/실행
```bash
# APK 빌드 + 디버그 기기에 설치
./gradlew installDebug

# 앱 실행 (패키지/액티비티 기준)
adb shell am start -n com.boksl.running/.MainActivity
```

- CLI로 AVD 실행
```bash
# AVD 목록 확인
$ANDROID_SDK_ROOT/emulator/emulator -list-avds

# AVD 실행
$ANDROID_SDK_ROOT/emulator/emulator -avd <AVD_NAME> -no-snapshot-load

# 부팅 완료 대기/확인
adb wait-for-device
adb shell getprop sys.boot_completed
```

## Lint / 정적 분석

- Android Lint
```bash
./gradlew lint
```

- Ktlint 검사
```bash
./gradlew ktlintCheck
```

- Detekt 검사
```bash
./gradlew detekt
```

- 품질 게이트(권장)
```bash
./gradlew lint ktlintCheck detekt
```

## 컴파일

- Debug Kotlin 컴파일
```bash
./gradlew compileDebugKotlin
```

- Release Kotlin 컴파일
```bash
./gradlew compileReleaseKotlin
```

## 빌드

- Debug APK 생성
```bash
./gradlew assembleDebug
```

- Release APK 생성
```bash
./gradlew assembleRelease
```

- 전체 빌드(+테스트)
```bash
./gradlew build
```

## 테스트

- Unit Test (Debug)
```bash
./gradlew testDebugUnitTest
```

- 계측 테스트(디바이스/에뮬레이터 필요)
```bash
./gradlew connectedDebugAndroidTest
```
