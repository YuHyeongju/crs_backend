# CRS (Congestion & Restaurant Service) — Backend

식당의 **실시간 혼잡도**와 **리뷰**를 지도 기반으로 제공하는 서비스의 백엔드 API 서버입니다.
일반 사용자는 식당 혼잡도/리뷰를 조회·등록하고, 혼잡도를 제보하면 **리워드 포인트**를 적립해 가게 **쿠폰**으로 교환할 수 있습니다.
상인(MERCHANT)은 자기 가게를 등록·관리하고 쿠폰을 발행하며, 관리자(ADMIN)는 가게 승인·회원 제재·신고 처리를 수행합니다.

> 이 저장소는 **백엔드 전용**입니다. 프론트엔드(React, `http://localhost:3000`)는 별도 저장소이며
> 이 레포에서는 `crs_frontend/` 경로로 `.gitignore` 처리되어 추적되지 않습니다.

---

## 기술 스택

| 구분 | 사용 기술 |
| :--- | :--- |
| Language | Java 17 |
| Framework | Spring Boot 3.5.6 (Web, Data JPA, Validation, Security) |
| ORM | Hibernate / Spring Data JPA |
| DB | MySQL 8 |
| 인증 | JWT (Access Token 30분 + Refresh Token 쿠키) + BCrypt 비밀번호 암호화 |
| API 문서 | springdoc-openapi (Swagger UI) |
| Build | Gradle (Gradle Wrapper 포함) |

---

## 프로젝트 구조

```
src/main/java/com/hyeongju/crs/crs/
├── CrsApplication.java          # 애플리케이션 진입점
├── config/                      # SecurityConfig(CORS·보안), WebConfig
├── controller/                  # REST 컨트롤러 (API 엔드포인트)
├── service/                     # 비즈니스 로직
├── repository/                  # Spring Data JPA 리포지토리
├── domain/                      # JPA 엔티티 / Enum
├── dto/                         # 요청·응답 DTO
└── security/                    # JwtUtil (토큰 생성/검증), JwtAuthenticationFilter (요청별 토큰 검사)
```

---

## 도메인 모델 (주요 엔티티)

- **User** — 회원. `Role`(USER / MERCHANT / ADMIN)을 가지며 비밀번호는 BCrypt로 저장. `status`(ACTIVE 등)로 제재/탈퇴 상태 관리.
- **Restaurant** — 식당. `kakaoId`(카카오 지도 식별자) 기준으로 식별. `approvalStatus`(PENDING/APPROVED), 등록 상인(`user`) 연관.
  - **RestaurantMenu** — 메뉴명/가격/메뉴 사진
  - **RestaurantFacilities** — 편의시설(와이파이, 화장실, 주차, 포장, 카카오페이, 삼성페이, 키오스크)
- **Congestion / CongestionStatus** — 식당별 혼잡도 기록 및 상태값. (N+1 최적화 적용 — `troubleshooting_N1_congestion.md` 참고)
- **Review / ReviewReport** — 리뷰 및 리뷰 신고
- **BookMark** — 사용자별 즐겨찾기
- **Reward** — 포인트 적립/차감 **장부**(이벤트 1행 단위). 혼잡도 제보 시 적립(+), 쿠폰 교환 시 차감(−). 잔액은 합계(`SUM`)로 계산.
- **Coupon** — 상인이 자기 가게에 발행하는 쿠폰(제목·필요 포인트·유효기간·활성여부).
- **UserCoupon** — 유저가 포인트로 교환해 보유한 쿠폰(사용 여부·발급/사용 시각).

---

## API 개요

> 모든 응답은 JSON 또는 단순 문자열 메시지. 인증이 필요한 일부 엔드포인트는 **HttpSession**의 `userIdx`를 사용합니다.

### 인증 `/api/auth`
| Method | Path | 설명 |
| :--- | :--- | :--- |
| POST | `/register/user` | 일반 사용자 회원가입 |
| POST | `/register/merchant` | 상인 회원가입 (사업자번호 검증) |
| POST | `/register/admin` | 관리자 회원가입 (관리자 코드 검증) |
| POST | `/login` | 로그인 (Access Token JSON 반환 + Refresh Token HttpOnly 쿠키 설정) |
| POST | `/refresh` | Access Token 재발급 (Refresh Token 쿠키로 자동 갱신) |
| POST | `/logout` | 로그아웃 (DB Refresh Token 삭제 + 쿠키 만료) |
| POST | `/withdraw?id=` | 회원 탈퇴 |

### 식당 `/api/restaurants`
| Method | Path | 설명 |
| :--- | :--- | :--- |
| POST | `/detail` | kakaoId로 식당 조회/없으면 생성(getOrCreate) |
| POST | `/register` | 상인의 가게 등록 (multipart, 메뉴 이미지 포함) |
| GET | `/my-restaurant-list` | 내가 등록한(승인된) 가게 목록 |
| POST | `/bulkDetails` | 여러 kakaoId의 평점/리뷰수 일괄 조회 (지도 핀 최적화) |
| GET | `/kakaoId/{kakaoId}` | kakaoId 단건 상세(미등록 시 빈 DTO 반환) |
| POST | `/update/{restIdx}` | 가게 정보 수정 (multipart) |
| POST | `/delete/{restIdx}` | 가게 삭제(메뉴 이미지 파일 포함) |

