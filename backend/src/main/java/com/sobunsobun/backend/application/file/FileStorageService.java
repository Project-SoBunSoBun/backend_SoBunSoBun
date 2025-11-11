package com.sobunsobun.backend.application.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.url-prefix:/files}")
    private String urlPrefix;

    // 허용 확장자/콘텐츠 타입
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    // 5MB
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    /**
     * 이미지 파일 저장
     * @param file 업로드된 파일 (null 또는 비어있으면 null 반환)
     * @return 저장된 파일 URL (파일이 없으면 null)
     */
    public String saveImage(MultipartFile file) {
        // 파일이 null이거나 비어있으면 null 반환
        if (file == null || file.isEmpty()) {
            log.info("파일이 비어있어 null 반환");
            return null;
        }

        validateImage(file);
        try {
            Path root = Paths.get(uploadDir);
            Files.createDirectories(root);

            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
            Path target = root.resolve(filename);

            Files.copy(file.getInputStream(), target);

            String accessUrl = (urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length()-1) : urlPrefix)
                    + "/" + filename;
            log.info("Saved profile image: {}", target);
            return accessUrl;
        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패");
        }
    }

    public void deleteIfLocal(String url) {
        if (url == null || url.isBlank()) return;
        // urlPrefix로 시작하는지 확인하여 로컬 파일만 삭제
        String cleanPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length()-1) : urlPrefix;
        if (url.startsWith(cleanPrefix + "/")) {
            String filename = url.substring((cleanPrefix + "/").length());
            Path path = Paths.get(uploadDir).resolve(filename);
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("기존 파일 삭제 실패: {}", path);
            }
        }
    }

    private void validateImage(MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 용량이 5MB를 초과합니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다.(jpg/png/webp)");
        }
    }

    private String getExtension(String original) {
        if (!StringUtils.hasText(original)) return "";
        int idx = original.lastIndexOf('.');
        return (idx > -1 ? original.substring(idx + 1) : "").toLowerCase();
    }
}
