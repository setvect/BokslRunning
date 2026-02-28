# 기술 스택 제안서 (Android MVP)

프로젝트: 복슬달리기(BokslRunning)  
기준 문서: `docs/PRD.md`  
대상: Android 단독 MVP (Kotlin)

## 1. 기술 스택 요약

- 언어/플랫폼: Kotlin, Android (minSdk 26+, targetSdk 최신)
- UI: Jetpack Compose + Material 3
- 아키텍처: MVVM + UDF(UI State) + Clean-ish Layer (`ui/domain/data`)
- 비동기/상태: Kotlin Coroutines + Flow + StateFlow
- 위치 추적: FusedLocationProviderClient + Foreground Service(`location` 타입)
- 지도/경로: Google Maps SDK for Android (Polyline)
- 로컬 저장소: Room(SQLite)
- 단순 설정 저장: DataStore (Preferences)
- 백그라운드 작업: WorkManager
- 차트: Vico(Compose) (대안: MPAndroidChart)
- 내보내기/가져오기: kotlinx.serialization (JSON)
- 의존성 주입: Hilt
- 로깅: Timber
- 테스트: JUnit5(or JUnit4), MockK, Turbine, Android Instrumentation Test
- 품질 도구: Detekt, Ktlint

## 2. 선정 이유 (PRD 기준)

- 러닝 중 기록 지속(Q3, Q4): Foreground Service 기반 위치 추적으로 OS 제약 상황에서 안정성 확보.
- 앱 중단 복구(Q1, P4): Room에 러닝 중 트랙 포인트/세션 상태를 주기 저장하여 마지막 저장 지점부터 복구.
- 오프라인 우선(C1, C2): 핵심 기능을 로컬 DB 중심으로 설계하여 네트워크 없이 동작.
- 지도 경로(B3, D2): Google Maps 기반 경로 시각화(Polyline)로 구현 단순화.
- 분석/월별 차트(E1, E2): Room 집계 쿼리 + 차트 라이브러리로 MVP 범위 충족.
- 전체 내보내기/가져오기(F, G): JSON 단일 파일 + `schema_version` 정책 반영 용이.

## 3. 권장 모듈 구조

단일 앱 모듈에서 시작하고, 필요 시 기능 모듈로 분리:

- `app` : 진입점, DI, Navigation
- `core` : 공통 유틸/에러/시간/단위 변환
- `data` : Room, DataStore, Repository 구현, Import/Export
- `domain` : UseCase, 모델, 정책(중복 판정/병합)
- `feature-home`
- `feature-run`
- `feature-history`
- `feature-stats`
- `feature-settings`

MVP 초기에는 `app + core + data + domain + feature-*`를 Gradle 모듈로 분리하지 않고 패키지 구조로 시작해도 무방.

## 4. 빌드/운영 기준

- 빌드: Gradle Kotlin DSL
- JDK: 17
- CI 최소 파이프라인:
  - `./gradlew ktlintCheck detekt test`
  - Debug APK 빌드 확인
- 릴리즈 전 수동 점검:
  - 10분 실주행/오프라인/화면 꺼짐/강제 종료 복구 시나리오(S1~S5)

## 5. 버전 가이드 (초기)

- Kotlin: stable 최신
- Compose BOM: stable 최신
- Android Gradle Plugin: stable 최신
- Room / Coroutines / Hilt / WorkManager: stable 최신

참고: 정확한 버전 숫자는 프로젝트 생성 시점의 안정 버전으로 고정하고, `libs.versions.toml`로 관리.
