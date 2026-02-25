# 👥 단체 채팅방 종합 가이드

> **작성일**: 2026-02-24  
> **Base URL**: `/api/v1/chat`  
> **인증**: 모든 API에 JWT Bearer Token 필요 (`Authorization: Bearer {token}`)  
> **WebSocket**: STOMP 프로토콜 (`/ws` 엔드포인트)

---

## 📌 목차

1. [개요](#1-개요)
2. [채팅방 타입 구분](#2-채팅방-타입-구분)
3. [단체 채팅방 생성/조회](#3-단체-채팅방-생성조회)
4. [단체 채팅방 멤버 초대](#4-단체-채팅방-멤버-초대)
5. [단체 채팅방 나가기](#5-단체-채팅방-나가기)
6. [채팅방 상세 정보 조회](#6-채팅방-상세-정보-조회)
7. [채팅방 목록 조회](#7-채팅방-목록-조회)
8. [메시지 전송 (WebSocket STOMP)](#8-메시지-전송-websocket-stomp)
9. [메시지 조회 (REST API)](#9-메시지-조회-rest-api)
10. [읽음 처리](#10-읽음-처리)
11. [이미지 업로드](#11-이미지-업로드)
12. [DB 스키마 & ERD](#12-db-스키마--erd)
13. [에러 코드 전체](#13-에러-코드-전체)
14. [iOS 연동 가이드](#14-ios-연동-가이드)
15. [전체 API 요약](#15-전체-api-요약)

---

## 1. 개요

공동구매 게시글(`GroupPost`)에 연결된 **단체 채팅방**을 관리하는 기능입니다.  
1:1 채팅과 동일한 WebSocket STOMP 인프라를 사용하며, 단체 채팅 고유의 멤버 관리(초대/퇴장) 기능이 추가됩니다.

### 핵심 동작 방식

| 개념 | 설명 |
|------|------|
| **getOrCreate 패턴** | 동일 `groupPostId`에 이미 단체 채팅방이 있으면 기존 방을 반환하고, 없으면 새로 생성 |
| **방장(owner)** | 채팅방을 최초 생성한 사용자가 방장 |
| **자동 멤버 등록** | 생성 요청자는 자동으로 첫 번째 멤버로 등록 |
| **중복 멤버 방지** | 이미 멤버인 사용자를 다시 추가하면 에러 없이 무시 |
| **멤버 상태 관리** | `ACTIVE`(활성) / `LEFT`(퇴장) 상태로 멤버를 관리 |
| **메시지 브로드캐스트** | 1:1과 동일하게 `/topic/chat/room/{roomId}`로 모든 멤버에게 전송 |

---

## 2. 채팅방 타입 구분

```java
public enum ChatRoomType {
    ONE_TO_ONE,  // 1:1 개인 채팅
    GROUP        // 단체 채팅
}
```

| 타입 | enum 값 | 설명 | 특이사항 |
|------|---------|------|----------|
| 1:1 개인 채팅 | `ONE_TO_ONE` | 두 사용자 간 대화 | `groupPostId = null` |
| 단체 채팅 | `GROUP` | 공동구매 게시글에 연결된 그룹 대화 | `groupPostId = 게시글 ID` |

---

## 3. 단체 채팅방 생성/조회

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
| `memberIds` | Long[] | ❌ | 초대할 사용자 ID 목록 (생략 가능, null 가능) |

### 처리 순서

```
① groupPostId로 게시글 존재 여부 확인
② 기존 단체 채팅방이 있는지 확인 (chatRoomRepository.findByGroupPostId)
   ├─ 있으면 → 기존 방 반환 (요청자가 멤버가 아니면 자동 추가)
   └─ 없으면 → 새 ChatRoom(type=GROUP) 생성
③ 요청자(owner)를 첫 번째 멤버로 추가
④ memberIds에 포함된 사용자들을 멤버로 추가 (존재하지 않는 사용자는 건너뜀)
```

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

> ⚠️ 기존 방 조회 시, 요청자가 아직 멤버가 아니면 **자동으로 멤버에 추가**됩니다.

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

## 4. 단체 채팅방 멤버 초대

### `POST /api/v1/chat/rooms/{roomId}/members/{targetUserId}`

단체 채팅방에 새 멤버를 초대합니다. Body 없이 Path Parameter만으로 동작합니다.

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `roomId` | Long | 채팅방 ID |
| `targetUserId` | Long | 초대할 사용자 ID |

### 권한 조건

- 요청자가 해당 채팅방의 **활성 멤버(ACTIVE)**여야 함
- 대상 채팅방이 **GROUP 타입**이어야 함

### 처리 순서

```
① 채팅방 존재 여부 확인
② 채팅방 타입이 GROUP인지 확인
③ 요청자가 활성 멤버인지 확인
④ 대상 사용자가 이미 멤버인지 확인 → 이미 멤버면 무시 (에러 없음)
⑤ 대상 사용자를 멤버로 추가 (status=ACTIVE)
```

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

## 5. 단체 채팅방 나가기

### `DELETE /api/v1/chat/rooms/{roomId}/members/me`

단체 채팅방에서 나갑니다. 나간 사용자의 상태가 `ACTIVE` → `LEFT`로 변경되며, 더 이상 메시지를 수신하지 않습니다.

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `roomId` | Long | 나갈 채팅방 ID |

### 권한 조건

- 요청자가 해당 채팅방의 **활성 멤버**여야 함
- 대상 채팅방이 **GROUP 타입**이어야 함

### 처리 순서

```
① 채팅방 존재 여부 확인
② 채팅방 타입이 GROUP인지 확인
③ 요청자가 활성 멤버인지 확인
④ 멤버 상태를 LEFT로 변경
```

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

> ⚠️ 방장이 나가도 채팅방은 유지됩니다. 방장 위임 기능은 현재 미지원입니다.

---

## 6. 채팅방 상세 정보 조회

### `GET /api/v1/chat/rooms/{roomId}/detail`

개인(ONE_TO_ONE) / 단체(GROUP) 채팅방 모두 지원하는 상세 정보 조회 API입니다.  
채팅방 타입에 따라 응답 구조가 달라집니다.

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `roomId` | Long | 채팅방 ID |

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

### Response — 개인 채팅방 (ONE_TO_ONE) (참고용)

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

### Response 필드 (전체)

| 필드 | 타입 | 공통/개인/단체 | 설명 |
|------|------|---------------|------|
| `roomId` | Long | 공통 | 채팅방 ID |
| `roomName` | String | 공통 | 채팅방 이름 (개인=상대방 닉네임) |
| `roomType` | String | 공통 | `"ONE_TO_ONE"` / `"GROUP"` |
| `ownerId` | Long | 공통 | 방장 사용자 ID |
| `memberCount` | Integer | 공통 | 활성 멤버 수 (`ACTIVE` 상태만 카운트) |
| `unreadCount` | Long | 공통 | 안 읽은 메시지 수 (Redis 기반) |
| `lastMessage` | String? | 공통 | 마지막 메시지 내용 |
| `lastMessageAt` | String? | 공통 | 마지막 메시지 시간 (ISO 8601) |
| `createdAt` | String | 공통 | 채팅방 생성 시간 (ISO 8601) |
| `groupPostId` | Long? | **단체 전용** | 연결된 공동구매 게시글 ID |
| `groupPostTitle` | String? | **단체 전용** | 연결된 게시글 제목 |
| `members` | MemberInfo[] | 공통 | 채팅방 활성 멤버 목록 |

### MemberInfo 구조

| 필드 | 타입 | 설명 |
|------|------|------|
| `userId` | Long | 사용자 ID |
| `nickname` | String | 닉네임 |
| `profileImage` | String? | 프로필 이미지 URL (없으면 null) |
| `isOwner` | Boolean | 방장 여부 |

### Error Responses

| HTTP | 에러 코드 | 상황 |
|------|-----------|------|
| 404 | `ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| 403 | `NOT_MEMBER` | 채팅방 멤버가 아님 |
| 400 | `GET_ROOM_DETAIL_FAILED` | 기타 조회 오류 |

---

## 7. 채팅방 목록 조회

### `GET /api/v1/chat/rooms/list`

사용자가 참여 중인 모든 채팅방 목록을 조회합니다.  
**최신 메시지 순(lastMessageTime 내림차순)**으로 정렬됩니다.  
1:1과 단체 채팅방이 함께 반환됩니다.

### Response

```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "roomId": 10,
      "roomName": "떠나바 모임",
      "roomType": "GROUP",
      "memberCount": 5,
      "lastMessage": "내일 몇 시에 만날까요?",
      "lastMessageTime": "2026-02-24T12:15:00",
      "unreadCount": 0
    },
    {
      "roomId": 1,
      "roomName": "홍길동",
      "roomType": "ONE_TO_ONE",
      "memberCount": 2,
      "lastMessage": "안녕하세요!",
      "lastMessageTime": "2026-02-24T14:30:00",
      "unreadCount": 3
    }
  ],
  "message": "채팅방 목록 조회 성공"
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | Long | 채팅방 ID |
| `roomName` | String | 채팅방 이름 (1:1은 상대방 닉네임, 단체는 설정된 이름) |
| `roomType` | String | `"ONE_TO_ONE"` 또는 `"GROUP"` |
| `memberCount` | Integer | 멤버 수 |
| `lastMessage` | String? | 마지막 메시지 미리보기 (null 가능) |
| `lastMessageTime` | String? | 마지막 메시지 시간 (ISO 8601, null 가능) |
| `unreadCount` | Long | 안 읽은 메시지 개수 (Redis 기반) |

### 💡 UI 가이드

- `roomType == "GROUP"` → 단체 채팅 UI (멤버 수 표시, 그룹 아이콘)
- `roomType == "ONE_TO_ONE"` → 1:1 채팅 UI
- `unreadCount > 0`이면 배지 표시
- 이미 정렬되어 있으므로 순서 유지

---

## 8. 메시지 전송 (WebSocket STOMP)

단체 채팅의 메시지 전송/수신은 1:1과 **완전히 동일**합니다.

### WebSocket 연결

```
WebSocket 엔드포인트: /ws
STOMP 프로토콜 사용
```

### 메시지 전송

```
SEND /app/chat/send
```

```json
{
  "roomId": 10,
  "type": "TEXT",
  "content": "안녕하세요!",
  "imageUrl": null,
  "cardPayload": null
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `roomId` | Long | ✅ | 채팅방 ID |
| `type` | String | ✅ | 메시지 타입 (`TEXT`, `IMAGE`, `SYSTEM` 등) |
| `content` | String | ❌ | 텍스트 메시지 내용 |
| `imageUrl` | String | ❌ | 이미지 URL (IMAGE 타입) |
| `cardPayload` | String | ❌ | 카드 JSON (INVITE_CARD, SETTLEMENT_CARD 타입) |

### 메시지 수신 (구독)

```
SUBSCRIBE /topic/chat/room/{roomId}
```

채팅방에 입장하면 해당 토픽을 구독합니다. 해당 방의 모든 멤버에게 메시지가 브로드캐스트됩니다.

### 수신 메시지 형식

```json
{
  "id": 123,
  "roomId": 10,
  "senderId": 456,
  "userId": 456,
  "senderName": "홍길동",
  "nickname": "홍길동",
  "senderProfileImageUrl": "/files/profile456.jpg",
  "profileImage": "/files/profile456.jpg",
  "type": "TEXT",
  "content": "안녕하세요!",
  "imageUrl": null,
  "cardPayload": null,
  "readCount": 0,
  "createdAt": "2026-02-24T14:30:00",
  "readByMe": false
}
```

### 메시지 타입 상수

| 타입 | 설명 | UI 처리 |
|------|------|---------|
| `TEXT` | 일반 텍스트 메시지 | 말풍선 표시 (`content` 사용) |
| `IMAGE` | 이미지 메시지 | 이미지 표시 (`imageUrl` 사용) |
| `INVITE_CARD` | 초대장 카드 | 카드 UI (`cardPayload` JSON 파싱) |
| `SETTLEMENT_CARD` | 정산서 카드 | 카드 UI (`cardPayload` JSON 파싱) |
| `SYSTEM` | 시스템 메시지 | 중앙 정렬, 회색 텍스트 |
| `ENTER` | 채팅방 입장 | "○○○님이 입장하셨습니다" (회색) |
| `LEAVE` | 채팅방 퇴장 | "○○○님이 퇴장하셨습니다" (회색) |

> ⚠️ `TALK` 타입은 더 이상 사용되지 않습니다. **`TEXT`**를 사용하세요.

---

## 9. 메시지 조회 (REST API)

### 9-1. 페이징 조회

#### `GET /api/v1/chat/rooms/{roomId}/messages?page=0&size=50`

최신 메시지부터 내림차순으로 조회합니다.

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `page` | Integer | 0 | 페이지 번호 (0부터) |
| `size` | Integer | 50 | 한 페이지당 메시지 개수 |

### Response

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "content": [ /* MessageResponse 배열 */ ],
    "totalElements": 100,
    "totalPages": 2,
    "currentPage": 0,
    "size": 50,
    "first": true,
    "last": false
  },
  "message": "메시지 조회 완료"
}
```

### 9-2. 커서 기반 조회 (무한 스크롤)

#### `GET /api/v1/chat/rooms/{roomId}/messages/cursor?lastMessageId=&size=20`

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `lastMessageId` | Long | ❌ | null | 마지막으로 받은 메시지 ID (첫 요청 시 생략) |
| `size` | Integer | ❌ | 20 | 한 번에 가져올 메시지 개수 |

### Response

```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 121,
      "roomId": 10,
      "userId": 456,
      "nickname": "홍길동",
      "profileImage": "/files/profile456.jpg",
      "type": "TEXT",
      "content": "안녕하세요!",
      "imageUrl": null,
      "cardPayload": null,
      "readCount": 2,
      "createdAt": "2026-02-24T14:20:00",
      "readByMe": true
    }
  ],
  "message": "과거 메시지 조회 성공"
}
```

### MessageResponse 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 메시지 ID |
| `roomId` | Long | 채팅방 ID |
| `userId` | Long | 발신자 ID ⭐ **권장** |
| `nickname` | String | 발신자 닉네임 ⭐ **권장** |
| `profileImage` | String? | 발신자 프로필 이미지 URL ⭐ **권장** |
| `senderId` | Long | 발신자 ID (호환용, `userId`와 동일) |
| `senderName` | String | 발신자 이름 (호환용, `nickname`과 동일) |
| `senderProfileImageUrl` | String? | 프로필 URL (호환용, `profileImage`와 동일) |
| `type` | String | 메시지 타입 |
| `content` | String? | 메시지 내용 |
| `imageUrl` | String? | 이미지 URL (IMAGE 타입) |
| `cardPayload` | String? | 카드 JSON (INVITE_CARD, SETTLEMENT_CARD 타입) |
| `readCount` | Integer | 읽은 사람 수 |
| `createdAt` | String | 메시지 전송 시간 (ISO 8601) |
| `readByMe` | Boolean | 내가 읽었는지 여부 |

### 9-1 vs 9-2 차이점

| | `/messages` (페이징) | `/messages/cursor` (커서) |
|---|---|---|
| **페이징** | offset 기반 (`page`, `size`) | 커서 기반 (`lastMessageId`, `size`) |
| **응답 구조** | `PageResponse<MessageResponse>` | `List<MessageResponse>` (배열만) |
| **정렬** | 최신 → 과거 (내림차순) | 과거 → 최신 (오름차순) |
| **용도** | 초기 메시지 로딩 | 무한 스크롤 (위로 당기기) |

---

## 10. 읽음 처리

### WebSocket STOMP

```
SEND /app/chat/read
```

```json
{
  "roomId": 10,
  "lastReadMessageId": 123
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | Long | 채팅방 ID |
| `lastReadMessageId` | Long | 마지막으로 읽은 메시지 ID |

### 읽음 완료 알림 (서버 → 클라이언트)

```
구독: /user/{userId}/queue/private
```

```json
{
  "type": "READ_COMPLETE",
  "roomId": 10,
  "message": "✅ 읽음 처리 완료"
}
```

---

## 11. 이미지 업로드

### `POST /api/v1/chat/rooms/{chatId}/images`

`Content-Type: multipart/form-data`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `image` | File | ✅ | 이미지 파일 (jpg/png/webp, 최대 5MB) |
| `message` | String | ❌ | 이미지와 함께 보낼 텍스트 |

### Response

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 124,
    "roomId": 10,
    "userId": 123,
    "nickname": "나",
    "profileImage": "/files/profile123.jpg",
    "type": "IMAGE",
    "content": "사진 보내드려요",
    "imageUrl": "/files/chat-img-abc123.jpg",
    "readCount": 0,
    "createdAt": "2026-02-24T14:35:00",
    "readByMe": true
  },
  "message": "이미지 업로드 성공"
}
```

### Error Responses

| HTTP | 에러 코드 | 상황 |
|------|-----------|------|
| 400 | `IMAGE_REQUIRED` | 이미지 파일 누락 |
| 400 | `IMAGE_UPLOAD_FAILED` | 이미지 업로드 실패 |
| 403 | `NOT_MEMBER` | 채팅방 멤버가 아님 |

---

## 12. DB 스키마 & ERD

### `chat_room` 테이블

| 컬럼 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `id` | BIGINT (PK) | ❌ | 채팅방 ID (auto increment) |
| `name` | VARCHAR(255) | ❌ | 채팅방 이름 |
| `room_type` | VARCHAR(20) | ❌ | `ONE_TO_ONE` / `GROUP` |
| `owner_id` | BIGINT (FK) | ❌ | 방장 사용자 ID → `user.id` |
| `group_post_id` | BIGINT (FK) | ✅ | 연결된 게시글 ID → `group_post.id` |
| `last_message_at` | DATETIME | ✅ | 마지막 메시지 시간 |
| `last_message_preview` | VARCHAR(500) | ✅ | 마지막 메시지 미리보기 |
| `last_message_sender_id` | BIGINT | ✅ | 마지막 메시지 발신자 ID |
| `message_count` | BIGINT | ✅ | 메시지 총 개수 |
| `created_at` | DATETIME | ❌ | 생성 시간 |
| `updated_at` | DATETIME | ❌ | 수정 시간 |

### `chat_member` 테이블

| 컬럼 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `id` | BIGINT (PK) | ❌ | 멤버 ID (auto increment) |
| `chat_room_id` | BIGINT (FK) | ❌ | 채팅방 ID → `chat_room.id` |
| `user_id` | BIGINT (FK) | ❌ | 사용자 ID → `user.id` |
| `status` | VARCHAR(20) | ❌ | `ACTIVE` / `LEFT` |
| `created_at` | DATETIME | ❌ | 참여 시간 |

### ERD 관계

```
group_post (1) ──── (0..1) chat_room
                            │
                            ├── room_type = 'GROUP'
                            ├── group_post_id = group_post.id
                            │
                            └── (1) ──── (*) chat_member
                                            │
                                            ├── user_id → user.id
                                            └── status: ACTIVE / LEFT

chat_room (1) ──── (*) chat_message
                        │
                        ├── sender_id → user.id
                        ├── type: TEXT / IMAGE / SYSTEM / ENTER / LEAVE / ...
                        └── content, image_url, card_payload
```

> `ddl-auto: update` 설정이므로 서버 시작 시 자동 반영됩니다.  
> 1:1 채팅방은 `group_post_id = NULL`로 유지됩니다.

---

## 13. 에러 코드 전체

| HTTP 코드 | error 코드 | 설명 |
|-----------|-----------|------|
| 400 | `CREATE_GROUP_ROOM_FAILED` | 단체 채팅방 생성 실패 |
| 400 | `INVITE_FAILED` | 멤버 초대 실패 (GROUP 아님 / 권한 없음 / 사용자 없음) |
| 400 | `LEAVE_FAILED` | 채팅방 퇴장 실패 (GROUP 아님 / 멤버 아님 / 방 없음) |
| 400 | `GET_MESSAGES_FAILED` | 메시지 조회 실패 |
| 400 | `IMAGE_REQUIRED` | 이미지 파일 누락 |
| 400 | `IMAGE_UPLOAD_FAILED` | 이미지 업로드 실패 |
| 400 | `GET_ROOM_DETAIL_FAILED` | 채팅방 상세 조회 실패 |
| 403 | `NOT_MEMBER` | 채팅방 멤버가 아님 |
| 403 | `ACCESS_DENIED` | 접근 권한 없음 |
| 404 | `POST_NOT_FOUND` | 존재하지 않는 게시글 |
| 404 | `ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| 404 | `USER_NOT_FOUND` | 존재하지 않는 사용자 |
| 500 | `CHAT_ROOM_LIST_FAILED` | 채팅방 목록 조회 서버 오류 |
| 500 | `GET_CHAT_MESSAGES_FAILED` | 과거 메시지 조회 서버 오류 |

### 에러 응답 구조

```json
{
  "status": "error",
  "code": 400,
  "error": "INVITE_FAILED",
  "message": "단체 채팅방에서만 멤버를 초대할 수 있습니다"
}
```

---

## 14. iOS 연동 가이드

### 14-1. 단체 채팅방 생성 플로우

```swift
// 1. 단체 채팅방 생성
// POST /api/v1/chat/rooms/group
let body: [String: Any] = [
    "roomName": "떠나바 모임",
    "groupPostId": 5,
    "memberIds": [2, 3, 4]  // 선택
]
// Response → roomId 저장

// 2. WebSocket 구독 (채팅방 입장)
stompClient.subscribe(destination: "/topic/chat/room/\(roomId)")

// 3. 과거 메시지 로드
// GET /api/v1/chat/rooms/{roomId}/messages/cursor?size=20

// 4. 메시지 전송
let message: [String: Any] = [
    "roomId": roomId,
    "type": "TEXT",
    "content": "안녕하세요!"
]
stompClient.send(destination: "/app/chat/send", body: message)
```

### 14-2. 채팅방 목록에서 타입 구분

```swift
// GET /api/v1/chat/rooms/list
for room in chatRoomList {
    if room.roomType == "GROUP" {
        // 단체 채팅 UI (멤버 수 표시, 그룹 아이콘)
        cell.titleLabel.text = "\(room.roomName) (\(room.memberCount))"
        cell.iconView.image = UIImage(named: "group_chat_icon")
    } else {
        // 1:1 채팅 UI
        cell.titleLabel.text = room.roomName
        cell.iconView.image = UIImage(named: "private_chat_icon")
    }
    
    if room.unreadCount > 0 {
        cell.badgeLabel.text = "\(room.unreadCount)"
        cell.badgeLabel.isHidden = false
    }
}
```

### 14-3. 채팅방 상세 정보 화면

```swift
// GET /api/v1/chat/rooms/{roomId}/detail
if detail.roomType == "GROUP" {
    // 단체 채팅방 정보 화면
    titleLabel.text = detail.roomName
    memberCountLabel.text = "멤버 \(detail.memberCount)명"
    
    // 멤버 목록 표시
    for member in detail.members {
        let label = "\(member.nickname)\(member.isOwner ? " (방장)" : "")"
        // ...
    }
    
    // 연결된 게시글 표시
    if let postTitle = detail.groupPostTitle {
        postButton.setTitle("📦 \(postTitle)", for: .normal)
        postButton.tag = detail.groupPostId ?? 0
    }
}
```

### 14-4. 멤버 초대 & 나가기

```swift
// 멤버 초대
// POST /api/v1/chat/rooms/{roomId}/members/{targetUserId}
// Body 없음

// 채팅방 나가기
// DELETE /api/v1/chat/rooms/{roomId}/members/me
// Body 없음
// 성공 후 → 채팅방 목록으로 이동
```

### 14-5. 무한 스크롤 구현

```swift
// 첫 요청 (채팅방 진입 시)
GET /api/v1/chat/rooms/{roomId}/messages/cursor?size=20

// 스크롤 업 → 더 오래된 메시지 로드
// 응답의 첫 번째(가장 오래된) 메시지의 id를 커서로 사용
GET /api/v1/chat/rooms/{roomId}/messages/cursor?lastMessageId=101&size=20

// 응답이 빈 배열 [] → 더 이상 과거 메시지 없음
```

### 14-6. 에러 처리

```swift
if response.status == "error" {
    switch response.code {
    case 400:
        showAlert(message: response.message)
    case 403:
        // 권한 없음 → 채팅방 목록으로 이동
        navigateToChatList()
    case 404:
        // 리소스 없음 → 채팅방 목록으로 이동
        navigateToChatList()
    case 500:
        showRetryAlert()
    default:
        showAlert(message: "알 수 없는 오류가 발생했습니다")
    }
}
```

---

## 15. 전체 API 요약

### 단체 채팅 전용 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/chat/rooms/group` | 단체 채팅방 생성/조회 |
| `POST` | `/api/v1/chat/rooms/{roomId}/members/{targetUserId}` | 멤버 초대 |
| `DELETE` | `/api/v1/chat/rooms/{roomId}/members/me` | 채팅방 나가기 |

### 개인/단체 공통 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/v1/chat/rooms` | 채팅방 목록 (페이징) |
| `GET` | `/api/v1/chat/rooms/list` | 채팅방 목록 (iOS용, 전체) |
| `GET` | `/api/v1/chat/rooms/{roomId}/detail` | 채팅방 상세 정보 |
| `GET` | `/api/v1/chat/rooms/{roomId}/messages` | 메시지 조회 (페이징) |
| `GET` | `/api/v1/chat/rooms/{roomId}/messages/cursor` | 과거 메시지 조회 (무한스크롤) |
| `POST` | `/api/v1/chat/rooms/{chatId}/images` | 이미지 업로드 |

### WebSocket STOMP

| 경로 | 방향 | 설명 |
|------|------|------|
| `SEND /app/chat/send` | 클라 → 서버 | 메시지 전송 |
| `SUBSCRIBE /topic/chat/room/{roomId}` | 서버 → 클라 | 채팅방 메시지 수신 (브로드캐스트) |
| `SEND /app/chat/read` | 클라 → 서버 | 읽음 처리 |
| `SUBSCRIBE /user/{userId}/queue/private` | 서버 → 클라 | 개인 알림 (읽음 완료 등) |

---

## 📞 문의

- **API 문서 최신화**: 2026-02-24
- **버전**: v1

---

## 📚 관련 문서

- [채팅 API Response 명세서 (iOS팀용)](./CHAT_API_RESPONSES.md)
- [기존 단체 채팅 API 문서](./GROUP_CHAT_API.md)
- [Redis 없이 실행하기](./RUNNING_WITHOUT_REDIS.md)
