package com.sobunsobun.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 개인 채팅방 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "개인 채팅방 생성 요청")
public class CreatePrivateChatRoomRequest {

    @Schema(description = "상대방 사용자 ID", example = "2", required = true)
    private Long otherUserId;

    @Schema(description = "연결할 공동구매 게시글 ID (게시글에서 채팅 시작 시)", example = "5")
    private Long groupPostId;
}
