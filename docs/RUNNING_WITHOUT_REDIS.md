# Redis 없이 애플리케이션 실행하기

## 📋 개요

소분소분 백엔드는 **Redis가 없어도 대부분의 기능이 정상 작동**합니다.  
단, **채팅 기능만 Redis가 필수**입니다.

---

## ✅ Redis 없이 작동하는 기능

- ✅ 사용자 인증 (로그인, 회원가입, JWT)
- ✅ 게시글 CRUD
- ✅ 댓글 CRUD
- ✅ 공동구매 기능
- ✅ 정산 기능
- ✅ 알림 기능
- ✅ 파일 업로드
- ✅ 검색 기능
- ✅ 기타 모든 REST API

---

## ⚠️ Redis가 필요한 기능

- ❌ **실시간 채팅** (WebSocket + Redis Pub/Sub)
  - 메시지 송수신
  - 안 읽은 메시지 카운트
  - 읽음 처리
  - 실시간 알림

---

## 🚀 Redis 없이 실행하기

### 1. 환경 변수 설정하지 않기

**기존 방식 (Redis 필수):**
```bash
export redisHost=localhost
export redisPort=6379
```

**Redis 없이 실행:**
```bash
# redisHost, redisPort 환경 변수를 설정하지 않으면 자동으로 Redis 없이 실행됩니다.
./gradlew bootRun
```

### 2. 애플리케이션 시작 시 로그 확인

**Redis 없이 시작 시 로그:**
```
⚠️ ═══════════════════════════════════════════════════════
⚠️ Redis 서버가 연결되지 않았습니다!
⚠️ 채팅 기능을 사용하려면 Redis 서버를 시작해주세요.
⚠️ 다른 API는 정상적으로 작동합니다.
⚠️ ═══════════════════════════════════════════════════════
```

**Redis 연결 성공 시 로그:**
```
========================================
🎉 Redis 연결 100% 성공! 가져온 값: ...
========================================
```

---

## 🔧 Redis 설치 및 실행 (선택사항)

채팅 기능을 사용하려면 Redis를 설치하고 실행해야 합니다.

### macOS
```bash
# Homebrew로 설치
brew install redis

# Redis 서버 시작
brew services start redis

# 또는 포그라운드 실행
redis-server
```

### Windows
```powershell
# Docker로 실행 (권장)
docker run --name redis -p 6379:6379 -d redis:latest

# 또는 Windows용 Redis 다운로드
# https://github.com/microsoftarchive/redis/releases
```

### Docker Compose
```yaml
# docker-compose.yml
version: '3'
services:
  redis:
    image: redis:latest
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

volumes:
  redis-data:
```

```bash
docker-compose up -d redis
```

---

## 📊 API 작동 상태 확인

### Redis 없이 실행 시

| API | 상태 | 설명 |
|-----|------|------|
| `GET /api/posts` | ✅ 정상 | 게시글 목록 조회 |
| `POST /api/auth/login` | ✅ 정상 | 로그인 |
| `GET /api/v1/chat/rooms` | ✅ 정상 | 채팅방 목록 조회 (DB 기반) |
| `POST /api/v1/chat/rooms` | ✅ 정상 | 채팅방 생성 |
| `WebSocket /ws-stomp` | ⚠️ 제한 | 연결은 가능하나 메시지 전송 불가 |

### Redis 있을 때

| API | 상태 | 설명 |
|-----|------|------|
| 모든 REST API | ✅ 정상 | - |
| 실시간 채팅 | ✅ 정상 | WebSocket + Redis Pub/Sub |
| 안 읽은 메시지 | ✅ 정상 | Redis 캐시 사용 |

---

## 🧪 테스트

### Redis 없이 API 테스트
```bash
# 서버 시작 (Redis 환경 변수 없이)
./gradlew bootRun

# 게시글 API 테스트
curl http://localhost:8081/api/posts

# 로그인 테스트
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password"}'

# ✅ 모두 정상 작동!
```

### Redis 있을 때 채팅 테스트
```bash
# Redis 시작
redis-server

# 환경 변수 설정
export redisHost=localhost
export redisPort=6379

# 서버 시작
./gradlew bootRun

# 채팅 WebSocket 연결
# ws://localhost:8081/ws-stomp

# ✅ 실시간 채팅 작동!
```

---

## 🐛 트러블슈팅

### Q1. Redis 연결 오류가 계속 발생합니다.
**A**: Redis 서버가 실행 중인지 확인하세요.
```bash
# Redis 실행 확인
redis-cli ping
# 응답: PONG

# Redis 서버 시작
redis-server
```

### Q2. 채팅 외 다른 API도 안 됩니다.
**A**: Redis와 관계없이 MySQL이나 다른 설정 문제일 수 있습니다. 로그를 확인하세요.

### Q3. Redis를 완전히 비활성화하고 싶습니다.
**A**: 이미 선택적으로 구성되어 있습니다. `redisHost` 환경 변수만 설정하지 않으면 됩니다.

---

## 📞 문의

Redis 관련 문의사항은 백엔드 팀에 문의해주세요.

- **이메일**: backend@sobunsobun.com
- **Slack**: #backend-support

---

**작성일**: 2026-02-14  
**버전**: v1.0
