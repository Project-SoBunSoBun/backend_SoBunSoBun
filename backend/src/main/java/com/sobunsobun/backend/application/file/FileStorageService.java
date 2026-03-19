package com.sobunsobun.backend.application.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.annotation.PostConstruct;
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
     * 서비스 초기화 - 업로드 디렉토리 생성 및 검증
     */
    @PostConstruct
    public void init() {
        try {
            Path root = Paths.get(uploadDir);

            log.info(" [FileStorageService Init] 업로드 디렉토리 설정: {}", root.toAbsolutePath());

            if (!Files.exists(root)) {
                log.warn(" [FileStorageService Init] 디렉토리가 없어 생성 중...");
                Files.createDirectories(root);
                log.info(" [FileStorageService Init] 디렉토리 생성 완료: {}", root.toAbsolutePath());
            } else {
                log.info(" [FileStorageService Init] 디렉토리 이미 존재: {}", root.toAbsolutePath());
            }

            // 디렉토리 권한 확인
            boolean isWritable = Files.isWritable(root);
            log.info(" [FileStorageService Init] 디렉토리 쓰기 권한: {}", isWritable ? " YES" : " NO");

            if (!isWritable) {
                log.error(" [FileStorageService Init] 디렉토리에 쓰기 권한이 없습니다!");
            }

        } catch (IOException e) {
            String errorMsg = String.format(
                    " [FileStorageService Init] 디렉토리 생성 실패\n" +
                    "   경로: %s\n" +
                    "   원인: %s\n" +
                    "   해결방법: 디렉토리 권한 확인 또는 application.yml의 file.upload-dir 확인",
                    uploadDir, e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException("업로드 디렉토리를 생성할 수 없습니다: " + uploadDir, e);
        }
    }

    /**
     * 이미지 파일 저장
     * @param file 업로드된 파일 (null 또는 비어있으면 null 반환)
     * @return 저장된 파일 URL (파일이 없으면 null)
     */
    public String saveImage(MultipartFile file) {
        // 파일이 null이거나 비어있으면 null 반환
        if (file == null || file.isEmpty()) {
            log.info(" [saveImage] 파일이 비어있어 null 반환");
            return null;
        }

        log.info(" [saveImage] 파일 저장 시작 - filename: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        validateImage(file);

        try {
            Path root = Paths.get(uploadDir);

            // 디렉토리 존재 확인 및 생성
            if (!Files.exists(root)) {
                log.warn(" [saveImage] 디렉토리 없음, 생성 중: {}", root.toAbsolutePath());
                Files.createDirectories(root);
                log.info(" [saveImage] 디렉토리 생성 완료");
            }

            // 파일명 생성 (UUID + 확장자)
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
            Path target = root.resolve(filename);

            log.info(" [saveImage] 파일 저장 경로: {}", target.toAbsolutePath());

            // 파일 복사
            Files.copy(file.getInputStream(), target);

            log.info(" [saveImage] 파일 저장 성공: {}", target.toAbsolutePath());

            // 접근 URL 생성
            String accessUrl = (urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length()-1) : urlPrefix)
                    + "/" + filename;

            log.info(" [saveImage] 접근 URL: {}", accessUrl);

            return accessUrl;
        } catch (IOException e) {
            log.error(" [saveImage] 파일 저장 실패 - {}: {}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 저장 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error(" [saveImage] 예상치 못한 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 처리 중 오류 발생");
        }
    }

    /**
     * 로컬 파일 삭제
     */
    public void deleteIfLocal(String url) {
        if (url == null || url.isBlank()) return;

        String cleanPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length()-1) : urlPrefix;
        if (url.startsWith(cleanPrefix + "/")) {
            String filename = url.substring((cleanPrefix + "/").length());
            Path path = Paths.get(uploadDir).resolve(filename);
            try {
                if (Files.deleteIfExists(path)) {
                    log.info(" [deleteIfLocal] 파일 삭제 완료: {}", path.toAbsolutePath());
                }
            } catch (IOException e) {
                log.warn(" [deleteIfLocal] 파일 삭제 실패: {}", path.toAbsolutePath(), e);
            }
        }
    }

    /**
     * 파일 존재 여부 확인
     */
    public boolean fileExists(String url) {
        if (url == null || url.isBlank()) return false;

        String cleanPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length()-1) : urlPrefix;
        if (url.startsWith(cleanPrefix + "/")) {
            String filename = url.substring((cleanPrefix + "/").length());
            Path path = Paths.get(uploadDir).resolve(filename);
            boolean exists = Files.exists(path);
            log.debug(" [fileExists] filename: {}, exists: {}", filename, exists);
            return exists;
        }
        return false;
    }

    /**
     * 이미지 파일 검증
     */
    private void validateImage(MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            log.warn(" [validateImage] 파일 크기 초과: {} bytes", file.getSize());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 용량이 5MB를 초과합니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn(" [validateImage] 허용되지 않는 파일 타입: {}", contentType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다.(jpg/png/webp)");
        }

        log.info(" [validateImage] 파일 검증 성공 - contentType: {}", contentType);
    }

    /**
     * 파일 확장자 추출
     */
    private String getExtension(String original) {
        if (!StringUtils.hasText(original)) return "";
        int idx = original.lastIndexOf('.');
        return (idx > -1 ? original.substring(idx + 1) : "").toLowerCase();
    }
}



