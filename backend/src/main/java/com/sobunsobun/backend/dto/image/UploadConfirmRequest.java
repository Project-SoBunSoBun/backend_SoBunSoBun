package com.sobunsobun.backend.dto.image;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 업로드 완료 확인 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadConfirmRequest {

    /**
     * 업로드 ID (필수)
     */
    @NotBlank(message = "업로드 ID는 필수입니다.")
    private String uploadId;
}

