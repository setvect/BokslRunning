# 구현 작업 계획서 (기술 관점)

프로젝트: 복슬달리기(BokslRunning)  
기준 문서: `docs/PRD.md`, `docs/storyboard.md`, `docs/tech-stack.md`

## 1. 문서 목적

- MVP 구현을 위한 기술 작업 순서를 정의한다.
- 화면 단위가 아니라 기능/데이터/상태 흐름 기준으로 작업을 쪼갠다.
- 각 단계의 선행조건과 산출물을 명확히 하여 개발 중 의존성 충돌을 줄인다.

## 2. 구현 원칙

- 오프라인 우선: 핵심 기록/조회 기능은 로컬 데이터 기준으로 동작한다.
- 상태 단일화: 화면 상태는 ViewModel의 UI State로 관리한다.
- 도메인 우선: UI보다 도메인 모델/저장 구조를 먼저 고정한다.
- 점진 통합: 러닝 코어(기록 엔진) 완성 후 화면을 붙인다.

## 3. 선행 결정 사항

- 앱 구조: `ui / domain / data` 패키지 구조로 시작한다.
- 저장소: Room + DataStore를 기본 저장소로 사용한다.
- 위치 추적: Foreground Service + FusedLocationProviderClient를 사용한다.
- 지도 렌더링: Google Maps SDK(Polyline)를 사용한다.
- 내보내기 포맷: `bokslrunning_export_v1.json` + `schema_version` 필수.

## 4. 단계별 구현 순서

- [x] **Phase 0. 프로젝트 골격 구성**

- 목표: 이후 기능 개발이 가능한 최소 실행 구조를 만든다.
- 주요 작업:
  - Gradle/Compose/Hilt/Room/DataStore 기본 의존성 세팅
  - 패키지 구조 생성(`ui`, `domain`, `data`, `core`)
  - 앱 내비게이션 라우트 골격 생성
- 산출물:
  - 앱 실행 가능한 기본 화면(빈 홈)
  - DI 진입점/Hilt Application 세팅
- 완료 내역 (2026-02-28):
  - Hilt/Navigation/Room/DataStore/Coroutines 의존성 및 플러그인 구성 완료
  - `ui/domain/data/core` 패키지 골격 + `home/settings/history/stats` 라우트 placeholder 연결 완료

- [x] **Phase 1. 도메인/저장소 기반 확정**

- 목표: 러닝 기록의 데이터 계약을 먼저 고정한다.
- 주요 작업:
  - 도메인 모델 정의: `Profile`, `RunningSession`, `TrackPoint`, `RunStats`
  - 테이블 명세서 작성/확정: 테이블, 컬럼, 타입, PK/FK, 인덱스, 제약조건
  - Room Entity/DAO/Repository 구현
  - DataStore로 프로필 및 단순 플래그 저장
- 산출물:
  - 테이블 명세서 문서(확정본)
  - 세션/트랙 포인트 CRUD 동작
  - 프로필 저장/조회 동작
- 완료 내역 (2026-03-01):
  - 도메인 계약 고정: `Profile`, `RunningSession`, `TrackPoint`, `RunStats`, `AppPreferences`, `Gender`, `SessionStatus`
  - Room v1 구현 완료: `running_sessions`, `track_points` Entity/DAO/Database 및 status converter 추가
  - Repository 구현 완료: `RunningRepository`, `ProfileRepository` 인터페이스 및 기본 구현체 연결
  - DataStore 구현 완료: 프로필 키 4종 + 플래그 키 2종 저장/조회 경로 확정
  - 테이블 명세서 문서 추가: `docs/database-schema.md`
  - Phase 1 회귀 테스트 추가: DAO/Repository/DataStore 단위 테스트

- [x] **Phase 2. 온보딩/권한/기본 네비게이션 구현**

- 목표: 첫 실행부터 홈 진입까지의 진입 플로우를 완성한다.
- 주요 작업:
  - 온보딩 화면 + 프로필 입력 화면 구현
  - 위치 권한 요청/거부/설정 이동 처리
  - 홈/설정/기록/통계 화면 라우팅 연결
- 산출물:
  - 스토리보드 1~3 구간 동작
  - 권한 상태에 따른 분기 완료
- 완료 내역 (2026-03-07):
  - 첫 실행 진입 플로우 구현 완료: `온보딩 -> 프로필 입력 -> 위치 권한 -> 홈`
  - 상태 기반 앱 시작 라우팅 구현 완료: `Profile`/`AppPreferences` 조합으로 초기 목적지 결정
  - 프로필 폼 및 설정 내 프로필 수정 재사용 흐름 구현 완료
  - 위치 권한 요청/거부/설정 이동 분기 및 홈의 권한 안내 다이얼로그 구현 완료
  - 홈 누적 요약 카드 구현 완료: 저장된 세션 기준 거리/시간/평균 속도/칼로리 집계 연결
  - Phase 2 단위 테스트 추가: 앱 시작 상태, 프로필 폼 검증, 홈 요약 집계, 권한 상태 매핑

