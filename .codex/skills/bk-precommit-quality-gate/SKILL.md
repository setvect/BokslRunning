---
name: bk-precommit-quality-gate
description: Run pre-commit quality gates for this Android project with Gradle (lint, ktlint, detekt, unit test, compile/build) and report pass/fail clearly. Use when user asks to check quality before commit (e.g. "커밋 전에 검사해줘", "품질 게이트 돌려줘", "lint/test/build 확인해줘").
allowed-tools: Bash(git status *), Bash(git diff *), Bash(./gradlew *)
---

## 커밋 전 품질 게이트 실행

커밋 전에 Android 품질/빌드 상태를 검증하고 결과를 요약한다.

### 1. 변경사항 확인
```bash
git status --short
git diff --stat
```
- 변경사항이 없으면 검사 목적을 사용자에게 재확인한다.

### 2. 기본 품질 게이트 실행
아래 순서로 실행한다.
```bash
./gradlew lint ktlintCheck detekt
./gradlew testDebugUnitTest
./gradlew compileDebugKotlin
./gradlew assembleDebug
```
- 빠른 검증 요청이면 첫 두 줄만 실행하고 생략 항목을 결과에 명시한다.

### 3. 실패 시 처리
- 실패한 첫 태스크와 핵심 에러(파일/라인/룰)를 요약한다.
- 원인 후보가 명확하면 최소 수정안을 제시하고, 필요 시 바로 수정 후 재실행한다.
- 실패 상태에서는 커밋 진행을 권장하지 않는다.

### 4. 성공 시 결과 보고
- 실행한 명령 목록
- 전체 성공/실패 여부
- 생략한 게이트(있다면 이유 포함)
- 커밋 진행 가능 여부

### 5. 기본 원칙
- 프로젝트 루트에서 `./gradlew`만 사용한다.
- 임의로 `-x test` 같은 우회 옵션을 쓰지 않는다.
- 시간이 오래 걸리는 태스크를 생략하면 반드시 사용자에게 명시한다.
