package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.application.file.FileStorageService;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.UserStatus;
import com.sobunsobun.backend.domain.WithdrawalReason;
import com.sobunsobun.backend.dto.account.WithdrawRequest;
import com.sobunsobun.backend.dto.account.WithdrawResponse;
import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.infrastructure.oauth.AppleOAuthClient;
import com.sobunsobun.backend.repository.AuthProviderRepository;
import com.sobunsobun.backend.repository.WithdrawalReasonRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.CommentRepository;
import com.sobunsobun.backend.repository.SavedPostRepository;
import com.sobunsobun.backend.repository.UserDeviceRepository;
import com.sobunsobun.backend.repository.PostReportRepository;
import com.sobunsobun.backend.repository.CommentReportRepository;
import com.sobunsobun.backend.repository.BugReportRepository;
import com.sobunsobun.backend.repository.InquiryRepository;
import com.sobunsobun.backend.repository.NotificationRepository;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatInviteRepository;
import com.sobunsobun.backend.support.util.NicknameNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 비즈니스 로직 서비스
 *
 * 담당 기능:
 * - 닉네임 중복 확인 및 정규화
 * - 사용자 프로필 관리
 * - 닉네임 유효성 검증
 * - 회원 탈퇴 및 관련 데이터 초기화
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final AppleOAuthClient appleOAuthClient;
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    private final GroupPostRepository groupPostRepository;
    private final CommentRepository commentRepository;
    private final SavedPostRepository savedPostRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PostReportRepository postReportRepository;
    private final CommentReportRepository commentReportRepository;
    private final BugReportRepository bugReportRepository;
    private final InquiryRepository inquiryRepository;
    private final NotificationRepository notificationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatInviteRepository chatInviteRepository;
    private final NicknameNormalizer nicknameNormalizer;
    private final FileStorageService fileStorageService;

    /**
     * 닉네임 사용 가능 여부 확인
     *
     * 1. 닉네임 정규화 (공백 제거, 대소문자 통일 등)
     * 2. 유효성 검증 (길이, 문자 규칙)
     * 3. 데이터베이스 중복 확인
     *
     * @param rawNickname 검증할 원본 닉네임
     * @return 사용 가능하면 true, 불가능하면 false
     * @throws ResponseStatusException 유효하지 않은 닉네임인 경우
     */
    public boolean isNicknameAvailable(String rawNickname) {
        log.info("[사용자 작동] 닉네임 중복 확인 - 닉네임: {}", rawNickname);

        // 1. 닉네임 정규화
        String normalizedNickname = nicknameNormalizer.normalize(rawNickname);
        log.debug("닉네임 정규화 완료: {} -> {}", rawNickname, normalizedNickname);

        // 2. 유효성 검증
        validateNicknameFormat(normalizedNickname);

        // 3. 중복 확인
        boolean isAvailable = !userRepository.existsByNickname(normalizedNickname);
        log.debug("닉네임 중복 확인 완료: {} -> 사용가능: {}", normalizedNickname, isAvailable);

        return isAvailable;
    }

    /**
     * 닉네임 정규화 (외부 호출용)
     *
     * @param rawNickname 원본 닉네임
     * @return 정규화된 닉네임
     */
    public String normalizeNickname(String rawNickname) {
        return nicknameNormalizer.normalize(rawNickname);
    }

    /**
     * 사용자 닉네임 업데이트
     *
     * 1. 닉네임 정규화 및 검증
     * 2. 다른 사용자와 중복 확인
     * 3. 사용자 존재 확인
     * 4. 닉네임 업데이트 (동시성 처리 포함)
     *
     * @param userId 사용자 ID
     * @param rawNickname 새로운 닉네임
     * @throws ResponseStatusException 사용자 없음, 중복 닉네임 등
     */
    @Transactional
    public void updateUserNickname(Long userId, String rawNickname) {
        log.info("사용자 닉네임 업데이트 시작 - 사용자 ID: {}, 새 닉네임: {}", userId, rawNickname);

        // 1. 닉네임 정규화 및 검증
        String normalizedNickname = nicknameNormalizer.normalize(rawNickname);
        validateNicknameFormat(normalizedNickname);

        // 2. 다른 사용자와 중복 확인 (자신 제외)
        if (userRepository.existsByNicknameAndIdNot(normalizedNickname, userId)) {
            log.warn("닉네임 중복 발생 - 사용자 ID: {}, 닉네임: {}", userId, normalizedNickname);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        // 3. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 없음 - 사용자 ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });

        // 4. 닉네임 업데이트 (동시성 고려한 DB 레벨 중복 체크)
        String oldNickname = user.getNickname();
        user.setNickname(normalizedNickname);

        try {
            userRepository.saveAndFlush(user); // 즉시 DB 반영하여 유니크 제약 조건 위반 감지
            log.info("[사용자 작동] 닉네임 업데이트 완료 - 사용자 ID: {}, {} -> {}", userId, oldNickname, normalizedNickname);
        } catch (DataIntegrityViolationException e) {
            log.error("닉네임 중복 DB 오류 {}: 사용자 ID: {}, 닉네임: {}", e.getClass().getSimpleName(), userId, normalizedNickname);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }
    }


    /**
     * 닉네임 형식 유효성 검증
     *
     * 검증 규칙:
     * - null/공백 불허
     * - 1~8자 제한
     * - 한글, 영문, 숫자만 허용
     *
     * @param nickname 검증할 닉네임 (정규화된)
     * @throws ResponseStatusException 유효하지 않은 경우
     */
    private void validateNicknameFormat(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임은 비어 있을 수 없습니다.");
        }

        if (nickname.length() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임은 최대 8자입니다.");
        }

        // 한글, 영문, 숫자만 허용 (특수문자, 이모지 등 불허)
        if (!nickname.matches("^[가-힣a-zA-Z0-9]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임은 한글/영문/숫자만 가능합니다.");
        }
    }

    /**
     * 사용자 프로필 업데이트 (닉네임 + 프로필 이미지)
     *
     * 회원가입 완료 시 또는 프로필 수정 시 호출됩니다.
     *
     * 처리 순서:
     * 1. 닉네임 정규화 및 검증
     * 2. 사용자 조회
     * 3. 프로필 이미지 업로드 (선택적)
     * 4. 기존 이미지 삭제 (새 이미지 업로드 시)
     * 5. 닉네임 및 이미지 URL 업데이트
     *
     * @param userId 사용자 ID
     * @param rawNickname 새로운 닉네임
     * @param profileImage 프로필 이미지 파일 (선택적, null 가능)
     * @throws ResponseStatusException 사용자 없음, 닉네임 중복, 이미지 업로드 실패 등
     */
    @Transactional
    public void updateUserProfile(Long userId, String rawNickname, MultipartFile profileImage) {
        log.info("[사용자 작동] 프로필 업데이트 시도 - 사용자 ID: {}, 닉네임: {}, 이미지 있음: {}",
                userId, rawNickname, profileImage != null && !profileImage.isEmpty());

        // 1. 닉네임 정규화 및 검증
        String normalizedNickname = nicknameNormalizer.normalize(rawNickname);
        validateNicknameFormat(normalizedNickname);

        // 2. 다른 사용자와 닉네임 중복 확인 (자신 제외)
        if (userRepository.existsByNicknameAndIdNot(normalizedNickname, userId)) {
            log.warn("닉네임 중복 발생 - 사용자 ID: {}, 닉네임: {}", userId, normalizedNickname);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 없음 - 사용자 ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });

        // 4. 프로필 이미지 업로드 (선택적)
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String oldImageUrl = user.getProfileImageUrl();
                String newImageUrl = fileStorageService.saveImage(profileImage);

                user.setProfileImageUrl(newImageUrl);
                log.info("프로필 이미지 업로드 완료 - 사용자 ID: {}, URL: {}", userId, newImageUrl);

                // 기존 이미지 삭제 (로컬 파일인 경우)
                if (oldImageUrl != null && !oldImageUrl.isBlank()) {
                    fileStorageService.deleteIfLocal(oldImageUrl);
                    log.info("기존 프로필 이미지 삭제 - URL: {}", oldImageUrl);
                }
            } catch (ResponseStatusException e) {
                // FileStorageService에서 발생한 예외 그대로 전달
                log.error("프로필 이미지 업로드 실패 {}: 사용자 ID: {}", e.getClass().getSimpleName(), userId);
                throw e;
            }
        }

        // 5. 닉네임 업데이트
        String oldNickname = user.getNickname();
        user.setNickname(normalizedNickname);

        try {
            userRepository.saveAndFlush(user);
            log.info("[사용자 작동] 프로필 업데이트 완료 - 사용자 ID: {}, 닉네임: {} -> {}, 이미지: {}",
                    userId, oldNickname, normalizedNickname, user.getProfileImageUrl());
        } catch (DataIntegrityViolationException e) {
            log.error("프로필 업데이트 DB 오류 {}: 사용자 ID: {}", e.getClass().getSimpleName(), userId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }
    }

    /**
     * 사용자 프로필 이미지만 업데이트
     *
     * 닉네임 변경 없이 이미지만 변경할 때 사용합니다.
     *
     * @param userId 사용자 ID
     * @param profileImage 프로필 이미지 파일
     * @throws ResponseStatusException 사용자 없음, 이미지 업로드 실패 등
     */
    @Transactional
    public void updateProfileImage(Long userId, MultipartFile profileImage) {
        log.info("[사용자 작동] 프로필 이미지 업데이트 시도 - 사용자 ID: {}", userId);

        // 파일 상태 로그
        if (profileImage == null) {
            log.info("프로필 이미지 파일: null (프로필 이미지 삭제)");
        } else if (profileImage.isEmpty()) {
            log.info("프로필 이미지 파일: empty (프로필 이미지 삭제)");
        } else {
            log.info("프로필 이미지 파일: {} ({}bytes)",
                    profileImage.getOriginalFilename(), profileImage.getSize());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 없음 - 사용자 ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });

        String oldImageUrl = user.getProfileImageUrl();
        log.info("기존 프로필 이미지 URL: {}", oldImageUrl);

        // 파일이 null이거나 비어있으면 null 저장, 아니면 새 이미지 저장
        String newImageUrl = fileStorageService.saveImage(profileImage);
        log.info("새 프로필 이미지 URL: {}", newImageUrl);

        user.setProfileImageUrl(newImageUrl);
        userRepository.saveAndFlush(user);

        log.info("DB 업데이트 완료 - 프로필 이미지 URL이 {}로 변경됨", newImageUrl);

        // 기존 이미지가 있고 새 이미지가 다르면 삭제
        if (oldImageUrl != null && !oldImageUrl.isBlank() && !oldImageUrl.equals(newImageUrl)) {
            log.info("기존 이미지 삭제 시도: {}", oldImageUrl);
            fileStorageService.deleteIfLocal(oldImageUrl);
        }

        log.info("[사용자 작동] 프로필 이미지 업데이트 완료 - 사용자 ID: {}, 최종 URL: {}", userId, newImageUrl);
    }

    /**
     * 위치 인증 정보 조회
     *
     * 위치 인증 여부, 만료 여부, 남은 시간 등을 계산하여 반환합니다.
     * 위치 인증은 24시간 후 만료됩니다.
     *
     * @param userId 사용자 ID
     * @return 위치 인증 정보
     * @throws ResponseStatusException 사용자 없음
     */
    public com.sobunsobun.backend.dto.user.LocationVerificationResponse getLocationVerification(Long userId) {
        log.info("[사용자 작동] 위치 인증 정보 조회 - 사용자 ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 없음 - 사용자 ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });

        java.time.LocalDateTime locationVerifiedAt = user.getLocationVerifiedAt();
        boolean isVerified = locationVerifiedAt != null;
        boolean isExpired = false;
        Long remainingMinutes = null;

        // 위치 인증 만료 여부 계산 (24시간 기준)
        if (isVerified) {
            java.time.LocalDateTime expirationTime = locationVerifiedAt.plusHours(24);
            isExpired = java.time.LocalDateTime.now().isAfter(expirationTime);

            if (!isExpired) {
                remainingMinutes = java.time.Duration.between(
                        java.time.LocalDateTime.now(),
                        expirationTime
                ).toMinutes();
            }
        }

        log.info("위치 인증 정보 조회 완료 - 사용자 ID: {}, 인증됨: {}, 만료됨: {}, 남은 시간: {}분",
                userId, isVerified, isExpired, remainingMinutes);

        return com.sobunsobun.backend.dto.user.LocationVerificationResponse.builder()
                .address(user.getAddress())
                .locationVerifiedAt(locationVerifiedAt)
                .isVerified(isVerified)
                .isExpired(isExpired)
                .remainingMinutes(remainingMinutes)
                .build();
    }

    /**
     * 위치 인증 업데이트
     *
     * 사용자의 주소와 위치 인증 시간을 업데이트합니다.
     * 위치 인증은 24시간 동안 유효합니다.
     *
     * @param userId 사용자 ID
     * @param address 주소
     * @throws ResponseStatusException 사용자 없음
     */
    @Transactional
    public void updateLocationVerification(Long userId, String address) {
        log.info("[사용자 작동] 위치 인증 업데이트 - 사용자 ID: {}, 주소: {}", userId, address);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 없음 - 사용자 ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });

        user.setAddress(address);
        user.setLocationVerifiedAt(java.time.LocalDateTime.now());
        userRepository.saveAndFlush(user);

        log.info("[사용자 작동] 위치 인증 업데이트 완료 - 사용자 ID: {}, 인증 시간: {}", userId, user.getLocationVerifiedAt());
    }

    /**
     * 회원 탈퇴 처리
     *
     * 1. 사용자 상태를 DELETED로 변경
     * 2. withdrawn_at에 탈퇴 일시 저장
     * 3. 탈퇴 사유 기록
     *
     * @param userId 탈퇴할 사용자 ID
     * @param request 탈퇴 사유 정보
     * @return 탈퇴 응답 정보
     * @throws ResponseStatusException 사용자 없음 또는 이미 탈퇴한 사용자
     */
    @Transactional
    public WithdrawResponse withdrawUser(Long userId, WithdrawRequest request) {
        log.info("[사용자 작동] 회원 탈퇴 시작 - 사용자 ID: {}, 사유: {}", userId, request.getReasonCode());

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 없음 - 사용자 ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });

        // 2. 이미 탈퇴한 사용자 확인
        if (user.getStatus() == UserStatus.DELETED) {
            log.warn("이미 탈퇴한 사용자 - 사용자 ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 탈퇴한 사용자입니다.");
        }

        // 3. 사용자 관련 데이터 모두 삭제 (FK 의존성 역순으로 삭제)
        log.info("🗑️ 사용자 관련 데이터 삭제 시작 - 사용자 ID: {}", userId);

        try {
            // 3-1. 알림 삭제
            log.debug("알림 삭제 중...");
            notificationRepository.deleteByUserId(userId);

            // 3-2. 채팅 초대 삭제 (받은 초대 + 보낸 초대)
            log.debug("채팅 초대 삭제 중...");
            chatInviteRepository.deleteByInviteeId(userId);
            chatInviteRepository.deleteByInviterId(userId);

            // 3-3. 채팅 메시지 삭제 (보낸 모든 메시지)
            log.debug("채팅 메시지 삭제 중...");
            chatMessageRepository.deleteBySenderId(userId);

            // 3-4. 채팅방 멤버 정보 삭제
            log.debug("채팅방 멤버 정보 삭제 중...");
            chatMemberRepository.deleteByUserId(userId);

            // 3-5. 버그 신고 삭제
            log.debug("버그 신고 삭제 중...");
            bugReportRepository.deleteByUser(user);

            // 3-6. 1:1 문의 삭제
            log.debug("1:1 문의 삭제 중...");
            inquiryRepository.deleteByUser(user);

            // 3-7. 댓글 신고 삭제 (사용자가 한 신고)
            log.debug("댓글 신고 삭제 중...");
            commentReportRepository.deleteByUserId(userId);

            // 3-8. 게시글 신고 삭제 (사용자가 한 신고)
            log.debug("게시글 신고 삭제 중...");
            postReportRepository.deleteByUserId(userId);

            // 3-9. 사용자가 작성한 게시글의 연관 데이터 삭제 (FK 순서 중요)
            // 게시글을 삭제하기 전에 게시글을 참조하는 데이터를 먼저 삭제해야 함
            List<GroupPost> userPosts = groupPostRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
            for (GroupPost post : userPosts) {
                Long postId = post.getId();
                // 게시글에 달린 신고 삭제
                postReportRepository.deleteByPostId(postId);
                // 게시글에 달린 정산은 settlement 테이블 FK ON DELETE CASCADE로 자동 삭제
                // 게시글을 저장한 내역 삭제
                savedPostRepository.deleteByPostId(postId);
                // 게시글에 달린 댓글의 신고 삭제 후 댓글 삭제
                commentRepository.deleteByPostId(postId);
            }

            // 3-10. 저장한 게시글 삭제 (다른 사람 게시글 저장)
            log.debug("저장한 게시글 삭제 중...");
            savedPostRepository.deleteByUserId(userId);

            // 3-12. 댓글 삭제 (다른 사람 게시글에 단 댓글)
            log.debug("댓글 삭제 중...");
            commentRepository.deleteByUserId(userId);

            // 3-13. 게시글 삭제 (작성한 모든 게시글)
            log.debug("게시글 삭제 중...");
            groupPostRepository.deleteByOwnerId(userId);

            // 3-14. 사용자 디바이스 정보 삭제 (FCM 토큰 등)
            log.debug("디바이스 정보 삭제 중...");
            userDeviceRepository.deleteByUserId(userId);

            log.info("✅ 사용자 관련 데이터 삭제 완료 - 사용자 ID: {}", userId);
        } catch (Exception e) {
            log.error("❌ 사용자 관련 데이터 삭제 중 오류 발생 - 사용자 ID: {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 삭제 중 오류가 발생했습니다.");
        }

        // 4. Apple 계정 연결 해제 (Apple 정책 요구사항)
        authProviderRepository.findByUserIdAndProvider(userId, "APPLE").ifPresent(appleProvider -> {
            String refreshToken = appleProvider.getRefreshToken();
            if (refreshToken != null && !refreshToken.isBlank()) {
                try {
                    appleOAuthClient.revokeToken(refreshToken);
                    appleProvider.setRefreshToken(null);
                    authProviderRepository.save(appleProvider);
                    log.info("✅ Apple Revoke 완료 - 사용자 ID: {}", userId);
                } catch (Exception e) {
                    // Revoke 실패는 탈퇴 자체를 막지 않음
                    log.warn("⚠️ Apple Revoke 실패 (탈퇴 계속 진행) - 사용자 ID: {}, 오류: {}", userId, e.getMessage());
                }
            }
        });

        // 6. 프로필 이미지 파일 삭제
        String profileImageUrl = user.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            try {
                fileStorageService.deleteIfLocal(profileImageUrl);
                log.info("프로필 이미지 삭제 완료 - URL: {}", profileImageUrl);
            } catch (Exception e) {
                log.warn("프로필 이미지 삭제 실패 (무시) - URL: {}, 오류: {}", profileImageUrl, e.getMessage());
            }
        }

        // 7. 탈퇴 사유 저장 (개인정보 익명화 전에 먼저 저장해야 원본 정보가 보존됨)
        WithdrawalReason withdrawalReason = WithdrawalReason.builder()
                .user(user)
                .reasonCode(request.getReasonCode())
                .reasonDetail(request.getReasonDetail())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .mannerScore(user.getMannerScore())
                .address(user.getAddress())
                .build();

        withdrawalReasonRepository.saveAndFlush(withdrawalReason);
        log.info("✅ 탈퇴 사유 저장 완료 - 사용자 ID: {}", userId);

        // 8. 사용자 상태 변경 및 탈퇴 일시 저장 + 개인정보 익명화
        LocalDateTime withdrawnAt = LocalDateTime.now();
        LocalDateTime reactivatableAt = withdrawnAt.plusDays(90); // 90일 후 재가입 가능

        user.setStatus(UserStatus.DELETED);
        user.setWithdrawnAt(withdrawnAt);
        user.setReactivatableAt(reactivatableAt);

        // 개인정보 익명화 (email/nickname unique 제약 충돌 방지)
        user.setEmail("withdrawn_" + userId + "@deleted.local");
        user.setNickname("회원탈퇴_" + userId);
        user.setProfileImageUrl(null);
        user.setAddress(null);
        user.setLocationVerifiedAt(null);

        userRepository.saveAndFlush(user);

        log.info("✅ 사용자 상태 변경 및 개인정보 익명화 완료 - 사용자 ID: {}, 탈퇴 일시: {}, 재가입 가능 일시: {}",
                userId, withdrawnAt, reactivatableAt);


        // 9. 응답 반환
        return WithdrawResponse.builder()
                .message("회원탈퇴가 완료되었습니다. 관련 데이터가 모두 삭제되었습니다.")
                .withdrawnAt(withdrawnAt)
                .reactivatableAt(reactivatableAt)
                .dataRetentionDays(90)  // 90일 후 재가입 가능
                .build();
    }

    /**
     * 회원 탈퇴 사유 조회
     *
     * 탈퇴한 사용자의 탈퇴 사유를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 탈퇴 사유 정보
     * @throws ResponseStatusException 사용자 없음 또는 탈퇴 사유 없음
     */
    public com.sobunsobun.backend.dto.account.WithdrawalReasonResponse getWithdrawalReason(Long userId) {
        log.info("[사용자 작동] 회원 탈퇴 사유 조회 - 사용자 ID: {}", userId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 없음 - 사용자 ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });

        // 2. 탈퇴 사유 조회
        WithdrawalReason reason = withdrawalReasonRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("탈퇴 사유 없음 - 사용자 ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "탈퇴 사유 정보를 찾을 수 없습니다.");
                });

        log.info("✅ 탈퇴 사유 조회 완료 - 사용자 ID: {}, 사유: {}", userId, reason.getReasonCode());

        return com.sobunsobun.backend.dto.account.WithdrawalReasonResponse.builder()
                .id(reason.getId())
                .reasonCode(reason.getReasonCode())
                .reasonDetail(reason.getReasonDetail())
                .email(reason.getEmail())
                .nickname(reason.getNickname())
                .mannerScore(reason.getMannerScore())
                .address(reason.getAddress())
                .withdrawnAt(user.getWithdrawnAt())
                .createdAt(reason.getCreatedAt())
                .build();
    }
}