- [x] **Phase 3. 러닝 코어(기록 엔진) 구현**

- 목표: 실제 러닝 데이터가 누락 없이 쌓이는 핵심 엔진을 만든다.
- 주요 작업:
  - 러닝 상태 머신 구현(Ready → Running → StopConfirm → Saved)
  - Foreground Service 기반 위치 수집 루프 구현
  - 거리/페이스/평균/최고속도/칼로리 계산 로직 구현
  - 주기 저장(세션 + 트랙포인트)
- 산출물:
  - 러닝 시작/진행/종료 저장까지 백엔드 로직 완성
  - 앱 재실행 시 진행 세션 복구 가능
- 완료 내역 (2026-03-07):
  - `RunEngineRepository` 기반 상태 머신 구현 완료: `Ready -> Running -> StopConfirm -> Saved`
  - Foreground Service + `FusedLocationProviderClient` 기반 위치 수집 루프 구현 완료
  - 거리/pace/최고속도/칼로리 계산 유틸 및 5초 flush 저장 정책 구현 완료
  - 활성 세션 복구 구현 완료: `IN_PROGRESS` 세션과 기존 트랙포인트 기반 snapshot 복원
  - 최소 런 라우트 placeholder 연결 완료: `RunReady`, `RunLive`, `RunSummary`
  - Phase 3 단위 테스트 추가: 계산 유틸, 앱 시작 active session 분기, 엔진 상태 전이/복구

- [x] **Phase 4. 러닝 UI 연동**

- 목표: 기록 엔진과 화면을 연결해 사용자 동작을 완성한다.
- 주요 작업:
  - Ready 화면(현재 위치, 시작/취소, 오프라인 안내)
  - 러닝 라이브 화면(실시간 지표 + 지도 경로)
  - 종료 확인 다이얼로그 및 저장 플로우
  - 결과 요약 화면 구현
- 산출물:
  - 스토리보드 4~7 구간 동작
  - 종료 후 요약/완료 이동 가능
  - Google Maps 기반 현재 위치/Polyline 표시

- [x] **Phase 5. 기록 조회 기능 구현**

- 목표: 저장된 기록을 목록/상세로 확인 가능하게 한다.
- 주요 작업:
  - 기록 목록(날짜순, 핵심 요약 값)
  - 기록 상세(지도 경로 + 요약 수치)
- 산출물:
  - 스토리보드 9 구간 동작
  - 과거 기록 재열람 가능
- 완료 내역 (2026-03-07):
  - 기록 목록 구현 완료: `SAVED` 세션만 날짜순 역정렬로 페이지 단위 조회하고 날짜/거리/시간/평균 페이스 표시
  - 기록 상세 구현 완료: 지도 경로 + 거리/시간/평균 페이스/최고 속도/칼로리 요약 표시
  - 빈 기록 상태 구현 완료: `러닝 시작` CTA와 홈과 동일한 위치 권한 안내 흐름 연결
  - 공용 UI 포맷터 및 권한 다이얼로그 재사용 경로 정리 완료
  - Phase 5 단위 테스트 추가: 저장 기록 조회 쿼리, history ViewModel 목록/상세 상태 검증

- [x] **Phase 6. 통계 집계/차트 구현**

- 목표: 누적/월별 통계를 계산하고 시각화한다.
- 주요 작업:
  - 누적 거리/시간/평균속도 집계 쿼리 구현
  - 월별 집계 데이터셋 생성
  - 통계 화면(요약 카드 + 월별 차트) 구현
- 산출물:
  - 스토리보드 10 구간 동작
  - 신규 기록 반영 시 통계 갱신
- 완료 내역 (2026-03-07):
  - 월별 통계 도메인/저장소 계약 추가 완료: `MonthlyStatsPoint`, `StatsChartMetric`, `observeMonthlyStats()`
  - Room 월별 집계 쿼리 구현 완료: `SAVED` 세션 기준 로컬 타임존 월 그룹핑 + 빈 달 0 버킷 보정
  - 통계 화면 구현 완료: 누적 요약 카드, 전체 기간 가로 스크롤 차트, 선택 월 상세 카드, 빈 상태 메시지
  - 신규 저장 기록 반영 시 통계 자동 갱신 흐름 연결 완료: `HomeSummary` + 월별 Flow 결합
  - Phase 6 단위 테스트 추가: DAO 월별 집계, repository 버킷 보정, stats ViewModel 상태, 포맷터 검증
  - 차트 라이브러리 적용 완료: 현재 프로젝트의 AGP/Compose 제약에 맞춰 Vico Compose `1.13.1` 사용

- [x] **Phase 7. 내보내기 구현**

- 목표: 전체 데이터 백업 가능한 단일 JSON 생성 기능을 완성한다.
- 주요 작업:
  - Export DTO 설계 및 `schema_version` 포함
  - 세션 + 트랙포인트 직렬화
  - 파일 생성 및 공유/저장 액션 연결
