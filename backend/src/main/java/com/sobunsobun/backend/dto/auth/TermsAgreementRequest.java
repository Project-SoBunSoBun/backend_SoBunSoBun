package com.sobunsobun.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class TermsAgreementRequest {
    @NotBlank(message = "로그인 토큰이 필요합니다.")
    private String loginToken;

    private boolean serviceTermsAgreed;
    private boolean privacyPolicyAgreed;
    private boolean marketingOptionalAgreed; // 선택적 마케팅 동의
}