### 혼잡도 `/api/congestion`
| Method | Path | 설명 |
| :--- | :--- | :--- |
| GET | `/{kakaoId}` | 단일 가게 현재 혼잡도 |
| POST | `/bulkStatus` | 여러 가게 현재 혼잡도 일괄 조회 |
| POST | `/updateStatus` | 혼잡도 상태 등록/갱신 (제보 시 리워드 포인트 자동 적립, 같은 가게 30분 쿨다운) |
| GET | `/history` | 내 혼잡도 제보 이력 (세션 필요) |

### 리뷰 `/api/reviews`
| Method | Path | 설명 |
| :--- | :--- | :--- |
| GET | `/{restIdx}` | 식당의 리뷰 목록 |
| POST | `/register` | 리뷰 등록 |
| POST | `/report` | 리뷰 신고 |
| GET | `/my/{userIdx}` | 내 리뷰 목록 (페이징) |
| PUT | `/{reviewIdx}` | 내 리뷰 수정 |
| DELETE | `/{reviewIdx}?userIdx=` | 내 리뷰 삭제 |

### 즐겨찾기 `/api/bookmarks`
| Method | Path | 설명 |
| :--- | :--- | :--- |
| POST | `/toggle` | 즐겨찾기 추가/해제 토글 |
| GET | `/my-bookmark-list/{userIdx}` | 내 즐겨찾기 kakaoId 목록 |
| GET | `/details` | 마이페이지용 즐겨찾기 상세 (세션 필요) |

### 리워드 `/api/rewards`
| Method | Path | 설명 |
| :--- | :--- | :--- |
| GET | `/balance/{userIdx}` | 보유 포인트 잔액 (적립/차감 내역 합계) |

### 쿠폰 `/api/coupons`
| Method | Path | 설명 |
| :--- | :--- | :--- |
| POST | `/register` | 상인: 쿠폰 등록 (본인 소유 가게 검증) |
| GET | `/my-store/{merchantUserIdx}` | 상인: 내 가게 쿠폰 목록 |
| POST | `/delete/{couponIdx}?merchantUserIdx=` | 상인: 쿠폰 비활성화(소프트 삭제) |
| GET | `/available` | 교환 가능한 쿠폰 목록 (활성·유효기간 내) |
| POST | `/{couponIdx}/redeem?userIdx=` | 유저: 포인트로 쿠폰 교환 (잔액 확인 후 차감 + 발급) |
| GET | `/my/{userIdx}` | 유저: 보유 쿠폰 목록 |
| POST | `/use/{userCouponIdx}?userIdx=` | 유저: 쿠폰 사용 처리 |

### 마이페이지 (`/api/users`, `/api/merchants`, `/api/admins`)
| Method | Path | 설명 |
| :--- | :--- | :--- |
| GET | `/api/users/mypage` | 일반 사용자 프로필 (세션) |
| POST | `/api/users/mypage/updateUser` | 일반 사용자 정보 수정 |
| GET | `/api/merchants/mypage` | 상인 프로필 |
| POST | `/api/merchants/mypage/updateMerchant` | 상인 정보 수정 |
| GET | `/api/admins/mypage` | 관리자 프로필 |
| POST | `/api/admins/mypage/updateAdmin` | 관리자 정보 수정 |

### 관리자 기능 `/api/admins`
| Method | Path | 설명 |
| :--- | :--- | :--- |
| GET | `/pending` | 승인 대기 가게 목록 |
| POST | `/approve/{restIdx}` | 가게 승인 |
| POST | `/reject/{restIdx}` | 가게 거절 |
| GET | `/users` | 전체 회원 목록 |
| GET | `/users/{userIdx}` | 회원 상세 |
| POST | `/users/{userIdx}/sanction` | 회원 제재 |
| POST | `/users/{userIdx}/deactivate` | 회원 탈퇴 처리 |
| GET | `/reports` | 리뷰 신고 목록 |
| GET | `/reports/{reportIdx}` | 리뷰 신고 상세 |
| POST | `/reports/{reportIdx}/process?approve=` | 신고 처리(승인/반려) |

> 서버 기동 후 Swagger UI에서 전체 스펙 확인 가능: `http://localhost:8080/swagger-ui.html`

---

## 리워드 / 쿠폰 동작 메모

- **적립** — 혼잡도 제보(`/api/congestion/updateStatus`) 저장 시 시스템이 자동으로 포인트 적립. **같은 유저+같은 가게는 30분 쿨다운**(쿨다운 내 재제보는 기록은 되지만 적립은 제외)으로 어뷰징 방지.
- **잔액** — 별도 잔액 컬럼 없이 `Reward` 장부의 합계(`SUM`)로 계산. 교환 시 음수 행을 추가해 차감하므로 항상 정확.
- **쿠폰** — 상인이 **본인 소유의 승인된 가게**에만 등록(소유 검증). 유저는 보유 포인트로 교환 → `UserCoupon` 발급 + 포인트 차감(같은 트랜잭션) → 사용 처리. 등록 즉시 노출(현재 MVP는 관리자 검수 없음).

