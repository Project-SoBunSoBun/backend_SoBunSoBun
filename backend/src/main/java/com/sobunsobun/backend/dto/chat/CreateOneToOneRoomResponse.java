package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1:1 채팅방 생성/조회 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOneToOneRoomResponse {

    /**
     * 채팅방 ID
     */
    @JsonProperty("roomId")
    private Long roomId;

    /**
     * 상대방의 닉네임
     */
    @JsonProperty("otherUserName")
    private String otherUserName;

    /**
     * 상대방의 프로필 이미지 URL
     */
    @JsonProperty("otherUserProfileImageUrl")
    private String otherUserProfileImageUrl;

    /**
     * 새로 생성되었는지 기존 방인지 여부
     * true: 새로 생성됨
     * false: 기존 방
     */
    @JsonProperty("isNewRoom")
    private Boolean isNewRoom;
}
