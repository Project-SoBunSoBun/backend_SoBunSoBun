package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.domain.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket 메시지 전송 요청 DTO
 *
 * 클라이언트 → 서버: /app/chat/send
 *
 * 타입별 사용:
 * - TEXT: content 사용
 * - IMAGE: imageUrl 사용 (또는 imageData로 처리 후 저장)
 * - INVITE_CARD: cardPayload 사용
 * - SETTLEMENT_CARD: cardPayload 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendChatMessageRequest {

    /**
     * 채팅방 ID
     */
    private Long roomId;

    /**
     * 메시지 타입
     */
    private ChatMessageType type;

    /**
     * 메시지 내용 (TEXT/SYSTEM)
     */
    private String content;

    /**
     * 이미지 URL (IMAGE)
     * 또는 이미지 업로드 시 presigned URL로 변환
     */
    private String imageUrl;

    /**
     * 카드 페이로드 (INVITE_CARD, SETTLEMENT_CARD)
     * JSON 형식의 문자열
     */
    private String cardPayload;

    @Override
    public String toString() {
        return "SendChatMessageRequest{" +
                "roomId=" + roomId +
                ", type=" + type +
                ", content='" + content + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", cardPayload='" + cardPayload + '\'' +
                '}';
    }
}
