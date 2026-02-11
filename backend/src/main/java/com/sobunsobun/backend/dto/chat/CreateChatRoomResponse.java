package com.sobunsobun.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅방 생성 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "채팅방 생성 응답")
public class CreateChatRoomResponse {

    @Schema(description = "생성된 채팅방 ID", example = "1")
    private Long roomId;

    @Schema(description = "채팅방 이름", example = "민준")
    private String roomName;

    @Schema(description = "채팅방 타입", example = "PRIVATE")
    private String roomType;

    @Schema(description = "응답 메시지", example = "✅ 개인 채팅방 생성/조회 성공")
    private String message;
}
