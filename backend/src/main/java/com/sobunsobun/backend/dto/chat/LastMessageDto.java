package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.domain.chat.ChatMessage;
import com.sobunsobun.backend.domain.chat.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LastMessageDto {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @JsonProperty("id")
    private String id;

    @JsonProperty("roomId")
    private Long roomId;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("profileImage")
    private String profileImage;

    @JsonProperty("type")
    private String type;

    @JsonProperty("content")
    private String content;

    @JsonProperty("imageUrl")
    private String imageUrl;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("settlementId")
    private Long settlementId;

    @JsonProperty("inviteId")
    private Long inviteId;

    public static LastMessageDto from(ChatMessage message) {
        if (message == null) return null;

        Long settlementId = null;
        Long inviteId = null;
        if (message.getCardPayload() != null) {
            try {
                if (message.getType() == ChatMessageType.SETTLEMENT_CARD) {
                    SettlementCardPayload payload = MAPPER.readValue(
                            message.getCardPayload(), SettlementCardPayload.class);
                    settlementId = payload.getSettlementId();
                } else if (message.getType() == ChatMessageType.INVITE_CARD) {
                    InviteCardPayload payload = MAPPER.readValue(
                            message.getCardPayload(), InviteCardPayload.class);
                    inviteId = payload.getInviteId();
                }
            } catch (Exception ignored) {
            }
        }

        String createdAtStr = null;
        if (message.getCreatedAt() != null) {
            createdAtStr = message.getCreatedAt()
                    .atZone(KST)
                    .format(FORMATTER);
        }

        // User lazy loading 실패 처리 (삭제된 사용자 등)
        // EntityNotFoundException은 전역 핸들러가 처리
        Long userId = null;
        String nickname = null;
        String profileImage = null;
        if (message.getSender() != null) {
            userId = message.getSender().getId();
            nickname = message.getSender().getNickname();
            profileImage = message.getSender().getProfileImageUrl();
        }

        return LastMessageDto.builder()
                .id(message.getId() != null ? message.getId().toString() : null)
                .roomId(message.getChatRoom() != null ? message.getChatRoom().getId() : null)
                .userId(userId)
                .nickname(nickname)
                .profileImage(profileImage)
                .type(message.getType() != null ? message.getType().name() : null)
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .createdAt(createdAtStr)
                .settlementId(settlementId)
                .inviteId(inviteId)
                .build();
    }
}
