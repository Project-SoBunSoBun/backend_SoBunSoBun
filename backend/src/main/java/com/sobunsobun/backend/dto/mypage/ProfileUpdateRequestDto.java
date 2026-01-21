package com.sobunsobun.backend.dto.mypage;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * 프로필 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequestDto {

    /**
     * 닉네임 (선택)
     * - 2~20자
     * - 한글, 영문, 숫자만 허용
     * - 중복 불가
     */
    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
    private String nickname;

    /**
     * 프로필 이미지 URL (선택)
     * - 최대 500자
     * - URL 형식
     */
    @Size(max = 500, message = "프로필 이미지 URL은 최대 500자입니다.")
    @URL(message = "올바른 URL 형식이 아닙니다.")
    private String profileImageUrl;
}

