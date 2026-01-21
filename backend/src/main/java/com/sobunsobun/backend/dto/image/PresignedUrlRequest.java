package com.sobunsobun.backend.dto.image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 발급 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignedUrlRequest {

    /**
     * 파일명 (필수)
     */
    @NotBlank(message = "파일명은 필수입니다.")
    @Size(max = 255, message = "파일명은 최대 255자입니다.")
    private String filename;

    /**
     * Content Type (필수, image/jpeg 또는 image/png)
     */
    @NotBlank(message = "Content Type은 필수입니다.")
    @Pattern(regexp = "image/(jpeg|jpg|png)", message = "지원하지 않는 이미지 형식입니다.")
    private String contentType;

    /**
     * 업로드 목적 (필수, PROFILE, POST, INQUIRY)
     */
    @NotBlank(message = "업로드 목적은 필수입니다.")
    @Pattern(regexp = "PROFILE|POST|INQUIRY", message = "올바른 업로드 목적이 아닙니다.")
    private String purpose;

    /**
     * 파일 크기 (bytes, 필수, 최대 10MB)
     */
    @NotNull(message = "파일 크기는 필수입니다.")
    @Positive(message = "파일 크기는 0보다 커야 합니다.")
    private Long fileSize;
}

