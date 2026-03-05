package com.sobunsobun.backend.dto.manner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 매너 평가 요청 DTO
 *
 * 한 거래(groupPostId)에 대해 여러 명을 한 번에 평가할 수 있습니다.
 * 각 reviews 항목마다 receiverId와 tagCodes를 지정합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MannerReviewRequest {

    /**
     * 어떤 거래(게시글)에 대한 평가인지
     */
    @NotNull(message = "거래 게시글 ID는 필수입니다.")
    private Long groupPostId;

    /**
     * 평가 대상 목록 (1명 이상)
     */
    @Valid
    @NotEmpty(message = "평가 대상을 최소 1명 이상 입력해야 합니다.")
    private List<ReviewItem> reviews;

    /**
     * 개별 매너 평가 항목
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewItem {

        /**
         * 평가 대상 사용자 ID
         */
        @NotNull(message = "평가 대상 사용자 ID는 필수입니다.")
        private Long receiverId;

        /**
         * 선택한 태그 코드 목록 (1~3개)
         * 예: ["TAG001", "TAG003"]
         */
        @NotEmpty(message = "태그를 최소 1개 선택해야 합니다.")
        @Size(max = 3, message = "태그는 최대 3개까지 선택 가능합니다.")
        private List<String> tagCodes;
    }
}
