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
     * 프로필 이미지 URL
     *
     * 1:1 채팅: 상대방의 프로필 이미지
     * 그룹 채팅: GroupPost 이미지 (없을 경우 null)
     */
    @JsonProperty("profileImageUrl")
    private String profileImageUrl;

    /**
     * 채팅방 타입
     *
     * ONE_TO_ONE: 1:1 개인 채팅
     * GROUP: 단체 채팅
     */
    @JsonProperty("roomType")
    private String roomType;

    /**
     * 채팅방 멤버 수 (단체 채팅일 때 유용)
     */
    @JsonProperty("memberCount")
    private Integer memberCount;

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
     * Redis Hash "unread:counts:{userId}" / field "{roomId}" 에서 조회
     * Cache Miss 시 DB(ChatMember.lastReadAt 기준) Write-through 캐싱
     * 0이면 읽은 메시지만 있음
     */
    @JsonProperty("unreadCount")
    private Long unreadCount;

    /**
     * 연결된 공동구매 게시글 ID
     *
     * 1:1 채팅: 게시글에서 시작된 경우 해당 게시글 ID, 그 외 null
     * 단체 채팅: 항상 연결된 게시글 ID
     */
    @JsonProperty("groupPostId")
    private Long groupPostId;
}
