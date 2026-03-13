package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅방 상세 정보 응답 DTO
 *
 * 개인(ONE_TO_ONE) / 단체(GROUP) 채팅방 모두 지원합니다.
 * 개인 채팅방: otherUser 정보 포함, groupPost/members 는 null
 * 단체 채팅방: members 목록, groupPost 정보 포함, otherUser 는 null
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "채팅방 상세 정보 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomDetailResponse {

    // ====== 공통 필드 ======

    @Schema(description = "채팅방 ID", example = "1")
    @JsonProperty("roomId")
    private Long roomId;

    @Schema(description = "채팅방 이름", example = "떠나바 모임")
    @JsonProperty("roomName")
    private String roomName;

    @Schema(description = "채팅방 타입 (ONE_TO_ONE / GROUP)", example = "GROUP")
    @JsonProperty("roomType")
    private String roomType;

    @Schema(description = "방장 사용자 ID", example = "10")
    @JsonProperty("ownerId")
    private Long ownerId;

    @Schema(description = "채팅방 멤버 수", example = "4")
    @JsonProperty("memberCount")
    private Integer memberCount;

    @Schema(description = "안 읽은 메시지 수", example = "3")
    @JsonProperty("unReadCount")
    private Long unreadCount;

    @Schema(description = "마지막 메시지 정보")
    @JsonProperty("lastMessage")
    private LastMessageDto lastMessage;

    @Schema(description = "채팅방 생성 시간 (ISO 8601)")
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;


    // ====== 단체 채팅(GROUP) 전용 필드 ======

    @Schema(description = "연결된 공동구매 게시글 ID (1:1 채팅: 게시글에서 시작된 경우, 단체 채팅: 항상 존재)")
    @JsonProperty("groupPostId")
    private Long groupPostId;

    @Schema(description = "연결된 공동구매 게시글 제목")
    @JsonProperty("groupPostTitle")
    private String groupPostTitle;

    @Schema(description = "채팅방 멤버 목록")
    @JsonProperty("members")
    private List<MemberInfo> members;

    @Schema(description = "정산 ID (groupPost가 없으면 null)", example = "7")
    @JsonProperty("settlementId")
    private Long settlementId;

    @Schema(description = "정산 완료 여부 (groupPost가 없으면 null)", example = "false")
    @JsonProperty("isSettled")
    private Boolean isSettled;

    @Schema(description = "현재 사용자의 리뷰 완료 여부 (groupPost가 없으면 null)", example = "false")
    @JsonProperty("isReviewed")
    private Boolean isReviewed;

    // ====== 멤버 정보 내부 DTO ======

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "채팅방 멤버 정보")
    public static class MemberInfo {

        @Schema(description = "사용자 ID", example = "1")
        @JsonProperty("userId")
        private Long userId;

        @Schema(description = "닉네임", example = "홍길동")
        @JsonProperty("nickname")
        private String nickname;

        @Schema(description = "프로필 이미지 URL")
        @JsonProperty("profileImage")
        private String profileImage;

        @Schema(description = "방장 여부", example = "false")
        @JsonProperty("isOwner")
        private Boolean isOwner;

    }
}
