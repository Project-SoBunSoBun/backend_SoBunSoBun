package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 이미지 메시지 응답 DTO
 *
 * 이미지 업로드 후 반환되는 응답입니다.
 * senderName -> nickname, profileImageUrl -> profileImage, timestamp -> createdAt (ISO 8601)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "채팅 이미지 메시지 응답")
public class ChatImageMessageResponse {

    /**
     * 메시지 ID
     */
    @Schema(description = "메시지 ID", example = "123")
    @JsonProperty("id")
    private Long id;

    /**
     * 채팅방 ID
     */
    @Schema(description = "채팅방 ID", example = "1")
    @JsonProperty("roomId")
    private Long roomId;

    /**
     * 발신자 고유 ID
     */
    @Schema(description = "발신자 고유 ID", example = "456")
    @JsonProperty("userId")
    private Long userId;

    /**
     * 발신자 닉네임 (기존 senderName에서 변경)
     */
    @Schema(description = "발신자 닉네임", example = "홍길동")
    @JsonProperty("nickname")
    private String nickname;

    /**
     * 발신자 프로필 이미지 URL (기존 senderProfileImageUrl에서 변경)
     */
    @Schema(description = "발신자 프로필 이미지 URL", example = "/files/profile123.jpg")
    @JsonProperty("profileImage")
    private String profileImage;

    /**
     * 메시지 타입
     */
    @Schema(description = "메시지 타입", example = "IMAGE")
    @JsonProperty("type")
    private String type;

    /**
     * 메시지 내용 (이미지와 함께 전송된 텍스트)
     */
    @Schema(description = "메시지 내용", example = "사진 보내드려요!")
    @JsonProperty("content")
    private String content;

    /**
     * 이미지 URL
     */
    @Schema(description = "이미지 URL", example = "/files/chat-image-abc123.jpg")
    @JsonProperty("imageUrl")
    private String imageUrl;

    /**
     * 읽은 사람 수
     */
    @Schema(description = "읽은 사람 수", example = "0")
    @JsonProperty("readCount")
    private Integer readCount;

    /**
     * 메시지 생성 시간 (ISO 8601 형식, 기존 timestamp에서 변경)
     */
    @Schema(description = "메시지 생성 시간 (ISO 8601)", example = "2026-02-22T16:24:01")
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 내가 읽었는지 여부
     */
    @Schema(description = "내가 읽었는지 여부", example = "true")
    @JsonProperty("readByMe")
    private Boolean readByMe;
}

