# 로컬 개발 셋업 가이드 (macOS, CLI 기준)

이 문서는 `BokslRunning` 프로젝트를 로컬에서 빌드/설치/실행하기 위한 최소 개발 환경 설정 절차를 정리한다.

## 1. 전제 조건

- OS: macOS
- 패키지 매니저: Homebrew
- JDK: 17 이상 (권장: Temurin 17)

JDK 확인:

```bash
java -version
/usr/libexec/java_home -V
```

JDK 17 설치(필요 시):

```bash
brew install --cask temurin17
```

## 2. Android CLI 도구 설치

```bash
brew install --cask android-commandlinetools
brew install --cask android-platform-tools
```

설치 확인:

```bash
find "$(brew --prefix)/share" -type f -name sdkmanager 2>/dev/null
find "$(brew --prefix)/share" -type f -name avdmanager 2>/dev/null
which adb
```

## 3. 쉘 환경변수 설정 (`~/.zshrc`)

```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
echo 'export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk' >> ~/.zshrc
echo 'export ANDROID_HOME=$ANDROID_SDK_ROOT' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
echo 'export PATH="$PATH:/opt/homebrew/share/android-commandlinetools/cmdline-tools/latest/bin"' >> ~/.zshrc
echo 'export PATH="$PATH:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator"' >> ~/.zshrc
source ~/.zshrc
```

확인:

```bash
echo $JAVA_HOME
echo $ANDROID_SDK_ROOT
which sdkmanager
which avdmanager
which adb
```

## 4. SDK 패키지 설치

중요: `sdkmanager`는 JDK 17로 실행되어야 한다.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk

yes | sdkmanager --licenses
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
  "platform-tools" \
  "emulator" \
  "platforms;android-34" \
  "build-tools;34.0.0" \
  "system-images;android-34;google_apis;arm64-v8a"
```

설치 확인:

```bash
sdkmanager --list_installed | grep -E "platform-tools|emulator|platforms;android-34|build-tools;34.0.0|system-images;android-34;google_apis;arm64-v8a"
```

## 5. AVD 생성 (Galaxy S21+ 유사)

```bash
echo "no" | avdmanager create avd \
  -n Galaxy_S21_Plus_API_34 \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d pixel_6 \
  --force
```

### 5.1 S21+ 유사 스펙 보정 (선택)

`~/.android/avd/Galaxy_S21_Plus_API_34.avd/config.ini` 에 아래 값이 반영되도록 조정:

- `hw.lcd.width=1080`
- `hw.lcd.height=2400`
- `hw.lcd.density=393`
- `hw.lcd.vsync=120`
- `hw.cpu.ncore=8`
- `hw.ramSize=8192`
- `disk.dataPartition.size=8G`

## 6. 에뮬레이터 실행

```bash
export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk
"$ANDROID_SDK_ROOT/emulator/emulator" -avd Galaxy_S21_Plus_API_34 -no-snapshot-load
```

연결 확인:

```bash
"$ANDROID_SDK_ROOT/platform-tools/adb" devices
```

`emulator-5554    device` 가 보여야 설치 가능.

## 7. 프로젝트 빌드/설치/실행

프로젝트 루트: `/Users/boksl/IdeaProjects/BokslRunning`

```bash
cd /Users/boksl/IdeaProjects/BokslRunning
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew installDebug
```

앱 실행:

```bash
export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk
"$ANDROID_SDK_ROOT/platform-tools/adb" shell am start -n com.boksl.running/.MainActivity
```

## 8. 프로젝트 로컬 설정 파일

`local.properties`:

```properties
sdk.dir=/Users/boksl/Library/Android/sdk
```

주의:

- `local.properties`는 로컬 환경 전용 파일이다.
- 저장소 공용 파일(`gradle.properties`, `gradlew`)에 로컬 경로를 하드코딩하지 않는다.

## 9. 트러블슈팅

### 9.1 `SDK location not found`

원인: `local.properties` 누락  
해결: `sdk.dir` 경로 추가

### 9.2 `This tool requires JDK 17 or later`

원인: 셸이 Java 8 사용 중  
해결:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
```

### 9.3 `No connected devices!`

원인: 에뮬레이터가 ADB에 연결되지 않음  
해결:

```bash
"$ANDROID_SDK_ROOT/platform-tools/adb" devices
"$ANDROID_SDK_ROOT/platform-tools/adb" wait-for-device
```

`device` 상태 확인 후 `installDebug` 실행.

### 9.4 `Broken AVD system path`

원인: `ANDROID_SDK_ROOT`와 실제 system image 경로 불일치  
해결: `sdkmanager --sdk_root="$ANDROID_SDK_ROOT"`로 재설치 후 AVD 재생성

### 9.5 에뮬레이터 사이드바 Home/Back 버튼이 클릭되지 않음

현상: 사이드 툴바 버튼 클릭 입력이 불안정한 경우가 있음  
우회:

- 키보드 단축키 사용 (`Esc` 등)
- ADB keyevent 사용:
  - Home: `adb shell input keyevent 3`
  - Back: `adb shell input keyevent 4`
  - Recent: `adb shell input keyevent 187`

## 10. 에뮬레이터 종료

```bash
"$ANDROID_SDK_ROOT/platform-tools/adb" emu kill
```

