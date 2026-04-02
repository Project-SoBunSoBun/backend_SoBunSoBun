# 🛒 소분해요 (SoBunHaeYo) — Backend

> 공동구매 플랫폼 **소분해요**의 Spring Boot 백엔드 서버입니다.  
> 게시글 기반 공동구매 모집, 실시간 채팅, 정산, OAuth 인증을 지원합니다.

---

## 📌 프로젝트 개요

**소분해요**는 사용자들이 공동구매 게시글을 올리고, 참여자를 모집하여 함께 구매 후 정산까지 처리할 수 있는 모바일 중심 서비스입니다.

- 카카오 / 애플 소셜 로그인
- 공동구매 게시글 CRUD 및 상태 관리
- 실시간 1:1 채팅 & 단체 채팅 (WebSocket / STOMP)
- 참여자 간 정산(소분) 처리
- FCM 푸시 알림
- 매너 리뷰 · 신고 · 차단

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.4 |
| Build | Gradle |
| Database | MySQL 8 |
| Cache / Pub-Sub | Redis |
| ORM | Spring Data JPA (Hibernate) |
| 인증 | JWT (jjwt 0.11.5) · Kakao OAuth · Apple OAuth |
| 실시간 통신 | WebSocket + STOMP |
| 푸시 알림 | Firebase Admin SDK (FCM) |
| API 문서 | SpringDoc OpenAPI 3 (Swagger UI) |
| 로깅 | Log4j2 |
| 배포 | Docker Compose |

---

## 📁 프로젝트 구조

```
backend/src/main/java/com/sobunsobun/backend/
│
├── controller/          # REST 컨트롤러 (API 엔드포인트)
│   ├── auth/            # 인증 (로그인, 토큰 재발급)
│   ├── chat/            # 채팅방, 메시지, 초대, 멤버
│   ├── post/            # 게시글, 저장, 신고
│   ├── comment/         # 댓글, 댓글 신고
│   ├── settleup/        # 정산
│   ├── user/            # 사용자 프로필, 설정, 알림 등
│   └── search/          # 검색
│
├── application/         # 비즈니스 로직 서비스
├── domain/              # JPA 엔티티
├── repository/          # Spring Data JPA 레포지토리
├── infrastructure/      # 외부 연동 (OAuth, Firebase, Redis, STOMP)
├── security/            # Spring Security, JWT 필터
└── support/             # 공통 예외, 응답 형식, 유틸
```

---

## 🚀 시작하기

### 요구사항

- Java 17+
- Docker & Docker Compose
- MySQL 8
- Redis

### 1. 저장소 클론

```bash
git clone https://github.com/Project-SoBunHaeYo/backend_SoBunHaeYo.git
cd backend_SoBunHaeYo
```

### 2. 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다.

```dotenv
# Database
DB_HOST=localhost
DB_PORT=3307
DB_NAME=sobunsobun
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your_jwt_secret_key

# Kakao OAuth
KAKAO_CLIENT_ID=your_kakao_client_id

# Apple OAuth
APPLE_CLIENT_ID=com.yourapp.bundle
APPLE_TEAM_ID=XXXXXXXXXX
APPLE_KEY_ID=XXXXXXXXXX
APPLE_PRIVATE_KEY_PATH=apple/AuthKey_XXXXXXXXXX.p8
APPLE_REDIRECT_URI=https://your-domain/api/v1/auth/apple/callback

# Firebase FCM
FIREBASE_CREDENTIALS_JSON={"type":"service_account",...}
FIREBASE_PROJECT_ID=your-firebase-project-id
```

> Apple `.p8` 키 파일은 `backend/src/main/resources/apple/` 디렉토리에 배치하세요.

### 3. 로컬 DB 실행 (Docker)

```bash
docker-compose -f docker-compose.local.yml up -d
```

MySQL 컨테이너가 `localhost:3307`에 실행됩니다.

### 4. 애플리케이션 실행

```bash
cd backend
./gradlew bootRun
```

서버가 `http://localhost:8081` 에서 실행됩니다.

---

## 🔑 인증 방식

모든 API 요청에는 JWT Bearer Token이 필요합니다.

```
Authorization: Bearer {access_token}
```

| 토큰 | 만료 시간 |
|------|-----------|
| Access Token | 30분 |
| Refresh Token | 60일 |

### 지원 OAuth 제공자

- **카카오 (Kakao)**
- **애플 (Apple Sign In)**

---

## 💡 주요 기능

### 🗳 공동구매 게시글

- 게시글 생성 · 수정 · 삭제 · 상태 관리
- 게시글 상태: `OPEN` → `CLOSED` → `PROCESSING` → `COMPLETED` / `CANCELLED`
- 진행 중인 게시글이 있는 경우 중복 생성 방지
- 카테고리 · 태그 기반 필터링 및 페이징 조회
- 게시글 저장(북마크), 신고 기능

### 💬 채팅

- **1:1 채팅**: 사용자 간 개인 메시지
- **단체 채팅**: 공동구매 게시글 연동, getOrCreate 패턴
- WebSocket + STOMP 프로토콜로 실시간 메시지 처리
- Redis를 활용한 읽음 처리 및 안읽은 메시지 수 관리
- 채팅방 초대 · 멤버 관리

### 💰 정산 (소분)

- 공동구매 게시글 생성 시 자동으로 정산 생성
- 참여자별 금액 배분 및 정산 상태 추적

### 🔔 알림

- FCM을 통한 푸시 알림
- 채팅 메시지, 정산 요청 등 이벤트 기반 알림
- 사용자별 알림 설정

### 👤 사용자

- 프로필 조회 · 수정 (닉네임, 이미지)
- 매너 리뷰 작성 · 조회
- 사용자 신고 · 차단
- 탈퇴 처리 및 자동 정리 스케줄러

---

## ⚙️ 설정 요약

| 항목 | 설정값 |
|------|--------|
| 서버 포트 | `8081` |
| DB DDL | `update` |
| 파일 업로드 최대 크기 | 단일 파일 10MB / 요청 15MB |
| 타임존 | `Asia/Seoul` (KST) |
| 배치 Fetch Size | 50 (N+1 방지) |

---

## 🧪 테스트

```bash
cd backend
./gradlew test
```

테스트 환경에서는 MySQL 대신 H2 인메모리 데이터베이스를 사용합니다.

---

## 📄 라이선스

본 프로젝트는 팀 내부 프로젝트로, 별도의 라이선스가 명시되지 않는 한 외부 사용 및 배포를 제한합니다.
