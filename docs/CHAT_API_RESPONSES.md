# 📱 채팅 API Response 명세서 (iOS팀용)

> **작성일**: 2026-02-24  
> **Base URL**: `/api/v1/chat`  
> **인증**: 모든 API에 JWT Bearer Token 필요 (`Authorization: Bearer {token}`)

---

## 📌 목차

1. [공통 Response 구조](#1-공통-response-구조)
2. [1:1 채팅방 생성/조회](#2-11-채팅방-생성조회)
3. [단체 채팅방 생성/조회](#3-단체-채팅방-생성조회)
4. [채팅방 목록 조회](#4-채팅방-목록-조회)
5. [채팅방 상세 정보 조회](#5-채팅방-상세-정보-조회)
6. [메시지 조회 (페이징)](#6-메시지-조회-페이징)
7. [과거 메시지 조회 (무한스크롤)](#7-과거-메시지-조회-무한스크롤)
8. [이미지 업로드](#8-이미지-업로드)
9. [메시지 타입 상수](#9-메시지-타입-상수)
10. [에러 Response](#10-에러-response)

---

## 1. 공통 Response 구조

모든 API는 다음과 같은 표준 구조를 따릅니다:

```json
{
  "status": "success",
  "code": 200,
  "data": { ... },
  "message": "작업 완료"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `status` | String | `"success"` 또는 `"error"` |
| `code` | Integer | HTTP 상태 코드 (200, 400, 404 등) |
| `data` | Object | 실제 응답 데이터 (API마다 다름) |
| `message` | String | 응답 메시지 |
| `error` | String? | 에러 코드 (에러 발생 시만) |

---

## 2. 1:1 채팅방 생성/조회

### `POST /api/v1/chat/rooms`

두 사용자 간의 1:1 채팅방을 생성하거나 기존 채팅방을 반환합니다.

### Request
```json
{
  "targetUserId": 123
}
```

### Response
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "roomId": 1,
    "otherUserName": "홍길동",
    "otherUserProfileImageUrl": "/files/profile123.jpg",
    "isNewRoom": true
  },
  "message": "1:1 채팅방 생성/조회 성공"
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | Long | 채팅방 ID (이후 메시지 전송 시 사용) |
| `otherUserName` | String | 상대방 닉네임 |
| `otherUserProfileImageUrl` | String? | 상대방 프로필 이미지 URL |
| `isNewRoom` | Boolean | `true`: 새로 생성됨 / `false`: 기존 방 |

---

## 3. 단체 채팅방 생성/조회

### `POST /api/v1/chat/rooms/group`

공동구매 게시글에 연결된 단체 채팅방을 생성하거나 기존 채팅방을 반환합니다.

### Request
```json
{
  "roomName": "떠나바 모임",
  "groupPostId": 5,
  "memberIds": [2, 3, 4]
}
```

### Response
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
    "isNewRoom": true,
    "message": "✅ 단체 채팅방 생성 완료"
  },
  "message": "단체 채팅방 생성 성공"
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | Long | 채팅방 ID |
| `roomName` | String | 채팅방 이름 |
| `roomType` | String | `"GROUP"` (단체 채팅) |
| `groupPostId` | Long | 연결된 공동구매 게시글 ID |
| `memberCount` | Integer | 현재 멤버 수 |
| `isNewRoom` | Boolean | `true`: 새로 생성됨 / `false`: 기존 방 |

---

## 4. 채팅방 목록 조회

### `GET /api/v1/chat/rooms/list`

사용자가 참여 중인 모든 채팅방 목록을 조회합니다.  
**최신 메시지 순으로 정렬됩니다.**

### Response
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "roomId": 1,
      "roomName": "홍길동",
      "roomType": "ONE_TO_ONE",
      "memberCount": 2,
      "lastMessage": "안녕하세요!",
      "lastMessageTime": "2026-02-24T14:30:00",
      "unreadCount": 3
    },
    {
      "roomId": 10,
      "roomName": "떠나바 모임",
      "roomType": "GROUP",
      "memberCount": 5,
      "lastMessage": "내일 몇 시에 만날까요?",
      "lastMessageTime": "2026-02-24T12:15:00",
      "unreadCount": 0
    }
  ],
  "message": "채팅방 목록 조회 성공"
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | Long | 채팅방 ID |
| `roomName` | String | 채팅방 이름 (1:1은 상대방 닉네임) |
| `roomType` | String | `"ONE_TO_ONE"` 또는 `"GROUP"` |
| `memberCount` | Integer | 멤버 수 |
| `lastMessage` | String? | 마지막 메시지 미리보기 (null 가능) |
| `lastMessageTime` | String? | 마지막 메시지 시간 (ISO 8601, null 가능) |
| `unreadCount` | Long | 안 읽은 메시지 개수 |

### 💡 UI 가이드
- `unreadCount > 0`이면 배지 표시
- `lastMessageTime`은 "방금", "5분 전", "어제", "2월 24일" 형식으로 변환 권장
- 정렬: 이미 `lastMessageTime` 내림차순으로 정렬됨

---

## 5. 채팅방 상세 정보 조회

### `GET /api/v1/chat/rooms/{roomId}/detail`

채팅방의 상세 정보를 조회합니다. 개인/단체 채팅에 따라 응답 구조가 다릅니다.

### Response (1:1 채팅)
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "roomId": 1,
    "roomName": "홍길동",
    "roomType": "ONE_TO_ONE",
    "ownerId": 456,
    "memberCount": 2,
    "unreadCount": 3,
    "lastMessage": "안녕하세요!",
    "lastMessageAt": "2026-02-24T14:30:00",
    "createdAt": "2026-02-20T10:00:00",
    "members": [
      {
        "userId": 123,
        "nickname": "나",
        "profileImage": "/files/profile123.jpg",
        "isOwner": false
      },
      {
        "userId": 456,
        "nickname": "홍길동",
        "profileImage": "/files/profile456.jpg",
        "isOwner": true
      }
    ]
  },
  "message": "채팅방 상세 정보 조회 성공"
}
```

### Response (단체 채팅)
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "roomId": 10,
    "roomName": "떠나바 모임",
    "roomType": "GROUP",
    "ownerId": 100,
    "memberCount": 5,
    "unreadCount": 0,
    "lastMessage": "내일 몇 시에 만날까요?",
    "lastMessageAt": "2026-02-24T12:15:00",
    "createdAt": "2026-02-20T10:00:00",
    "groupPostId": 5,
    "groupPostTitle": "떠나바 같이 가실 분",
    "members": [
      {
        "userId": 100,
        "nickname": "방장님",
        "profileImage": "/files/profile100.jpg",
        "isOwner": true
      },
      {
        "userId": 101,
        "nickname": "멤버1",
        "profileImage": "/files/profile101.jpg",
        "isOwner": false
      }
    ]
  },
  "message": "채팅방 상세 정보 조회 성공"
}
```

### Response 필드 설명

#### 공통 필드
| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | Long | 채팅방 ID |
| `roomName` | String | 채팅방 이름 |
| `roomType` | String | `"ONE_TO_ONE"` 또는 `"GROUP"` |
| `ownerId` | Long | 방장 사용자 ID |
| `memberCount` | Integer | 멤버 수 |
| `unreadCount` | Long | 안 읽은 메시지 개수 |
| `lastMessage` | String? | 마지막 메시지 미리보기 |
| `lastMessageAt` | String? | 마지막 메시지 시간 (ISO 8601) |
| `createdAt` | String | 채팅방 생성 시간 (ISO 8601) |
| `members` | Array | 멤버 목록 |

#### 단체 채팅 전용 필드
| 필드 | 타입 | 설명 |
|------|------|------|
| `groupPostId` | Long? | 연결된 공동구매 게시글 ID |
| `groupPostTitle` | String? | 연결된 공동구매 게시글 제목 |

#### MemberInfo 객체
| 필드 | 타입 | 설명 |
|------|------|------|
| `userId` | Long | 사용자 ID |
| `nickname` | String | 닉네임 |
| `profileImage` | String? | 프로필 이미지 URL |
| `isOwner` | Boolean | 방장 여부 |

### 💡 UI 가이드
- **1:1 채팅**: `members`에서 `userId != 내 ID`인 사람이 상대방
- **단체 채팅**: `members`에서 `isOwner: true`인 사람이 방장
- `groupPostId`가 있으면 게시글 상세 페이지로 이동 가능하게 표시

---

## 6. 메시지 조회 (페이징)

### `GET /api/v1/chat/rooms/{roomId}/messages?page=0&size=50`

채팅방의 메시지를 페이징하여 조회합니다. (최신 메시지부터)

### Request Query Parameters
| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `page` | Integer | 0 | 페이지 번호 (0부터 시작) |
| `size` | Integer | 50 | 한 페이지당 메시지 개수 |

### Response
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "content": [
      {
        "id": 123,
        "roomId": 1,
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
        "readCount": 1,
        "createdAt": "2026-02-24T14:30:00",
        "readByMe": true
      },
      {
        "id": 122,
        "roomId": 1,
        "senderId": 123,
        "userId": 123,
        "senderName": "나",
        "nickname": "나",
        "senderProfileImageUrl": "/files/profile123.jpg",
        "profileImage": "/files/profile123.jpg",
        "type": "IMAGE",
        "content": "사진 보내드려요",
        "imageUrl": "/files/chat-img-abc123.jpg",
        "cardPayload": null,
        "readCount": 2,
        "createdAt": "2026-02-24T14:25:00",
        "readByMe": true
      }
    ],
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

### Response 필드 설명

#### PageResponse 필드
| 필드 | 타입 | 설명 |
|------|------|------|
| `content` | Array | 메시지 목록 (MessageResponse 배열) |
| `totalElements` | Long | 전체 메시지 개수 |
| `totalPages` | Integer | 전체 페이지 수 |
| `currentPage` | Integer | 현재 페이지 번호 (0부터 시작) |
| `size` | Integer | 페이지 크기 |
| `first` | Boolean | 첫 페이지 여부 (`currentPage == 0`) |
| `last` | Boolean | 마지막 페이지 여부 (`currentPage >= totalPages - 1`) |

#### MessageResponse 필드
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
| `type` | String | 메시지 타입 ([타입 목록](#9-메시지-타입-상수) 참고) |
| `content` | String? | 메시지 내용 (텍스트) |
| `imageUrl` | String? | 이미지 URL (IMAGE 타입일 때) |
| `cardPayload` | String? | 카드 JSON (INVITE_CARD, SETTLEMENT_CARD 타입일 때) |
| `readCount` | Integer | 읽은 사람 수 |
| `createdAt` | String | 메시지 전송 시간 (ISO 8601) |
| `readByMe` | Boolean | 내가 읽었는지 여부 |

### 💡 UI 가이드
- **신규 필드 우선 사용**: `userId`, `nickname`, `profileImage` 사용 권장
- **호환 필드**: `senderId`, `senderName`, `senderProfileImageUrl`는 하위 호환용
- `readByMe: false`인 메시지가 화면에 표시되면 읽음 처리 요청 필요
- `type`에 따라 다른 UI 표시:
  - `TEXT`: 일반 말풍선
  - `IMAGE`: 이미지 표시 (`imageUrl` 사용)
  - `SYSTEM`: 시스템 메시지 (중앙 정렬)
  - `ENTER/LEAVE`: 입장/퇴장 알림 (회색 텍스트)
  - `INVITE_CARD/SETTLEMENT_CARD`: 카드 UI (`cardPayload` 파싱 필요)

---

## 7. 과거 메시지 조회 (무한스크롤)

### `GET /api/v1/chat/rooms/{roomId}/messages/cursor?lastMessageId=&size=20`

커서 기반 페이징으로 과거 메시지를 조회합니다. (무한 스크롤용)  
**6번 메시지 조회와 동일한 `MessageResponse` DTO를 사용합니다.**

### Request Query Parameters
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
      "roomId": 1,
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
      "readCount": 2,
      "createdAt": "2026-02-24T14:20:00",
      "readByMe": true
    },
    {
      "id": 122,
      "roomId": 1,
      "senderId": 123,
      "userId": 123,
      "senderName": "나",
      "nickname": "나",
      "senderProfileImageUrl": "/files/profile123.jpg",
      "profileImage": "/files/profile123.jpg",
      "type": "IMAGE",
      "content": "사진 보내드려요",
      "imageUrl": "/files/chat-img-abc123.jpg",
      "cardPayload": null,
      "readCount": 2,
      "createdAt": "2026-02-24T14:25:00",
      "readByMe": true
    },
    {
      "id": 123,
      "roomId": 1,
      "senderId": 456,
      "userId": 456,
      "senderName": "홍길동",
      "nickname": "홍길동",
      "senderProfileImageUrl": "/files/profile456.jpg",
      "profileImage": "/files/profile456.jpg",
      "type": "ENTER",
      "content": "홍길동님이 입장하셨습니다.",
      "imageUrl": null,
      "cardPayload": null,
      "readCount": 0,
      "createdAt": "2026-02-24T14:15:00",
      "readByMe": true
    }
  ],
  "message": "과거 메시지 조회 성공"
}
```

### Response 필드 설명 (MessageResponse — 6번과 동일)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 메시지 ID (다음 요청 시 `lastMessageId`로 사용) ⭐ |
| `roomId` | Long | 채팅방 ID |
| `userId` | Long | 발신자 ID ⭐ **권장** |
| `nickname` | String | 발신자 닉네임 ⭐ **권장** |
| `profileImage` | String? | 발신자 프로필 이미지 URL ⭐ **권장** |
| `senderId` | Long | 발신자 ID (호환용, `userId`와 동일) |
| `senderName` | String | 발신자 이름 (호환용, `nickname`과 동일) |
| `senderProfileImageUrl` | String? | 프로필 URL (호환용, `profileImage`와 동일) |
| `type` | String | 메시지 타입 ([타입 목록](#9-메시지-타입-상수) 참고) |
| `content` | String? | 메시지 내용 (텍스트) |
| `imageUrl` | String? | 이미지 URL (IMAGE 타입일 때) |
| `cardPayload` | String? | 카드 JSON (INVITE_CARD, SETTLEMENT_CARD 타입일 때) |
| `readCount` | Integer | 읽은 사람 수 |
| `createdAt` | String | 메시지 전송 시간 (ISO 8601) |
| `readByMe` | Boolean | 내가 읽었는지 여부 |

### 💡 6번 vs 7번 차이점

| | 6번 `/messages` | 7번 `/messages/cursor` |
|---|---|---|
| **DTO** | `MessageResponse` | `MessageResponse` (동일) |
| **페이징** | offset 기반 (`page`, `size`) | 커서 기반 (`lastMessageId`, `size`) |
| **응답 구조** | `PageResponse<MessageResponse>` (content + 페이지 정보) | `List<MessageResponse>` (배열만) |
| **정렬** | 최신 메시지부터 (내림차순) | 오래된 메시지부터 (오름차순) |
| **용도** | 초기 메시지 로딩 | 무한 스크롤 (위로 당기기) |

### 💡 무한 스크롤 구현 가이드

```swift
// 첫 요청 (채팅방 진입 시)
GET /api/v1/chat/rooms/1/messages/cursor?size=20

// 응답의 첫 번째 메시지 ID: 101 (가장 오래된 메시지)

// 다음 요청 (스크롤 업 — 더 오래된 메시지 로드)
GET /api/v1/chat/rooms/1/messages/cursor?lastMessageId=101&size=20

// 응답이 빈 배열 [] 이면 → 더 이상 과거 메시지 없음
```

### ⚠️ 주의사항
- 메시지는 **시간 오름차순**으로 정렬됩니다 (오래된 것부터)
- 무한 스크롤 시 `lastMessageId`는 **현재 로드된 메시지 중 가장 오래된(첫 번째) 메시지의 `id`**를 사용
- `createdAt`은 ISO 8601 형식입니다 (`"2026-02-24T14:30:00"`)

---

## 8. 이미지 업로드

### `POST /api/v1/chat/rooms/{chatId}/images`

채팅방에 이미지를 업로드하고 이미지 메시지를 전송합니다.

### Request (multipart/form-data)
```
Content-Type: multipart/form-data

image: (파일 데이터)
message: "사진 보내드려요" (선택)
```

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
    "roomId": 1,
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

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 메시지 ID |
| `roomId` | Long | 채팅방 ID |
| `userId` | Long | 발신자 ID |
| `nickname` | String | 발신자 닉네임 |
| `profileImage` | String? | 발신자 프로필 이미지 URL |
| `type` | String | `"IMAGE"` |
| `content` | String? | 함께 전송된 텍스트 메시지 |
| `imageUrl` | String | 업로드된 이미지 URL ⭐ |
| `readCount` | Integer | 읽은 사람 수 (초기값 0) |
| `createdAt` | String | 메시지 전송 시간 (ISO 8601) |
| `readByMe` | Boolean | 항상 `true` (본인이 보낸 메시지) |

### 💡 iOS 구현 예시 (Swift)

```swift
func uploadChatImage(roomId: Int, image: UIImage, message: String?) async throws -> ChatImageMessageResponse {
    guard let imageData = image.jpegData(compressionQuality: 0.8) else {
        throw NSError(domain: "ImageError", code: -1)
    }
    
    let url = URL(string: "https://api.sobunsobun.com/api/v1/chat/rooms/\(roomId)/images")!
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
    
    let boundary = UUID().uuidString
    request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
    
    var body = Data()
    
    // 이미지 파일
    body.append("--\(boundary)\r\n".data(using: .utf8)!)
    body.append("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\n".data(using: .utf8)!)
    body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
    body.append(imageData)
    body.append("\r\n".data(using: .utf8)!)
    
    // 메시지 (선택)
    if let message = message {
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"message\"\r\n\r\n".data(using: .utf8)!)
        body.append("\(message)\r\n".data(using: .utf8)!)
    }
    
    body.append("--\(boundary)--\r\n".data(using: .utf8)!)
    request.httpBody = body
    
    let (data, _) = try await URLSession.shared.data(for: request)
    let response = try JSONDecoder().decode(ApiResponse<ChatImageMessageResponse>.self, from: data)
    return response.data
}
```

---

## 9. 메시지 타입 상수

채팅 메시지의 `type` 필드에 사용되는 상수 목록입니다.

| 타입 | 설명 | UI 처리 |
|------|------|---------|
| `TEXT` | 일반 텍스트 메시지 | 말풍선 표시 (`content` 사용) |
| `IMAGE` | 이미지 메시지 | 이미지 표시 (`imageUrl` 사용) |
| `INVITE_CARD` | 초대장 카드 | 카드 UI (`cardPayload` JSON 파싱) |
| `SETTLEMENT_CARD` | 정산서 카드 | 카드 UI (`cardPayload` JSON 파싱) |
| `SYSTEM` | 시스템 메시지 | 중앙 정렬, 회색 텍스트 |
| `ENTER` | 채팅방 입장 | "○○○님이 입장하셨습니다" (회색) |
| `LEAVE` | 채팅방 퇴장 | "○○○님이 퇴장하셨습니다" (회색) |

### ⚠️ 중요: `TALK` 타입은 존재하지 않습니다
- 일부 문서에 `TALK`로 표기되어 있지만, 실제로는 **`TEXT`**를 사용합니다.
- `TALK` 타입은 더 이상 사용되지 않습니다.

---

## 10. 에러 Response

에러 발생 시 다음과 같은 구조의 응답이 반환됩니다:

```json
{
  "status": "error",
  "code": 404,
  "error": "ROOM_NOT_FOUND",
  "message": "존재하지 않는 채팅방입니다 (roomId: 999)"
}
```

### 주요 에러 코드

| HTTP 코드 | error 코드 | 설명 |
|-----------|-----------|------|
| 400 | `CREATE_PRIVATE_ROOM_FAILED` | 개인 채팅방 생성 실패 |
| 400 | `CREATE_GROUP_ROOM_FAILED` | 단체 채팅방 생성 실패 |
| 400 | `INVITE_FAILED` | 멤버 초대 실패 |
| 400 | `LEAVE_FAILED` | 채팅방 퇴장 실패 |
| 400 | `GET_MESSAGES_FAILED` | 메시지 조회 실패 |
| 400 | `IMAGE_REQUIRED` | 이미지 파일 누락 |
| 400 | `IMAGE_UPLOAD_FAILED` | 이미지 업로드 실패 |
| 403 | `NOT_MEMBER` | 채팅방 멤버가 아님 |
| 403 | `ACCESS_DENIED` | 접근 권한 없음 |
| 404 | `USER_NOT_FOUND` | 존재하지 않는 사용자 |
| 404 | `POST_NOT_FOUND` | 존재하지 않는 게시글 |
| 404 | `ROOM_NOT_FOUND` | 존재하지 않는 채팅방 |
| 500 | `CHAT_ROOM_LIST_FAILED` | 채팅방 목록 조회 실패 (서버 오류) |
| 500 | `GET_CHAT_MESSAGES_FAILED` | 메시지 조회 실패 (서버 오류) |

### 💡 에러 처리 가이드 (Swift)

```swift
if response.status == "error" {
    switch response.code {
    case 400:
        // 잘못된 요청 → 사용자에게 메시지 표시
        showAlert(message: response.message)
    case 403:
        // 권한 없음 → 채팅방 목록으로 이동
        navigateToChatList()
    case 404:
        // 리소스 없음 → 채팅방 목록으로 이동
        navigateToChatList()
    case 500:
        // 서버 오류 → 재시도 또는 고객센터 안내
        showRetryAlert()
    default:
        showAlert(message: "알 수 없는 오류가 발생했습니다")
    }
}
```

---

## 📋 체크리스트 (iOS 개발자용)

### 필수 구현 사항
- [x] **JWT 토큰 인증**: 모든 API 호출 시 `Authorization: Bearer {token}` 헤더 포함
- [x] **에러 처리**: `status`, `code`, `error`, `message` 필드 확인
- [x] **신규 필드 우선 사용**: `userId`, `nickname`, `profileImage` 사용
- [x] **ISO 8601 파싱**: `createdAt`, `lastMessageAt` 등 날짜 필드 파싱
- [x] **Unix timestamp 변환**: `timestamp` 필드를 Date 객체로 변환
- [x] **메시지 타입 분기 처리**: `type` 필드에 따라 다른 UI 표시
- [x] **무한 스크롤 구현**: `lastMessageId` 커서 기반 페이징
- [x] **이미지 업로드**: multipart/form-data 방식 구현

### 권장 사항
- [x] **읽음 처리**: 메시지가 화면에 표시되면 WebSocket으로 읽음 처리 전송
- [x] **안 읽은 메시지 개수 표시**: `unreadCount` 배지 표시
- [x] **프로필 이미지 캐싱**: 같은 사용자 이미지 재사용
- [x] **채팅방 목록 정렬**: 이미 정렬되어 있으므로 순서 유지
- [x] **카드 메시지 파싱**: `cardPayload` JSON 파싱하여 커스텀 UI 표시

---

## 📞 문의

- **백엔드 담당자**: [담당자 이름]
- **API 문서 최신화**: 2026-02-24
- **버전**: v1

---

## 📚 관련 문서

- [단체 채팅 API 문서](./GROUP_CHAT_API.md)
- [WebSocket STOMP 연동 가이드](./WEBSOCKET_GUIDE.md) (별도 작성 필요)
- [Redis 없이 실행하기](./RUNNING_WITHOUT_REDIS.md)
