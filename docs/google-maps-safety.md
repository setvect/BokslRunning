# Google Maps SDK 운영 가이드 (러닝 지도 전용)

프로젝트: 복슬달리기(BokslRunning)  
대상 범위: Android 앱 내 러닝 지도 표시(Polyline)  
작성일: 2026-02-28

## 1. 목적

- 러닝 지도 기능만 사용하면서 과도한 API 호출/비용 발생을 방지한다.
- API 키 노출/오용으로 인한 예상치 못한 과금을 예방한다.

## 2. 사용 범위

- 사용: `Maps SDK for Android` (지도 표시, 경로 Polyline)
- 미사용: Street View, Places, Routes API

## 3. 기본 원칙

- API 키는 반드시 제한(restriction)한다.
- Quota(쿼터)로 호출량 하드캡을 건다.
- Budget Alert(예산 알림)으로 조기 탐지한다.
- 디버그/릴리즈 키를 분리한다.

## 4. API Key 보안 설정 (필수)

1. Google Cloud Console에서 Maps 키를 생성한다.
2. `Application restrictions`를 `Android apps`로 설정한다.
3. 허용 앱으로 아래를 등록한다.
   - 패키지명 (`applicationId`)
   - SHA-1 인증서 지문 (debug/release 각각)
4. `API restrictions`를 활성화하고 `Maps SDK for Android`만 허용한다.

## 5. 과금 폭주 방지 설정

1. Quota 제한
   - `Maps SDK for Android` 일일/분당 쿼터를 낮게 설정한다.
   - 1인 개인용 기준 초기값 예시: 일 500~1,000 요청.
   - 부족하면 단계적으로 상향한다.
2. Budget Alert
   - 월 예산을 낮게 설정한다(예: USD 1~5).
   - 50% / 90% / 100% 알림을 설정한다.
3. 모니터링
   - 사용량 리포트와 오류율(4xx/5xx)을 주기 확인한다.

## 6. 운영 체크리스트

- [ ] `Maps SDK for Android`만 활성화/허용되어 있다.
- [ ] API 키가 `Android apps` 제한(패키지+SHA-1) 상태다.
- [ ] debug/release 키가 분리되어 있다.
- [ ] Quota 하드캡이 설정되어 있다.
- [ ] Budget Alert가 설정되어 있다.

## 7. 이상 징후 대응

1. 즉시 API 키를 비활성화한다.
2. 비정상 호출이 발생한 API/SKU를 확인한다.
3. 키 제한 조건(패키지/SHA-1/API 제한)을 재검토한다.
4. 필요 시 새 키를 발급하고 앱에 교체 적용한다.

## 8. 참고

- Maps SDK for Android Usage and Billing  
  https://developers.google.com/maps/documentation/android-sdk/usage-and-billing
- Maps Billing and Pricing (2025 정책 변경 포함)  
  https://developers.google.com/maps/billing-and-pricing/march-2025
- Google Maps Platform Pricing  
  https://developers.google.com/maps/billing-and-pricing/pricing
- Manage Costs  
  https://developers.google.com/maps/billing-and-pricing/manage-costs
- Cloud Billing Budgets and Alerts  
  https://cloud.google.com/billing/docs/how-to/budgets
