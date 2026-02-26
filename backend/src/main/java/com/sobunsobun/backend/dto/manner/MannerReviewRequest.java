package com.sobunsobun.backend.dto.manner;

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
 * 클라이언트는 tagCodes 리스트에 "TAG001", "TAG002" 같은 코드를 전달합니다.
 * 최대 3개 태그까지 한 번에 선택 가능합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MannerReviewRequest {

    /**
     * 평가 대상 사용자 ID
     */
    @NotNull(message = "평가 대상 사용자 ID는 필수입니다.")
    private Long receiverId;

    /**
     * 어떤 거래(게시글)에 대한 평가인지
     */
    @NotNull(message = "거래 게시글 ID는 필수입니다.")
    private Long groupPostId;

    /**
     * 선택한 태그 코드 목록 (1~3개)
     * 예: ["TAG001", "TAG003"]
     */
    @NotEmpty(message = "태그를 최소 1개 선택해야 합니다.")
    @Size(max = 3, message = "태그는 최대 3개까지 선택 가능합니다.")
    private List<String> tagCodes;
}