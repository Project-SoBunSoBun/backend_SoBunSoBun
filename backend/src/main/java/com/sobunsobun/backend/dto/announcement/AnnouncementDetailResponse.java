package com.sobunsobun.backend.dto.announcement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementDetailResponse {
    private Long id;
    private String title;
    private String content;
    private String category;
    private Boolean isPinned;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

