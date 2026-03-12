# 명령어 가이드

이 문서는 `BokslRunning` 프로젝트에서 자주 사용하는 실행, 빌드, 테스트, 시뮬레이션 명령어를 정리한다.

## 1. 로컬 실행

Android Studio에서 `app` 실행을 권장한다.

CLI로 설치/실행:

```bash
./gradlew installDebug
adb shell am start -n com.boksl.running/.MainActivity
```

CLI로 AVD 실행:

```bash
$ANDROID_SDK_ROOT/emulator/emulator -list-avds
$ANDROID_SDK_ROOT/emulator/emulator -avd <AVD_NAME> -no-snapshot-load
adb wait-for-device
adb shell getprop sys.boot_completed
```

## 2. 정적 분석

Android Lint:

```bash
./gradlew lint
```

Ktlint 검사:

```bash
./gradlew ktlintCheck
```

Detekt 검사:

```bash
./gradlew detekt
```

권장 품질 게이트:

```bash
./gradlew lint ktlintCheck detekt
```

## 3. 컴파일

Debug Kotlin 컴파일:

```bash
./gradlew compileDebugKotlin
```

Release Kotlin 컴파일:

```bash
./gradlew compileReleaseKotlin
```

## 4. 빌드

Debug APK 생성:

```bash
./gradlew assembleDebug
```

Release APK 생성:

```bash
./gradlew assembleRelease
```

전체 빌드:

```bash
./gradlew build
```

## 5. 테스트

Unit Test (Debug):

```bash
./gradlew testDebugUnitTest
```

계측 테스트:

```bash
./gradlew connectedDebugAndroidTest
```

## 6. 러닝 시뮬레이션

에뮬레이터에서 앱 설치/실행 후 `러닝 준비` 화면까지 진입한 다음 사용한다.

기본 경로 재생:

```bash
./scripts/simulate_run.sh
```

재생 속도 배속:

```bash
./scripts/simulate_run.sh --speed 2.0
```

특정 에뮬레이터 지정:

```bash
./scripts/simulate_run.sh --serial emulator-5554
```

샘플 경로 파일:

- CSV: `tools/simulation/sample_run_loop.csv`
- GPX: `tools/simulation/sample_run_loop.gpx`

Android Studio 에뮬레이터 수동 재생:

- `Extended controls > Location > Routes`에서 `tools/simulation/sample_run_loop.gpx` 선택