---

## 로컬 실행 방법

### 1. 사전 준비
- JDK 17
- MySQL 8 (로컬 `3306` 포트), `crs` 스키마 생성
  ```sql
  CREATE DATABASE crs DEFAULT CHARACTER SET utf8mb4;
  ```
- 메뉴 이미지 업로드 경로: `C:/upload/menu_picts/` (없으면 자동 생성)

### 2. 설정
`src/main/resources/application.properties`에서 DB 접속 정보(`username`/`password`)를 본인 환경에 맞게 수정합니다.
> ⚠️ 현재 파일에 DB 비밀번호와 JWT 시크릿이 하드코딩되어 있습니다. (아래 "개선 필요" 참고)

### 3. 빌드 & 실행
```bash
# Windows PowerShell
./gradlew.bat bootRun

# 또는 빌드 후 실행
./gradlew.bat build
java -jar build/libs/crs-0.0.1-SNAPSHOT.jar
```
- 기본 포트: `8080`
- CORS 허용 출처: `http://localhost:3000` (프론트엔드)
- `spring.jpa.hibernate.ddl-auto=update` — 엔티티 기준으로 테이블 자동 생성/갱신

---

## 아키텍처 메모

- **계층 구조**: Controller → Service → Repository(JPA) → MySQL
- **인증 방식**: JWT 기반 Stateless 인증. 로그인 시 Access Token(30분)을 JSON으로 반환하고, Refresh Token(로그인 기억 시 30일 / 미체크 시 세션 쿠키)을 HttpOnly 쿠키로 설정. 모든 요청은 `JwtAuthenticationFilter`를 통과하며, 유효한 토큰이면 `request.setAttribute("authenticatedUserIdx", userIdx)`로 컨트롤러에 전달.
- **N+1 최적화**: 지도에서 다수 식당의 혼잡도를 조회할 때 발생하던 N+1 문제를 `Restaurant.congestions`에 `@BatchSize(size = 10)`을 적용해 해결. 상세 내용은 [`troubleshooting_N1_congestion.md`](./troubleshooting_N1_congestion.md).

---

## 🚧 아직 구현/개선이 필요한 부분

코드 전반을 점검한 결과, 다음 항목들이 미완성이거나 보강이 필요합니다. (우선순위 순)

### 보안
1. **역할 기반 접근 제어 미적용** — `SecurityConfig`가 `anyRequest().permitAll()`로 모든 경로를 허용. 관리자 API(`/api/admins/**`)에 역할 검사 없음. → `hasRole` 도입 필요.
2. **민감 정보 하드코딩 & 커밋됨** — `application.properties`에 DB 비밀번호와 `app.jwtSecret`이 평문으로 들어가 git에 추적됨. → 환경변수/`application-local.properties`로 분리 권장.

### 기능 / 정합성
3. **본인 검증 일관성** — 리뷰·즐겨찾기·리워드·쿠폰 등 일부 API는 `userIdx`를 요청 파라미터/바디로 받아 JWT 주체와 대조하지 않음(타인 행세 가능). JWT `authenticatedUserIdx`로 통일 권장.
5. **쿠폰 동시 교환 방어 약함** — 포인트 차감이 트랜잭션 내 "잔액 확인 → 차감" 수준이라, 동시 요청 시 이중 차감 여지가 있음. 비관적 락 또는 원자적 차감 도입 권장.
6. **관리자 쿠폰 검수 부재** — 상인이 등록한 쿠폰이 검수 없이 즉시 노출됨(의도된 MVP). 악성·허위 쿠폰 대비가 필요하면 쿠폰 승인제/신고 처리 추가.

### 코드 품질 / 운영
7. **로깅** — 곳곳에 `System.out.println` 디버그 출력이 다수 남아 있음. SLF4J 로거로 교체 권장.
8. **파일 업로드 경로 하드코딩** — `RestaurantService.uploadPath = "C:/upload/menu_picts/"` 가 Windows 절대경로로 고정. 설정값으로 외부화 + 정적 리소스 서빙 경로 정리 필요.
9. **테스트 부재** — 기본 컨텍스트 로드 테스트(`CrsApplicationTests`) 외 단위/통합 테스트 없음.
10. **예외 처리 산발적** — 컨트롤러마다 try/catch로 개별 처리. `@RestControllerAdvice` 전역 예외 처리로 통일하면 응답 형식 일관성↑.
11. **응답 포맷 불일치** — 어떤 API는 엔티티를 직접 반환(`Restaurant`)하고 어떤 것은 DTO를 반환. 엔티티 직접 노출은 순환참조/과다노출 위험이 있어 DTO로 통일 권장.

---

## 라이선스 / 기타
- 데모/학습용 프로젝트 (`version 0.0.1-SNAPSHOT`).
- 기본 Spring Boot 안내는 [`HELP.md`](./HELP.md) 참고.
