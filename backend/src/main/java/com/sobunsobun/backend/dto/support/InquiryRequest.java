package com.sobunsobun.backend.dto.support;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1:1 문의 제출 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryRequest {

    /**
     * 문의 유형 코드 (필수)
     */
    @NotBlank(message = "문의 유형은 필수 선택입니다.")
    private String typeCode;

    /**
     * 문의 내용 (필수, 1~1000자)
     */
    @NotBlank(message = "문의 내용은 필수입니다.")
    @Size(min = 1, max = 1000, message = "문의 내용은 1~1000자여야 합니다.")
    private String content;

    /**
     * 답변 받을 이메일 (필수)
     */
    @NotBlank(message = "답변 받을 이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String replyEmail;
}

