package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageResponse {
    private UUID id;
    private Long roomId;

    /**
     * 발신자 고유 ID (기존 senderId 유지 + userId 추가)
     */
    private Long senderId;

    /**
     * 발신자 고유 ID (신규 필드, senderId와 동일)
     */
    @JsonProperty("userId")
    private Long userId;

    /**
     * 발신자 이름 (기존 호환성 유지)
     * @deprecated nickname 필드 사용 권장
     */
    private String senderName;

    /**
     * 발신자 닉네임 (신규 필드, senderName과 동일)
     */
    @JsonProperty("nickname")
    private String nickname;

    /**
     * 발신자 프로필 이미지 URL (기존 호환성 유지)
     * @deprecated profileImage 필드 사용 권장
     */
    private String senderProfileImageUrl;

    /**
     * 발신자 프로필 이미지 URL (신규 필드, senderProfileImageUrl과 동일)
     */
    @JsonProperty("profileImage")
    private String profileImage;

    private String type;
    private String content;
    private String imageUrl;
    private String cardPayload;
    private Integer readCount;

    /**
     * 메시지 생성 시간 (ISO 8601 형식)
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private Boolean readByMe;

    /**
     * 정산 ID
     */
    private Integer settlementId;

    /**
     * 단체 채팅방 ID
     */
    private Integer groupChatRoomId;
}
