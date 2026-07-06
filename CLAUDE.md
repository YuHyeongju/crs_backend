# CRS 프로젝트 현황 (Claude 인계 문서)

## 프로젝트 구성
- **백엔드**: `C:\crs_backend\crs` (Spring Boot 3.5, MySQL 8, 포트 8080)
- **프론트엔드**: `C:\crs_frontend\my-restaurant-app` (React, 포트 3000)
- **DB**: MySQL, 스키마명 `crs`
- **이미지 업로드 경로**: `C:/upload/menu_picts/`
- **민감정보**: `src/main/resources/application-secret.properties` (gitignore 처리됨)

---

## 인증 방식 (2026-06-23 JWT로 전환 완료)

세션 방식을 JWT로 전면 교체했다.

- **Access Token**: 30분 수명, 프론트 `sessionStorage`에 저장, 모든 요청에 `Authorization: Bearer` 헤더로 전송
- **Refresh Token**: UUID, DB(`refresh_token` 테이블)에 저장, HttpOnly 쿠키로 전달
  - 로그인 기억 ✓ → 30일 영구 쿠키
  - 로그인 기억 ✗ → 세션 쿠키 (브라우저 닫으면 삭제)
- **JwtAuthenticationFilter**: 모든 요청에서 토큰 검증 후 `request.setAttribute("authenticatedUserIdx", userIdx)` 설정
- **컨트롤러 인증 패턴**: `Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");`

---

## 구현 완료 기능

### 백엔드
- 회원가입/로그인/로그아웃/회원탈퇴 (USER, MERCHANT, ADMIN 역할 구분)
- JWT 인증 (Access Token + Refresh Token, 로그인 기억)
- 혼잡도 조회/등록/수정 (30분 쿨다운, 포인트 자동 적립)
- 리뷰 등록/조회/수정/소프트삭제/신고
- 북마크 토글/조회 (카카오 가게 + DB-only 가게 모두 지원)
- 리워드 포인트 적립/잔액 조회
- 쿠폰 등록/조회/비활성화/교환/사용
- 상인 가게 등록/수정/삭제 (메뉴 이미지 포함)
- 관리자 가게 승인/거절, 회원 관리, 리뷰 신고 처리
- DB-only 가게 (kakaoId=null): restIdx 기반 조회, 좌표 저장, 혼잡도 연동
- N+1 최적화 (`@BatchSize`, `findByKakaoIdIn` 일괄 조회)
- 이메일 인증 기반 아이디 찾기 / 비밀번호 재설정 (Gmail SMTP)

### 프론트엔드
- 카카오 지도 기반 메인 화면 (마커, 혼잡도, 사이드 패널)
- 카카오 API 가게 + DB-only 상인 가게 통합 표시
- 가게 상세 페이지 (홈/사진/메뉴/리뷰 탭)
- 마이페이지 (역할별: 혼잡도 이력, 즐겨찾기, 리뷰, 포인트, 쿠폰)
- 상인 가게 등록/수정/삭제 패널 + 쿠폰 관리
- 관리자 패널 (가게 승인, 회원 관리, 신고 처리)
- JWT 인증 (AuthContext: 자동 토큰 갱신, 자동 로그인, 로그인 기억)
- axios timeout 10초 설정 (백엔드 재시작 시 무한 로딩 방지)
- 아이디 찾기 / 비밀번호 재설정 페이지 (FindIdPage.js, ResetPasswordPage.js)
- 사진/메뉴 탭 DB 전환 (PhotosTab, MenuTab)
- 홈페이지 2단계 로딩 최적화 (카카오 마커 즉시 표시 → 평점/상인핀 병렬 로드)
- 상세 페이지 로딩 최적화 (location.state 활용 카카오 재검색 생략, 혼잡도/평점 병렬 조회)

---

## 남은 작업

### 개선 필요 (포트폴리오 수준이므로 보류)
- 역할 기반 접근 제어 (`hasRole`) — 현재 `anyRequest().permitAll()`
- `userIdx` 본인 검증 통일 — 리뷰/쿠폰 등 일부 API가 JWT 주체와 대조 안 함
- `System.out.println` → SLF4J 로거 교체
- `@RestControllerAdvice` 전역 예외 처리

---

## 주요 파일 위치

| 역할 | 경로 |
|------|------|
| JWT 유틸 | `security/JwtUtil.java` |
| JWT 필터 | `security/JwtAuthenticationFilter.java` |
| Security 설정 | `config/SecurityConfig.java` |
| Refresh Token 엔티티 | `domain/RefreshToken.java` |
| 인증 컨트롤러 | `controller/AuthController.java` |
| 인증 서비스 | `service/AuthService.java` |
| 프론트 인증 컨텍스트 | `C:\crs_frontend\my-restaurant-app\src\context\AuthContext.js` |
| 프론트 로그인 페이지 | `C:\crs_frontend\my-restaurant-app\src\pages\auth\LoginPage.js` |
