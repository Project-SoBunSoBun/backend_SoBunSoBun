package com.sobunsobun.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 페이지 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "페이지 응답")
public class PageResponse<T> {

    @Schema(description = "데이터 목록")
    private List<T> content;

    @Schema(description = "전체 요소 개수", example = "100")
    private long totalElements;

    @Schema(description = "전체 페이지 개수", example = "5")
    private int totalPages;

    @Schema(description = "현재 페이지 (0-indexed)", example = "0")
    private int currentPage;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "첫 페이지 여부")
    private boolean first;

    @Schema(description = "마지막 페이지 여부")
    private boolean last;

    public boolean getFirst() {
        return currentPage == 0;
    }

    public boolean getLast() {
        return currentPage >= totalPages - 1;
    }
}
