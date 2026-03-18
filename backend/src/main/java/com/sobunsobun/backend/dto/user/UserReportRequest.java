package com.sobunsobun.backend.dto.user;

import com.sobunsobun.backend.domain.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저 신고 요청 DTO
 */
@Getter
@NoArgsConstructor
public class UserReportRequest {

    @NotNull(message = "신고 대상 게시글 ID는 필수입니다.")
    private Long groupPostId;

    @NotNull(message = "신고 사유는 필수입니다.")
    private ReportReason reason;

    @Size(max = 1000, message = "신고 내용은 1000자 이내로 작성해주세요.")
    private String description;
}
