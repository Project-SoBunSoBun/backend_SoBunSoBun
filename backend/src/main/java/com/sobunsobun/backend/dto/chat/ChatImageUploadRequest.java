package com.sobunsobun.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 채팅 이미지 업로드 요청 DTO
 *
 * FormData 방식으로 이미지 파일과 함께 전송됩니다.
 * Content-Type: multipart/form-data
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "채팅 이미지 업로드 요청")
public class ChatImageUploadRequest {

    /**
     * 이미지 파일 (jpg/png/webp, 최대 5MB)
     */
    @Schema(description = "이미지 파일 (jpg/png/webp, 최대 5MB)", required = true)
    private MultipartFile image;

    /**
     * 채팅방 ID
     */
    @Schema(description = "채팅방 ID", required = true, example = "1")
    private Long chatId;

    /**
     * 메시지 내용 (이미지와 함께 전송할 텍스트, 선택)
     */
    @Schema(description = "메시지 내용 (선택)", example = "사진 보내드려요!")
    private String message;
}

