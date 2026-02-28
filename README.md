# BokslRunning

현재 프로젝트에서 자주 사용하는 Lint/컴파일/빌드 명령어 모음입니다.

## 환경

- JDK 17
- Gradle Wrapper 사용: `./gradlew`

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
