package com.sobunsobun.backend.dto.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이미지 업로드 응답 DTO (Multipart 방식)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadResponse {

    /**
     * 이미지 ID
     */
    private String imageId;

    /**
     * 이미지 URL
     */
    private String url;

    /**
     * 썸네일 URL
     */
    private String thumbnailUrl;

    /**
     * 파일 크기 (bytes)
     */
    private Long size;

    /**
     * Content Type
     */
    private String contentType;
}

