package com.sobunsobun.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * REST API 메시지 전송 요청 DTO
 *
 * POST /api/messages
 * userId는 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않습니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "메시지 전송 요청")
public class SendMessageRequest {

    @NotNull(message = "채팅방 ID는 필수입니다.")
    @Schema(description = "그룹 채팅방 ID", example = "1")
    private Long groupChatRoomId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Schema(description = "메시지 본문", example = "안녕하세요!")
    private String content;

    @Schema(description = "정산 ID (선택)", example = "42", nullable = true)
    private String settlementId;
}
