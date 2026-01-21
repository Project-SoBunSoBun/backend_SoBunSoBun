package com.sobunsobun.backend.dto.announcement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공지사항 목록 아이템 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementListItemResponse {

    /**
     * 공지사항 ID
     */
    private Long id;

    /**
     * 제목
     */
    private String title;

    /**
     * 카테고리 (SERVICE, UPDATE, EVENT, ETC)
     */
    private String category;

    /**
     * 상단 고정 여부
     */
    private Boolean isPinned;

    /**
     * 작성 일시
     */
    private LocalDateTime createdAt;
}

