# 👥 단체 채팅 API 문서

> **작성일**: 2026-02-23  
> **Base URL**: `/api/v1/chat`  
> **인증**: 모든 API에 JWT Bearer Token 필요 (`Authorization: Bearer {token}`)

---

## 📌 개요

공동구매 게시글(`GroupPost`)에 연결된 단체 채팅방을 관리하는 API입니다.

### 채팅방 타입 구분

| 타입 | enum 값 | 설명 |
|------|---------|------|
| 1:1 개인 채팅 | `ONE_TO_ONE` | 두 사용자 간 대화 |
| 단체 채팅 | `GROUP` | 공동구매 게시글에 연결된 그룹 대화 |

### 핵심 동작 방식

- **getOrCreate 패턴**: 동일 `groupPostId`에 이미 단체 채팅방이 있으면 기존 방을 반환하고, 없으면 새로 생성
- **방장(owner)**: 채팅방을 생성한 사용자가 방장
- **자동 멤버 등록**: 생성 요청자는 자동으로 첫 번째 멤버로 등록
- **중복 멤버 방지**: 이미 멤버인 사용자를 다시 추가하면 무시됨

---

## 1️⃣ 단체 채팅방 생성/조회

### `POST /api/v1/chat/rooms/group`

공동구매 게시글에 연결된 단체 채팅방을 생성하거나, 이미 존재하면 기존 채팅방을 반환합니다.

### Request Body

```json
{
  "roomName": "떠나바 모임",
  "groupPostId": 5,
  "memberIds": [2, 3, 4]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `roomName` | String | ✅ | 채팅방 이름 (비어있으면 게시글 제목 사용) |
| `groupPostId` | Long | ✅ | 연결할 공동구매 게시글 ID |
| `memberIds` | Long[] | ❌ | 초대할 사용자 ID 목록 (생략 가능) |

### Response — 새로 생성된 경우

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "roomId": 10,
    "roomName": "떠나바 모임",
    "roomType": "GROUP",
    "groupPostId": 5,
    "memberCount": 3,
    "isNewRoom": true,
    "message": "✅ 단체 채팅방 생성 성공"
  },
  "message": "단체 채팅방 생성 성공"
}
```

