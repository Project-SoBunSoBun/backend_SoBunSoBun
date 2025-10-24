package com.sobunsobun.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 프로필 업데이트 요청 DTO
 *
 * 닉네임과 프로필 이미지를 함께 업데이트할 때 사용
 * MultipartFile은 별도 파라미터로 받음
 */
@Schema(description = "프로필 업데이트 요청")
public record ProfileUpdateRequest(

        @Schema(description = "닉네임 (1-8자, 한글/영문/숫자)", example = "정무나가")
        @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
        @Size(max = 8, message = "닉네임은 최대 8자입니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글/영문/숫자만 가능합니다.")
        String nickname
) {
}

