package com.sobunsobun.backend.dto.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SavedPostDto {

    /**
     * 저장된 게시글 응답 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long postId;
        private String postTitle;
        private Long postOwnerId;
        private String postOwnerName;
        private String postCategory;
        private LocalDateTime postCreatedAt;
        private LocalDateTime savedAt;
    }

    /**
     * 저장된 게시글 목록 응답 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResponse {
        private Long id;
        private Long postId;
        private String postTitle;
        private Long postOwnerId;
        private String postOwnerName;
        private String postCategory;
        private Integer joinedMembers;
        private Integer maxMembers;
        private LocalDateTime postCreatedAt;
        private LocalDateTime savedAt;
    }

    /**
     * 저장 통계 응답 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatisticsResponse {
        private Long userId;
        private long totalSavedPosts;
        private long activePosts;
        private long closedPosts;
    }
}