### Response — 기존 방이 있는 경우

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "roomId": 10,
    "roomName": "떠나바 모임",
    "roomType": "GROUP",
    "groupPostId": 5,
    "memberCount": 4,
    "isNewRoom": false,
    "message": "✅ 기존 단체 채팅방 조회 성공"
  },
  "message": "기존 단체 채팅방 조회 성공"
}
```

> ⚠️ 기존 방 조회 시, 요청자가 아직 멤버가 아니면 자동으로 멤버에 추가됩니다.

### Response 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | Long | 채팅방 ID |
| `roomName` | String | 채팅방 이름 |
| `roomType` | String | `"GROUP"` 고정 |
| `groupPostId` | Long | 연결된 공동구매 게시글 ID |
| `memberCount` | Integer | 현재 채팅방 멤버 수 |
| `isNewRoom` | Boolean | `true`=새로 생성, `false`=기존 방 |
| `message` | String | 처리 결과 메시지 |

### Error Responses

| HTTP | 에러 코드 | 상황 |
|------|-----------|------|
| 404 | `POST_NOT_FOUND` | 존재하지 않는 게시글 |
| 400 | `CREATE_GROUP_ROOM_FAILED` | 기타 생성 오류 |

```json
{
  "status": "error",
  "code": 404,
  "error": "POST_NOT_FOUND",
  "message": "존재하지 않는 게시글입니다 (groupPostId: 5)"
}
```

---

## 2️⃣ 단체 채팅방 멤버 초대

### `POST /api/v1/chat/rooms/{roomId}/members/{targetUserId}`

단체 채팅방에 새 멤버를 초대합니다.

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `roomId` | Long | 채팅방 ID |
| `targetUserId` | Long | 초대할 사용자 ID |

### 권한 조건

- 요청자가 해당 채팅방의 **활성 멤버**여야 함
- 대상 채팅방이 **GROUP 타입**이어야 함

### Response — 성공

```json
{
  "status": "success",
  "code": 200,
  "data": null,
  "message": "멤버 초대 성공"
}
```

### Error Responses

| HTTP | 에러 코드 | 상황 |
|------|-----------|------|
| 400 | `INVITE_FAILED` | 채팅방이 GROUP이 아님 |
| 400 | `INVITE_FAILED` | 요청자가 멤버가 아님 |
| 400 | `INVITE_FAILED` | 대상 사용자가 존재하지 않음 |

> 💡 이미 멤버인 사용자를 초대하면 에러 없이 무시됩니다.

---

## 3️⃣ 단체 채팅방 나가기

### `DELETE /api/v1/chat/rooms/{roomId}/members/me`

단체 채팅방에서 나갑니다. 나간 사용자의 상태가 `LEFT`로 변경되며, 더 이상 메시지를 수신하지 않습니다.

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `roomId` | Long | 나갈 채팅방 ID |

### 권한 조건

- 요청자가 해당 채팅방의 **활성 멤버**여야 함
- 대상 채팅방이 **GROUP 타입**이어야 함

### Response — 성공

```json
{
  "status": "success",
  "code": 200,
  "data": null,
  "message": "채팅방 퇴장 성공"
}
```

### Error Responses

| HTTP | 에러 코드 | 상황 |
|------|-----------|------|
| 400 | `LEAVE_FAILED` | 채팅방이 GROUP이 아님 |
| 400 | `LEAVE_FAILED` | 요청자가 멤버가 아님 |
| 400 | `LEAVE_FAILED` | 존재하지 않는 채팅방 |

---

## 4️⃣ 채팅방 상세 정보 조회

### `GET /api/v1/chat/rooms/{roomId}/detail`

개인(ONE_TO_ONE) / 단체(GROUP) 채팅방 모두 지원하는 상세 정보 조회 API입니다.  
채팅방 타입에 따라 응답 구조가 달라집니다.

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `roomId` | Long | 채팅방 ID |

### Response — 개인 채팅방 (ONE_TO_ONE)

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "roomId": 1,
    "roomName": "홍길동",
    "roomType": "ONE_TO_ONE",
    "ownerId": 10,
    "memberCount": 2,
    "unreadCount": 3,
    "lastMessage": "안녕하세요!",
    "lastMessageAt": "2026-02-23T14:30:00",
    "createdAt": "2026-02-20T10:00:00",
    "otherUser": {
      "userId": 20,
      "nickname": "홍길동",
      "profileImage": "/files/profile-hong.jpg",
      "isOwner": false
    },
    "members": [
      {
        "userId": 10,
        "nickname": "김철수",
        "profileImage": "/files/profile-kim.jpg",
        "isOwner": true
      },
      {
        "userId": 20,
        "nickname": "홍길동",
        "profileImage": "/files/profile-hong.jpg",
        "isOwner": false
      }
    ]
  },
  "message": "채팅방 상세 정보 조회 성공"
}
```

> 개인 채팅방: `otherUser`에 상대방 정보 + `members`에 나 포함 전체 멤버 목록이 모두 반환됩니다.

