package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.mypage.MyProfileResponse;
import com.sobunsobun.backend.dto.mypage.ProfileUpdateRequestDto;
import com.sobunsobun.backend.dto.mypage.ProfileUpdateResponse;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사용자 프로필 비즈니스 로직 서비스
 *
 * 담당 기능:
 * - 프로필 조회 (닉네임, 프로필 이미지, 매너 점수, 통계)
 * - 프로필 수정 (닉네임, 프로필 이미지)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyProfileService {

    private final UserRepository userRepository;
    private final GroupPostRepository groupPostRepository;

    /**
     * 사용자 프로필 조회
     *
     * @param userId 사용자 ID
     * @return 프로필 정보
     */
    public MyProfileResponse getProfile(Long userId) {
        log.info("프로필 조회 시작 - userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음 - userId: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");
                });

        MyProfileResponse profile = MyProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .mannerScore(user.getMannerScore() != null ? user.getMannerScore().doubleValue() : 0.0)
                .participationCount(0)  // TODO: 참여 엔티티 구현 후 조회
                .hostCount((int) groupPostRepository.countByOwnerId(user.getId()))
                .build();

        log.info("프로필 조회 완료 - userId: {}, nickname: {}", userId, user.getNickname());

        return profile;
    }

    /**
     * 사용자 프로필 수정
     *
     * @param userId 사용자 ID
     * @param request 프로필 수정 요청
     * @return 수정된 프로필 정보
     */
    @Transactional
    public ProfileUpdateResponse updateProfile(Long userId, ProfileUpdateRequestDto request) {
        log.info("프로필 수정 시작 - userId: {}, nickname: {}", userId, request.getNickname());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음 - userId: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");
                });

        // 닉네임 수정
        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            // 중복 검사
            if (!user.getNickname().equals(request.getNickname())) {
                boolean nicknameDuplicate = userRepository.existsByNickname(request.getNickname());
                if (nicknameDuplicate) {
                    log.warn("닉네임 중복 - nickname: {}", request.getNickname());
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다");
                }
            }
            user.setNickname(request.getNickname());
        }

        // 프로필 이미지 수정
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        User savedUser = userRepository.save(user);

        ProfileUpdateResponse response = ProfileUpdateResponse.builder()
                .userId(savedUser.getId())
                .nickname(savedUser.getNickname())
                .profileImageUrl(savedUser.getProfileImageUrl())
                .message("프로필이 수정되었습니다.")
                .build();

        log.info("프로필 수정 완료 - userId: {}, nickname: {}", userId, savedUser.getNickname());

        return response;
    }
}

