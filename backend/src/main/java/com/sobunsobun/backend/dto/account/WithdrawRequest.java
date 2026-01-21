package com.sobunsobun.backend.dto.account;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 탈퇴 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequest {

    /**
     * 탈퇴 사유 코드 (필수)
     * - RARELY_USED, NO_NEARBY_POSTS, INCONVENIENT, PRIVACY_CONCERN, BAD_EXPERIENCE, OTHER
     */
    @NotBlank(message = "탈퇴 사유는 필수 선택입니다.")
    private String reasonCode;

    /**
     * 탈퇴 사유 상세 (선택, 최대 100자)
     */
    @Size(max = 100, message = "상세 사유는 최대 100자입니다.")
    private String reasonDetail;

    /**
     * 탈퇴 동의 체크 (필수, 반드시 true)
     */
    @NotNull(message = "탈퇴 동의는 필수입니다.")
    @AssertTrue(message = "탈퇴에 동의해야 합니다.")
    private Boolean agreedToTerms;
}

