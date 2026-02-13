package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sobunsobun.backend.domain.chat.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 클라이언트(iOS)와 주고받을 채팅 메시지 DTO
 *
 * Redis Pub/Sub으로 전달되는 JSON 데이터 형식이자,
 * WebSocket STOMP를 통해 클라이언트와 통신하는 형식입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    /**
     * 메시지 타입 (ENTER, TALK, LEAVE, TEXT, IMAGE, SYSTEM 등)
     *
     * ENTER: 사용자 입장
     * TALK: 일반 텍스트 메시지
     * LEAVE: 사용자 퇴장
     * TEXT: 텍스트 메시지
     * IMAGE: 이미지 메시지
     * SYSTEM: 시스템 메시지
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
     * 메시지 생성 시간 (타임스탬프)
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * 메시지 ID (DB에서 저장된 후 반환)
     */
    @JsonProperty("messageId")
    private Long messageId;
}