### Response — 단체 채팅방 (GROUP)

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "roomId": 10,
    "roomName": "떠나바 모임",
    "roomType": "GROUP",
    "ownerId": 10,
    "memberCount": 4,
    "unreadCount": 0,
    "lastMessage": "내일 만나요!",
    "lastMessageAt": "2026-02-23T15:00:00",
    "createdAt": "2026-02-21T09:00:00",
    "groupPostId": 5,
    "groupPostTitle": "치킨 소분해요",
    "members": [
      {
        "userId": 10,
        "nickname": "김철수",
        "profileImage": "/files/profile-kim.jpg",
        "isOwner": true
      },
      {
        "userId": 20,
        "nickname": "홍길동",
        "profileImage": "/files/profile-hong.jpg",
        "isOwner": false
      },
      {
        "userId": 30,
        "nickname": "이영희",
        "profileImage": "/files/profile-lee.jpg",
        "isOwner": false
      },
      {
        "userId": 40,
        "nickname": "박민수",
        "profileImage": null,
        "isOwner": false
      }
    ]
  },
  "message": "채팅방 상세 정보 조회 성공"
}
```

> 단체 채팅방: `members` 배열에 모든 활성 멤버 + `groupPostId`/`groupPostTitle` 게시글 정보가 포함됩니다. `otherUser`는 응답에 포함되지 않습니다.

### Response 필드 (전체)

| 필드 | 타입 | 공통/개인/단체 | 설명 |
|------|------|---------------|------|
| `roomId` | Long | 공통 | 채팅방 ID |
| `roomName` | String | 공통 | 채팅방 이름 (개인=상대방 닉네임) |
| `roomType` | String | 공통 | `"ONE_TO_ONE"` / `"GROUP"` |
| `ownerId` | Long | 공통 | 방장 사용자 ID |
| `memberCount` | Integer | 공통 | 활성 멤버 수 |
| `unreadCount` | Long | 공통 | 안 읽은 메시지 수 |
| `lastMessage` | String? | 공통 | 마지막 메시지 내용 |
| `lastMessageAt` | String? | 공통 | 마지막 메시지 시간 (ISO 8601) |
| `createdAt` | String | 공통 | 채팅방 생성 시간 (ISO 8601) |
| `otherUser` | MemberInfo? | **개인 전용** | 상대방 유저 정보 |
| `groupPostId` | Long? | **단체 전용** | 연결된 공동구매 게시글 ID |
| `groupPostTitle` | String? | **단체 전용** | 연결된 게시글 제목 |
| `members` | MemberInfo[] | **공통** | 채팅방 멤버 목록 (개인/단체 모두) |

### MemberInfo 구조

| 필드 | 타입 | 설명 |
|------|------|------|
| `userId` | Long | 사용자 ID |
| `nickname` | String | 닉네임 |
| `profileImage` | String? | 프로필 이미지 URL |
| `isOwner` | Boolean | 방장 여부 |

### Error Responses

| HTTP | 에러 코드 | 상황 |
|------|-----------|------|
| 404 | `ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| 403 | `NOT_MEMBER` | 채팅방 멤버가 아님 |
| 400 | `GET_ROOM_DETAIL_FAILED` | 기타 조회 오류 |

---

## 📋 채팅방 목록 응답 변경사항

기존 `GET /api/v1/chat/rooms/list` 응답에 `roomType`, `memberCount` 필드가 추가되었습니다.

### 변경 전

```json
{
  "roomId": 1,
  "roomName": "홍길동",
  "lastMessage": "안녕하세요",
  "lastMessageTime": "2026-02-23T14:30:00",
  "unreadCount": 3
}
```

### 변경 후

```json
{
  "roomId": 1,
  "roomName": "홍길동",
  "roomType": "ONE_TO_ONE",
  "memberCount": 2,
  "lastMessage": "안녕하세요",
  "lastMessageTime": "2026-02-23T14:30:00",
  "unreadCount": 3
}
```

```json
{
  "roomId": 10,
  "roomName": "떠나바 모임",
  "roomType": "GROUP",
  "memberCount": 5,
  "lastMessage": "내일 만나요!",
  "lastMessageTime": "2026-02-23T15:00:00",
  "unreadCount": 0
}
```

| 추가 필드 | 타입 | 설명 |
|-----------|------|------|
| `roomType` | String | `"ONE_TO_ONE"` 또는 `"GROUP"` |
| `memberCount` | Integer | 채팅방 활성 멤버 수 |

---

## 🗄️ DB 변경사항

### `chat_room` 테이블 — 컬럼 추가

| 컬럼 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `group_post_id` | BIGINT | ✅ YES | 연결된 `group_post.id` (FK) |

> `ddl-auto: update` 설정이므로 서버 시작 시 자동 반영됩니다.  
> 1:1 채팅방은 `group_post_id = NULL`로 유지됩니다.

### ERD 관계

```
group_post (1) ──── (0..1) chat_room
                            │
                            ├── room_type = 'GROUP'
                            └── group_post_id = group_post.id
```

---

## 🔄 전체 채팅 API 요약

### 개인 채팅 (기존)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/rooms/private` | 개인 채팅방 생성/조회 (레거시) |
| POST | `/rooms` | 1:1 채팅방 생성/조회 |

### 단체 채팅 (신규 ✨)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/rooms/group` | 단체 채팅방 생성/조회 |
| POST | `/rooms/{roomId}/members/{targetUserId}` | 멤버 초대 |
| DELETE | `/rooms/{roomId}/members/me` | 채팅방 나가기 |

