package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.application.file.FileStorageService;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.util.NicknameNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사용자 비즈니스 로직 서비스
 *
 * 담당 기능:
 * - 닉네임 중복 확인 및 정규화
 * - 사용자 프로필 관리
 * - 닉네임 유효성 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
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
        log.debug("닉네임 사용 가능 여부 확인 시작: {}", rawNickname);

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
            log.info("닉네임 업데이트 완료 - 사용자 ID: {}, {} -> {}", userId, oldNickname, normalizedNickname);
        } catch (DataIntegrityViolationException e) {
            log.error("닉네임 중복 DB 오류 - 사용자 ID: {}, 닉네임: {}", userId, normalizedNickname, e);
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
        log.info("프로필 업데이트 시작 - 사용자 ID: {}, 닉네임: {}, 이미지 있음: {}",
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
                log.error("프로필 이미지 업로드 실패 - 사용자 ID: {}", userId, e);
                throw e;
            }
        }

        // 5. 닉네임 업데이트
        String oldNickname = user.getNickname();
        user.setNickname(normalizedNickname);

        try {
            userRepository.saveAndFlush(user);
            log.info("프로필 업데이트 완료 - 사용자 ID: {}, 닉네임: {} -> {}, 이미지: {}",
                    userId, oldNickname, normalizedNickname, user.getProfileImageUrl());
        } catch (DataIntegrityViolationException e) {
            log.error("프로필 업데이트 DB 오류 - 사용자 ID: {}", userId, e);
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
        log.info("프로필 이미지 업데이트 시작 - 사용자 ID: {}", userId);

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

        log.info("프로필 이미지 업데이트 완료 - 사용자 ID: {}, 최종 URL: {}", userId, newImageUrl);
    }
}
