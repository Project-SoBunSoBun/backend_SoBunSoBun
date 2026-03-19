package com.sobunsobun.backend.dto.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.domain.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 아이템 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationItemResponse {

    /**
     * 알림 ID
     */
    private Long id;

    /**
     * 알림 유형 (COMMENT, PARTICIPATION, SETTLE_UP, ANNOUNCE 등)
     */
    private String type;

    /**
     * 액션을 수행한 사용자 닉네임 (없는 경우 null)
     */
    private String nickname;

    /**
     * 관련 게시글 ID (있는 경우)
     */
    private Long postId;

    /**
     * 읽음 여부
     */
    private Boolean isRead;

    /**
     * 생성 일시
     */
    private LocalDateTime createdAt;

    public static NotificationItemResponse from(Notification notification) {
        String payload = notification.getDataPayload();
        return NotificationItemResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .nickname(extractField(payload, "nickname"))
                .postId(extractPostId(payload))
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private static Long extractPostId(String dataPayload) {
        if (dataPayload == null || dataPayload.isBlank()) return null;
        try {
            JsonNode node = new ObjectMapper().readTree(dataPayload);
            if (node.has("postId") && !node.get("postId").isNull()) {
                return node.get("postId").asLong();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String extractField(String dataPayload, String fieldName) {
        if (dataPayload == null || dataPayload.isBlank()) return null;
        try {
            JsonNode node = new ObjectMapper().readTree(dataPayload);
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return node.get(fieldName).asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

