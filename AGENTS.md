# AGENTS.md

## 목적
- 이 문서는 초반 바이브 코딩을 위한 최소 작업 규칙만 정의한다.
- 상세 요구사항/화면/정책은 아래 기준 문서를 우선 참조한다.

## 기준 문서
- `docs/PRD.md`
- `docs/storyboard.md`
- `docs/tech-stack.md`
- `docs/implementation-plan.md`

## 작업 원칙
- MVP 범위 안에서만 구현한다.
- 구현 순서는 `docs/implementation-plan.md`의 Phase를 따른다.
- 큰 변경 시 해당 Phase 체크박스를 갱신한다.
- 문서와 코드가 충돌하면 PRD 기준으로 맞춘다.

## Android 공통 설정
- 플랫폼/언어: Kotlin, Android `minSdk 26+`, `targetSdk`는 프로젝트 기준 버전을 사용하고 주기적으로 업데이트한다.
- UI: Jetpack Compose + Material 3를 기본으로 사용한다.
- 아키텍처: `ui/domain/data` 분리, `MVVM + UDF(UI State)`를 유지한다.
- 상태/비동기: Kotlin Coroutines + Flow/StateFlow를 기본으로 사용한다.
- DI: Hilt를 기본 DI로 사용한다.
- 로컬 저장: Room(SQLite), 단순 키-값은 DataStore(Preferences)를 사용한다.
- 백그라운드/예약 작업: WorkManager를 사용한다.
- 위치 추적: Foreground Service(`location` 타입) + FusedLocationProviderClient를 사용한다.
- 지도: Google Maps SDK for Android + Polyline으로 경로를 표시한다.
- 모듈 전략: MVP 초기에는 단일 모듈 기반 패키지 분리, 필요 시 기능 모듈로 확장한다.

## Android 개발 규칙
- 신규 기능은 `docs/implementation-plan.md`의 Phase/작업 단위를 기준으로 구현한다.
- 화면 구현 시 `Storyboard`의 사용자 플로우를 우선 준수한다.
- Compose UI 상태는 immutable data class로 관리하고, 단방향 데이터 흐름을 유지한다.
- `ViewModel` 외부에서 비즈니스 로직을 최소화하고 UseCase/Repository 계층으로 이동한다.
- `GlobalScope` 사용을 금지하고, 생명주기 범위(scope) 내 코루틴만 사용한다.
- 권한은 최소 권한 원칙을 지키고, 거부/제한 상태 UX를 반드시 제공한다.
- 오프라인 우선 원칙을 지키고, 네트워크 부재 시에도 핵심 기록 기능이 동작해야 한다.

## 코드 스타일/품질 게이트
- 정적 분석: Detekt, Ktlint를 기본 품질 도구로 사용한다.
- 커밋 전 최소 확인: `./gradlew lint testDebugUnitTest`를 기본으로 한다.
- CI 기본 검증은 현재 프로젝트에 구성된 태스크 범위에서 유지한다.
- 테스트 우선순위: Unit Test > 통합/계측 테스트. 핵심 사용자 플로우는 계측 테스트를 추가한다.
- 좌표/거리/페이스 계산 로직은 순수 함수 중심으로 작성하고 테스트를 우선 보강한다.
- 큰 리팩터링 시 기능 동등성 확인을 위한 회귀 테스트를 함께 추가한다.

## 빌드/릴리즈 운영 규칙
- Gradle Kotlin DSL과 `libs.versions.toml` 기반 버전 관리를 사용한다.
- 빌드 변형은 `debug/release`를 기본으로 유지한다.
- `release` 최적화(R8/minify) 적용 여부는 릴리즈 직전에 결정하고 성능 회귀를 확인한다.
- 성능 이슈가 발생하면 러닝 실시간 UI/지도의 프레임 드랍과 recomposition을 점검한다.
- 릴리즈 전 수동 점검에 S1~S5 시나리오를 포함한다.

## 문서 동기화 규칙
- 정책/요구사항 변경이 발생하면 `PRD -> storyboard -> implementation-plan` 순으로 갱신한다.
- 구현 완료 시 해당 Phase 체크박스와 작업 내역을 함께 갱신한다.
- 기술 선택 변경(라이브러리/아키텍처)은 `docs/tech-stack.md`에 근거를 남긴다.
