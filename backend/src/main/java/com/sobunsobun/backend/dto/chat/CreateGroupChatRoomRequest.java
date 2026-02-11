package com.sobunsobun.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 단체 채팅방 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "단체 채팅방 생성 요청")
public class CreateGroupChatRoomRequest {

    @Schema(description = "채팅방 이름", example = "떠나바 모임", required = true)
    private String roomName;

    @Schema(description = "연결된 모임 ID", example = "5", required = true)
    private Long groupPostId;
}
