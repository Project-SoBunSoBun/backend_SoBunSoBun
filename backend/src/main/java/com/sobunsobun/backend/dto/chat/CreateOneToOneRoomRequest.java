package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1:1 채팅방 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOneToOneRoomRequest {

    /**
     * 대화할 상대방의 사용자 ID
     */
    @JsonProperty("targetUserId")
    private Long targetUserId;
}
