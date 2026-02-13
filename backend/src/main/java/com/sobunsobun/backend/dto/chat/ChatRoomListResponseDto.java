package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅방 목록 응답 DTO
 *
 * iOS 클라이언트의 채팅방 목록 화면에서 표시할 정보를 담고 있습니다.
 * 각 채팅방마다 다음 정보를 포함:
 * - 채팅방 ID
 * - 채팅방 이름 (1:1은 상대방 이름, 그룹은 방 이름)
 * - 마지막 메시지 내용
 * - 마지막 메시지 시간
 * - 안 읽은 메시지 개수 (Redis에서 조회)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomListResponseDto {

    /**
     * 채팅방 ID
     */
    @JsonProperty("roomId")
    private Long roomId;

    /**
     * 채팅방 이름
     *
     * 1:1 채팅: 상대방의 닉네임
     * 그룹 채팅: 채팅방의 이름
     */
    @JsonProperty("roomName")
    private String roomName;

    /**
     * 마지막 메시지 내용 (미리보기)
     *
     * null인 경우: 메시지 없음
     */
    @JsonProperty("lastMessage")
    private String lastMessage;

    /**
     * 마지막 메시지 전송 시간
     *
     * ISO 8601 형식: 2026-02-14T12:30:45
     * null인 경우: 메시지 없음
     */
    @JsonProperty("lastMessageTime")
    private LocalDateTime lastMessageTime;

    /**
     * 안 읽은 메시지 개수
     *
     * Redis의 room:{roomId}:user:{userId}:unread에서 조회한 값
     * 0이면 읽은 메시지만 있음
     */
    @JsonProperty("unreadCount")
    private Long unreadCount;
}