- 산출물:
  - 스토리보드 11 구간 동작
  - `bokslrunning_export_v1.json` 생성 가능
- 완료 내역 (2026-03-08):
  - 내보내기 전용 계약 추가 완료: `ExportRepository`, `ExportProgress`, export DTO 및 `schema_version = 1` JSON 스키마 고정
  - 전체 백업 내보내기 구현 완료: `SAVED` 세션 + 트랙포인트 + 프로필 + 앱 설정을 `bokslrunning_export_v1.json` 단일 파일로 캐시에 생성
  - Android 공유 경로 구현 완료: `FileProvider` + cache export path + system share sheet 기반 `파일 공유/저장` 연결
  - 설정/내보내기 화면 구현 완료: 설정 버튼 활성화, export route 추가, 준비/진행/완료/오류 상태 UI 및 취소/재시도 동작 연결
  - Phase 7 단위 테스트 추가: export repository JSON 스키마/필터링/빈 데이터/취소 정리, export ViewModel 상태 전이/이벤트 검증

- [x] **Phase 8. 가져오기 구현**

- 목표: 기존 데이터 보존 정책을 지키면서 병합 가져오기를 완성한다.
- 주요 작업:
  - 가져오기 시작 전 내부 백업 생성
  - JSON 파싱 + 스키마 버전 검증
  - 병합 정책 적용(기존 유지 + 신규 추가)
  - 중복 파일/중복 레코드 판정 및 결과 집계
- 산출물:
  - 스토리보드 12 구간 동작
  - 결과 안내(추가 N건, 중복 M건) 표시
- 완료 내역 (2026-03-08):
  - 가져오기 전용 계약 추가 완료: `ImportRepository`, `ImportProgress`, `ImportResult` 및 `schema_version = 1` 검증 경로 확정
  - 내부 백업 구현 완료: 가져오기 전 현재 데이터를 `filesDir/import-backups/` 아래 export 스키마 JSON으로 저장
  - 병합 가져오기 구현 완료: `SAVED` 세션만 `external_id` 기준 신규 추가하고, 동일 파일은 SHA-256 해시 저장으로 재가져오기 차단
  - 프로필/앱 설정 병합 정책 구현 완료: 현재 프로필이 없을 때만 import 파일의 프로필/설정을 적용
  - 설정/가져오기 화면 구현 완료: document picker 연동, 준비/진행/완료/중복 파일/오류 상태 UI 및 취소 동작 연결
  - Phase 8 단위 테스트 추가: import repository 백업/파싱/중복 파일/병합/프로필 정책 검증, import ViewModel 상태 전이 검증

- [x] **Phase 9. 예외/복구 플로우 마감**

- 목표: PRD의 중단/복구/오프라인 요구를 기능적으로 닫는다.
- 주요 작업:
  - 권한 미허용 상태 재진입 처리
  - 오프라인 상태 UI 안내 일관화
  - 러닝 중 프로세스 중단 후 이어하기/폐기 플로우 완성
- 산출물:
  - 스토리보드 2, 8과 연계된 복구 흐름 완성
  - 기능 단위 구현 마감
- 완료 내역 (2026-03-08):
  - 앱 재실행 복구 플로우 구현 완료: `IN_PROGRESS` 세션 감지 시 `RunRecovery`로 진입하고 `이어하기/폐기` 분기 및 홈 fallback 연결 완료
  - 권한 재진입 자동 이어서 구현 완료: 온보딩 권한 게이트와 홈/기록의 `설정 열기` 복귀 시 권한 재확인 후 `Home` 또는 `RunReady` 자동 이동 완료
  - 러닝 오프라인 안내 공통화 완료: `RunReady`, `RunRecovery`, `RunLive`, `RunSummary`에 동일 배너 재사용 경로 정리 완료
  - Phase 9 단위 테스트 추가 완료: 앱 시작 복구 분기, 권한 설정 복귀 이벤트, 복구 ViewModel 상태/이벤트 검증

## 5. 기능-단계 매핑

- A(프로필): Phase 1, 2
- B(러닝 기록/트래킹): Phase 3, 4
- C(오프라인 기록): Phase 3, 4, 9
- D(기록 열람): Phase 5
- E(분석/통계): Phase 6
- F(전체 내보내기): Phase 7
- G(가져오기): Phase 8

## 6. 구현 시 주의 포인트

- 칼로리 계산은 프로필 존재 여부에 따라 표시 정책(A2)을 분기한다.
- `완료` 버튼은 저장 확정이 아니라 화면 이동(P2)임을 UI 이벤트에서 분리한다.
- 앱 중단 복구는 "마지막 저장 지점부터 이어짐"(P4) 정책을 그대로 따른다.
- 가져오기 중 백업 실패 시 병합을 시작하지 않는다.
