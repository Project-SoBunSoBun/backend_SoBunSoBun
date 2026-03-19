package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateChatRoomResponse {

    @Schema(description = "생성된 채팅방 ID", example = "1")
    private Long roomId;

    @Schema(description = "채팅방 이름", example = "민준")
    private String roomName;

    @Schema(description = "채팅방 타입", example = "GROUP")
    private String roomType;

    @Schema(description = "연결된 공동구매 게시글 ID (단체 채팅방인 경우)", example = "5")
    private Long groupPostId;

    @Schema(description = "채팅방 멤버 수", example = "3")
    private Integer memberCount;

    @Schema(description = "새로 생성된 방인지 여부", example = "true")
    private Boolean isNewRoom;

    @Schema(description = "응답 메시지", example = " 채팅방 생성/조회 성공")
    private String message;
}
