package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sobunsobun.backend.domain.chat.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 클라이언트(iOS)와 주고받을 채팅 메시지 DTO
 *
 * Redis Pub/Sub으로 전달되는 JSON 데이터 형식이자,
 * WebSocket STOMP를 통해 클라이언트와 통신하는 형식입니다.
 *
 * STOMP 브로드캐스트 시 REST API 커서 조회 응답과 동일한 형식으로 전달됩니다.
 * 필드명 매핑: message→content, timestamp→createdAt(ISO 8601), messageId→id
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    /**
     * 메시지 타입 (ENTER, TALK, LEAVE, TEXT, IMAGE, SYSTEM 등)
     */
    @JsonProperty("type")
    private ChatMessageType type;

    /**
     * 채팅방 ID
     */
    @JsonProperty("roomId")
    private Long roomId;

    /**
     * 메시지 발신자 ID
     */
    @JsonProperty("senderId")
    private Long senderId;

    /**
     * 발신자 이름 (UI 표시용)
     */
    @JsonProperty("senderName")
    private String senderName;

    /**
     * 메시지 내용
     */
    @JsonProperty("message")
    private String message;

    /**
     * 이미지 URL (이미지 메시지일 경우)
     */
    @JsonProperty("imageUrl")
    private String imageUrl;

    /**
     * 카드 페이로드 (초대장, 정산 카드 등 JSON)
     */
    @JsonProperty("cardPayload")
    private String cardPayload;

    /**
     * 메시지 생성 시간 (타임스탬프, Redis 내부 전달용)
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * 메시지 ID (DB에서 저장된 후 반환)
     */
    @JsonProperty("messageId")
    private UUID messageId;

    // ──────────────────────────────────────────────────────
    // REST API 커서 조회 응답과 일치시키기 위한 추가 필드
    // STOMP 브로드캐스트 시 iOS ChatMessageModel과 호환
    // ──────────────────────────────────────────────────────

    /**
     * 발신자 닉네임 (nickname 필드, senderName과 동일)
     */
    @JsonProperty("nickname")
    private String nickname;

    /**
     * 발신자 프로필 이미지 URL
     */
    @JsonProperty("profileImage")
    private String profileImage;

    /**
     * 발신자 프로필 이미지 URL (기존 호환용)
     */
    @JsonProperty("senderProfileImageUrl")
    private String senderProfileImageUrl;

    /**
     * 발신자 고유 ID (senderId와 동일, userId 필드로 전달)
     */
    @JsonProperty("userId")
    private Long userId;

    /**
     * 메시지 생성 시간 (ISO 8601 형식, 예: "2026-02-25T15:51:51+09:00")
     */
    @JsonProperty("createdAt")
    private String createdAt;

    /**
     * 메시지 내용 (REST API와 동일한 필드명)
     */
    @JsonProperty("content")
    private String content;

    /**
     * 메시지 ID (REST API와 동일한 필드명, UUID 문자열)
     */
    @JsonProperty("id")
    private String id;

    /**
     * 읽음 여부
     */
    @JsonProperty("readByMe")
    private Boolean readByMe;

    /**
     * 읽은 횟수
     */
    @JsonProperty("readCount")
    private Integer readCount;

    /**
     * 정산 ID
     */
    @JsonProperty("settlementId")
    private Integer settlementId;

    /**
     * 초대 ID (INVITE_CARD 타입일 때)
     */
    @JsonProperty("inviteId")
    private Long inviteId;

    /**
     * 단체 채팅방 ID
     */
    @JsonProperty("groupChatRoomId")
    private Integer groupChatRoomId;
}
