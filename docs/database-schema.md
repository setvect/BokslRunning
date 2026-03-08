# Database Schema (Phase 1 확정)

프로젝트: 복슬달리기(BokslRunning)
기준 문서: `docs/PRD.md`, `docs/implementation-plan.md`

## 1. 개요

- DB 엔진: Room(SQLite)
- 버전: v1
- 목적: 러닝 세션/트랙 포인트 저장과 조회 계약 고정
- 단위 규칙:
  - 거리: meters
  - 속도: m/s
  - 페이스: sec/km
  - 시간: epochMillis

## 2. 테이블 명세

### 2.1 `running_sessions`

| 컬럼                      | 타입    | Null | 제약/설명                             |
| ------------------------- | ------- | ---- | ------------------------------------- |
| `id`                      | INTEGER | N    | PK, AUTOINCREMENT                     |
| `external_id`             | TEXT    | N    | UNIQUE, 내보내기/중복 판정용 UUID     |
| `status`                  | TEXT    | N    | `IN_PROGRESS` / `SAVED` / `DISCARDED` |
| `started_at_epoch_millis` | INTEGER | N    | 시작 시각                             |
| `ended_at_epoch_millis`   | INTEGER | Y    | 종료 시각                             |
| `duration_millis`         | INTEGER | N    | 기본값 0, `>= 0`                      |
| `distance_meters`         | REAL    | N    | 기본값 0, `>= 0`                      |
| `average_pace_sec_per_km` | REAL    | Y    | `>= 0`                                |
| `max_speed_mps`           | REAL    | N    | 기본값 0, `>= 0`                      |
| `calorie_kcal`            | REAL    | Y    | `>= 0`                                |
| `created_at_epoch_millis` | INTEGER | N    | 생성 시각                             |
| `updated_at_epoch_millis` | INTEGER | N    | 수정 시각                             |

인덱스:
- `(external_id)` UNIQUE
- `(status, started_at_epoch_millis)`
- `(started_at_epoch_millis)`

### 2.2 `track_points`

| 컬럼                       | 타입    | Null | 제약/설명                                      |
| -------------------------- | ------- | ---- | ---------------------------------------------- |
| `id`                       | INTEGER | N    | PK, AUTOINCREMENT                              |
| `external_id`              | TEXT    | N    | UNIQUE, 내보내기/중복 판정용 UUID              |
| `session_id`               | INTEGER | N    | FK -> `running_sessions.id`, ON DELETE CASCADE |
| `sequence`                 | INTEGER | N    | 세션 내 순번, `>= 0`                           |
| `latitude`                 | REAL    | N    | `[-90, 90]`                                    |
| `longitude`                | REAL    | N    | `[-180, 180]`                                  |
| `altitude_meters`          | REAL    | Y    | 고도                                           |
| `accuracy_meters`          | REAL    | Y    | `>= 0`                                         |
| `speed_mps`                | REAL    | Y    | `>= 0`                                         |
| `recorded_at_epoch_millis` | INTEGER | N    | 측정 시각                                      |

인덱스:
- `(external_id)` UNIQUE
- `(session_id, sequence)` UNIQUE
- `(session_id, recorded_at_epoch_millis)`

## 3. DAO 책임

### `RunningSessionDao`
- `insert`, `update`, `getById`, `observeById`
- `observeRecent(limit)` (시작 시각 역순)
- `getLatestByStatus(status)`
- `deleteById`

### `TrackPointDao`
- `insertAll(onConflict = IGNORE)`
- `observeBySessionId(sessionId)`
- `getBySessionId(sessionId)`
- `deleteBySessionId(sessionId)`

## 4. DataStore 키 명세

### 프로필
- `profile_weight_kg` (Float)
- `profile_gender` (String)
- `profile_age` (Int)
- `profile_updated_at_epoch_millis` (Long)

### 단순 플래그
- `onboarding_completed` (Boolean)
- `location_rationale_shown` (Boolean)

## 5. 구현 메모

- Room v1은 CHECK 제약을 애노테이션으로 완전하게 강제하지 못하므로,
  유효성 제약은 Repository 입력 검증(`require`)로 추가 보강한다.
- 활성 세션은 DataStore가 아니라 `running_sessions.status = IN_PROGRESS`로 조회한다.
