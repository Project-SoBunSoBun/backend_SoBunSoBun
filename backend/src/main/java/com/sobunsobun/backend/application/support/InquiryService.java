package com.sobunsobun.backend.application.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.application.file.FileStorageService;
import com.sobunsobun.backend.domain.Inquiry;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.support.InquiryRequest;
import com.sobunsobun.backend.dto.support.InquiryResponse;
import com.sobunsobun.backend.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 1:1 문의 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    // 이메일 검증 정규식
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    /**
     * 문의 제출
     * @param request 문의 요청
     * @param user 로그인한 사용자
     * @return 문의 응답
     */
    public InquiryResponse submitInquiry(InquiryRequest request, User user) {
        log.info(" [submitInquiry] 문의 제출 시작 - userId: {}, typeCode: {}", user.getId(), request.getTypeCode());
        log.info(" [submitInquiry] 요청 필드 검증 - typeCode: '{}', content: '{}', replyEmail: '{}'",
                request.getTypeCode(), request.getContent(), request.getReplyEmail());

        try {
            // 필드 검증
            if (request.getContent() == null || request.getContent().isBlank()) {
                log.warn(" [submitInquiry] content 필드가 null 또는 빈 문자열입니다");
                throw new IllegalArgumentException("문의 내용은 필수입니다.");
            }
            if (request.getTypeCode() == null || request.getTypeCode().isBlank()) {
                log.warn(" [submitInquiry] typeCode 필드가 null 또는 빈 문자열입니다");
                throw new IllegalArgumentException("문의 유형은 필수입니다.");
            }
            if (request.getReplyEmail() == null || request.getReplyEmail().isBlank()) {
                log.warn(" [submitInquiry] replyEmail 필드가 null 또는 빈 문자열입니다");
                throw new IllegalArgumentException("답변 받을 이메일은 필수입니다.");
            }

            // 이메일 형식 검증
            if (!isValidEmail(request.getReplyEmail())) {
                log.warn(" [submitInquiry] 잘못된 이메일 형식: '{}'", request.getReplyEmail());
                throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다: " + request.getReplyEmail());
            }
            log.info(" [submitInquiry] 이메일 형식 검증 통과: '{}'", request.getReplyEmail());

            // 스크린샷 파일 저장
            List<String> imageUrls = new ArrayList<>();
            if (request.getScreenshots() != null && !request.getScreenshots().isEmpty()) {
                log.info(" [submitInquiry] 스크린샷 {} 개 저장 시작", request.getScreenshots().size());
                for (int i = 0; i < request.getScreenshots().size(); i++) {
                    MultipartFile screenshot = request.getScreenshots().get(i);
                    log.info(" [submitInquiry] 처리 중: screenshot[{}] - name='{}', originalFilename='{}', size={}, contentType='{}', empty={}",
                            i, screenshot.getName(), screenshot.getOriginalFilename(),
                            screenshot.getSize(), screenshot.getContentType(), screenshot.isEmpty());

                    if (screenshot != null && !screenshot.isEmpty()) {
                        try {
                            String imageUrl = fileStorageService.saveImage(screenshot);
                            if (imageUrl != null && !imageUrl.isBlank()) {
                                imageUrls.add(imageUrl);
                                log.info(" [submitInquiry] 스크린샷[{}] 저장 성공: {}", i, imageUrl);
                            } else {
                                log.warn(" [submitInquiry] 스크린샷[{}] 저장 실패: imageUrl이 null 또는 빈 문자열", i);
                            }
                        } catch (Exception e) {
                            log.error(" [submitInquiry] 스크린샷[{}] 저장 중 예외 발생: {}", i, e.getMessage(), e);
                        }
                    } else {
                        log.warn(" [submitInquiry] 스크린샷[{}]이 null 또는 비어있음", i);
                    }
                }
            } else {
                log.info(" [submitInquiry] 첨부된 스크린샷 없음 (screenshots: {})",
                        request.getScreenshots() == null ? "null" : "empty list");
            }

            // 이미지 URL을 JSON 배열로 변환
            String imageUrlsJson = null;
            if (!imageUrls.isEmpty()) {
                try {
                    imageUrlsJson = objectMapper.writeValueAsString(imageUrls);
                    log.info(" [submitInquiry] 이미지 URL JSON 변환 성공: {} (총 {} 개)", imageUrlsJson, imageUrls.size());
                } catch (Exception e) {
                    log.error(" [submitInquiry] 이미지 URL JSON 변환 실패: {}", e.getMessage(), e);
                    imageUrlsJson = null;
                }
            } else {
                log.info(" [submitInquiry] 저장된 이미지 URL 없음, imageUrls 필드는 null로 저장됨");
            }

            // 문의 엔티티 생성
            Inquiry inquiry = Inquiry.builder()
                    .user(user)
                    .typeCode(request.getTypeCode())
                    .content(request.getContent())
                    .replyEmail(request.getReplyEmail())
                    .imageUrls(imageUrlsJson)
                    .status("RECEIVED")
                    .build();

            log.info(" [submitInquiry] 문의 엔티티 생성 - typeCode: '{}', content: '{}', replyEmail: '{}'",
                    inquiry.getTypeCode(), inquiry.getContent(), inquiry.getReplyEmail());

            // 저장
            Inquiry savedInquiry = inquiryRepository.save(inquiry);
            log.info(" [submitInquiry] 문의 저장 완료 - inquiryId: {}", savedInquiry.getId());

            return InquiryResponse.builder()
                    .inquiryId(savedInquiry.getId())
                    .status(savedInquiry.getStatus())
                    .message("문의가 접수되었습니다. 입력하신 이메일로 답변을 보내드리겠습니다.")
                    .createdAt(savedInquiry.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error(" [submitInquiry] 문의 제출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("문의 제출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자의 문의 목록 조회
     * @param user 사용자
     * @param pageable 페이징
     * @return 문의 목록
     */
    @Transactional(readOnly = true)
    public Page<Inquiry> getUserInquiries(User user, Pageable pageable) {
        log.info(" [getUserInquiries] 문의 목록 조회 - userId: {}", user.getId());
        return inquiryRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * 문의 상세 조회
     * @param inquiryId 문의 ID
     * @param user 로그인한 사용자
     * @return 문의
     */
    @Transactional(readOnly = true)
    public Inquiry getInquiry(Long inquiryId, User user) {
        log.info(" [getInquiry] 문의 상세 조회 - inquiryId: {}, userId: {}", inquiryId, user.getId());
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        // 문의자만 조회 가능 또는 관리자
        if (!inquiry.getUser().getId().equals(user.getId())) {
            log.warn(" [getInquiry] 권한 없음 - userId: {}, inquiryUser: {}", user.getId(), inquiry.getUser().getId());
            throw new IllegalArgumentException("자신의 문의만 조회할 수 있습니다.");
        }

        return inquiry;
    }

    /**
     * 스크린샷 이미지 URL 목록 조회
     * @param inquiry 문의
     * @return 이미지 URL 목록
     */
    @Transactional(readOnly = true)
    public List<String> getInquiryImageUrls(Inquiry inquiry) {
        if (inquiry.getImageUrls() == null || inquiry.getImageUrls().isBlank()) {
            return new ArrayList<>();
        }

        try {
            List<String> urls = objectMapper.readValue(inquiry.getImageUrls(), List.class);
            log.info(" [getInquiryImageUrls] 이미지 URL 조회 - count: {}", urls.size());
            return urls;
        } catch (Exception e) {
            log.warn(" [getInquiryImageUrls] 이미지 URL 파싱 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 이메일 형식 검증
     * @param email 검증할 이메일 주소
     * @return 유효한 이메일이면 true, 아니면 false
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}