### 공통

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/rooms` | 채팅방 목록 (페이징) |
| GET | `/rooms/list` | 채팅방 목록 (iOS용, 전체) |
| GET | `/rooms/{roomId}/detail` | 채팅방 상세 정보 (개인/단체 공통) |
| GET | `/rooms/{roomId}/messages` | 메시지 조회 (페이징) |
| GET | `/rooms/{roomId}/messages/cursor` | 메시지 조회 (커서 무한스크롤) |
| POST | `/rooms/{chatId}/images` | 이미지 업로드 |

### WebSocket STOMP (개인/단체 동일)

| 경로 | 설명 |
|------|------|
| `SEND /app/chat/send` | 메시지 전송 → `/topic/chat/room/{roomId}` 브로드캐스트 |
| `SEND /app/chat/read` | 읽음 처리 → `/user/{userId}/queue/private` 알림 |

---

## 🔧 iOS 연동 가이드

### 1. 단체 채팅방 생성

```swift
// POST /api/v1/chat/rooms/group
let body: [String: Any] = [
    "roomName": "떠나바 모임",
    "groupPostId": 5,
    "memberIds": [2, 3, 4]  // 선택
]
// Response → roomId를 저장하여 이후 메시지 전송에 사용
```

### 2. 채팅방 목록에서 타입 구분

```swift
// GET /api/v1/chat/rooms/list
// roomType으로 1:1 / 단체 구분
if room.roomType == "GROUP" {
    // 단체 채팅 UI (멤버 수 표시, 그룹 아이콘 등)
    print("멤버 \(room.memberCount)명")
} else {
    // 1:1 채팅 UI
}
```

### 3. 메시지 전송 (동일)

단체 채팅의 메시지 전송/수신은 1:1과 **완전히 동일**합니다.  
STOMP `/app/chat/send`로 `roomId`와 함께 전송하면 해당 방의 모든 멤버에게 브로드캐스트됩니다.

```swift
// WebSocket STOMP - 개인/단체 동일
let message: [String: Any] = [
    "roomId": 10,       // 단체 채팅방 ID
    "type": "TALK",
    "content": "안녕하세요!"
]
stompClient.send(destination: "/app/chat/send", body: message)
```

### 4. 채팅방 상세 정보 조회

```swift
// GET /api/v1/chat/rooms/{roomId}/detail
// 채팅방 입장 시 또는 정보 화면에서 호출

if detail.roomType == "ONE_TO_ONE" {
    // 개인 채팅: 상대방 정보 표시
    let other = detail.otherUser
    print("상대방: \(other.nickname)")
    // profileImage로 상대방 프로필 이미지 표시
} else {
    // 단체 채팅: 멤버 목록 표시
    print("멤버 \(detail.memberCount)명")
    for member in detail.members {
        print("\(member.nickname) \(member.isOwner ? "(방장)" : "")")
    }
    // groupPostTitle로 연결된 게시글 정보 표시
    if let title = detail.groupPostTitle {
        print("연결된 게시글: \(title)")
    }
}
```

### 5. 멤버 초대

```swift
// POST /api/v1/chat/rooms/{roomId}/members/{targetUserId}
// Body 없음, Path에 roomId와 초대할 사용자 ID
```

### 6. 채팅방 나가기

```swift
// DELETE /api/v1/chat/rooms/{roomId}/members/me
// Body 없음
```

---

## ⚠️ 에러 코드 전체

| 에러 코드 | HTTP | 설명 |
|-----------|------|------|
| `POST_NOT_FOUND` | 404 | 존재하지 않는 게시글 |
| `CREATE_GROUP_ROOM_FAILED` | 400 | 단체 채팅방 생성 실패 |
| `INVITE_FAILED` | 400 | 멤버 초대 실패 |
| `LEAVE_FAILED` | 400 | 채팅방 나가기 실패 |
| `ROOM_NOT_FOUND` | 404 | 존재하지 않는 채팅방 |
| `NOT_MEMBER` | 403 | 채팅방 멤버가 아님 |
| `GET_ROOM_DETAIL_FAILED` | 400 | 채팅방 상세 조회 실패 |
