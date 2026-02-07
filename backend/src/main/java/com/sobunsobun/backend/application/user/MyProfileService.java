package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.mypage.MyProfileResponse;
import com.sobunsobun.backend.dto.mypage.ProfileUpdateRequestDto;
import com.sobunsobun.backend.dto.mypage.ProfileUpdateResponse;
import com.sobunsobun.backend.dto.user.UserProfileResponse;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

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

        // 더미 매너 태그 데이터
        List<MyProfileResponse.MannerTagDto> mannerTags = new ArrayList<>();
        mannerTags.add(MyProfileResponse.MannerTagDto.builder()
                .tagId(1)
                .count(4)
                .build());
        mannerTags.add(MyProfileResponse.MannerTagDto.builder()
                .tagId(3)
                .count(2)
                .build());
        mannerTags.add(MyProfileResponse.MannerTagDto.builder()
                .tagId(5)
                .count(1)
                .build());

        MyProfileResponse profile = MyProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .mannerScore(user.getMannerScore() != null ? user.getMannerScore().doubleValue() : 0.0)
                .participationCount(0)  // TODO: 참여 엔티티 구현 후 조회
                .hostCount((int) groupPostRepository.countByOwnerId(user.getId()))
                .mannerTags(mannerTags)
                .build();

        log.info("프로필 조회 완료 - userId: {}, nickname: {}", userId, user.getNickname());

        return profile;
    }

    /**
     * 다른 사용자 프로필 조회
     *
     * 이미지나 닉네임 클릭 시 해당 유저 프로필을 확인하는 기능
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 프로필 정보
     */
    public UserProfileResponse getUserProfile(Long userId) {
        log.info("다른 사용자 프로필 조회 시작 - userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음 - userId: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");
                });

        // 더미 매너 태그 데이터
        List<UserProfileResponse.MannerTagDto> mannerTags = new ArrayList<>();
        mannerTags.add(UserProfileResponse.MannerTagDto.builder()
                .tagId(1)
                .count(4)
                .build());
        mannerTags.add(UserProfileResponse.MannerTagDto.builder()
                .tagId(3)
                .count(2)
                .build());
        mannerTags.add(UserProfileResponse.MannerTagDto.builder()
                .tagId(5)
                .count(1)
                .build());

        // 사용자가 작성한 게시글 조회
        var posts = groupPostRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
        List<UserProfileResponse.PostItemDto> postItems = posts.stream()
                .map(post -> UserProfileResponse.PostItemDto.builder()
                        .postId(post.getId())
                        .title(post.getTitle())
                        .thumbnailUrl(null)  // TODO: 게시글 이미지 기능 구현 후 추가
                        .status(post.getStatus().toString())
                        .totalAmount(null)  // TODO: 가격 정보 필드 추가 후 구현
                        .unitAmount(null)  // TODO: 1인당 가격 필드 추가 후 구현
                        .currentParticipants(post.getJoinedMembers())
                        .maxParticipants(post.getMaxMembers())
                        .region(null)  // TODO: 지역 필드 추가 후 구현
                        .createdAt(post.getCreatedAt())
                        .deadline(post.getDeadlineAt())
                        .viewCount(0)  // TODO: 조회수 기능 구현
                        .bookmarkCount(0)  // TODO: 북마크 수 조회 구현
                        .build())
                .toList();

        long postCountValue = groupPostRepository.countByOwnerId(user.getId());

        UserProfileResponse profile = UserProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .mannerScore(user.getMannerScore() != null ? user.getMannerScore().doubleValue() : 0.0)
                .participationCount(0)  // TODO: 참여 엔티티 구현 후 조회
                .hostCount((int) postCountValue)
                .postCount((int) postCountValue)
                .mannerTags(mannerTags)
                .posts(postItems)
                .introduction(null)  // TODO: 향후 사용자 소개 필드 추가 시 구현
                .build();

        log.info("다른 사용자 프로필 조회 완료 - userId: {}, nickname: {}, postCount: {}", userId, user.getNickname(), postItems.size());

        return profile;
    }

    /**
     * 닉네임으로 다른 사용자 프로필 조회
     *
     * 닉네임을 클릭했을 때 해당 유저 프로필을 확인하는 기능
     *
     * @param nickname 조회할 사용자 닉네임
     * @return 사용자 프로필 정보
     */
    public UserProfileResponse getUserProfileByNickname(String nickname) {
        log.info("닉네임으로 다른 사용자 프로필 조회 시작 - nickname: {}", nickname);

        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음 - nickname: {}", nickname);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");
                });

        // 더미 매너 태그 데이터
        List<UserProfileResponse.MannerTagDto> mannerTags = new ArrayList<>();
        mannerTags.add(UserProfileResponse.MannerTagDto.builder()
                .tagId(1)
                .count(4)
                .build());
        mannerTags.add(UserProfileResponse.MannerTagDto.builder()
                .tagId(3)
                .count(2)
                .build());
        mannerTags.add(UserProfileResponse.MannerTagDto.builder()
                .tagId(5)
                .count(1)
                .build());

        // 사용자가 작성한 게시글 조회
        var posts = groupPostRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
        List<UserProfileResponse.PostItemDto> postItems = posts.stream()
                .map(post -> UserProfileResponse.PostItemDto.builder()
                        .postId(post.getId())
                        .title(post.getTitle())
                        .thumbnailUrl(null)  // TODO: 게시글 이미지 기능 구현 후 추가
                        .status(post.getStatus().toString())
                        .totalAmount(null)  // TODO: 가격 정보 필드 추가 후 구현
                        .unitAmount(null)  // TODO: 1인당 가격 필드 추가 후 구현
                        .currentParticipants(post.getJoinedMembers())
                        .maxParticipants(post.getMaxMembers())
                        .region(null)  // TODO: 지역 필드 추가 후 구현
                        .createdAt(post.getCreatedAt())
                        .deadline(post.getDeadlineAt())
                        .viewCount(0)  // TODO: 조회수 기능 구현
                        .bookmarkCount(0)  // TODO: 북마크 수 조회 구현
                        .build())
                .toList();

        long postCountValue = groupPostRepository.countByOwnerId(user.getId());

        UserProfileResponse profile = UserProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .mannerScore(user.getMannerScore() != null ? user.getMannerScore().doubleValue() : 0.0)
                .participationCount(0)  // TODO: 참여 엔티티 구현 후 조회
                .hostCount((int) postCountValue)
                .postCount((int) postCountValue)
                .mannerTags(mannerTags)
                .posts(postItems)
                .introduction(null)  // TODO: 향후 사용자 소개 필드 추가 시 구현
                .build();

        log.info("닉네임으로 다른 사용자 프로필 조회 완료 - nickname: {}, userId: {}, postCount: {}", nickname, user.getId(), postItems.size());

        return profile;
    }

//    /**
//     * 사용자 프로필 수정
//     *
//     * @param userId 사용자 ID
//     * @param request 프로필 수정 요청
//     * @return 수정된 프로필 정보
//     */
//    @Transactional
//    public ProfileUpdateResponse updateProfile(Long userId, ProfileUpdateRequestDto request) {
//        log.info("프로필 수정 시작 - userId: {}, nickname: {}", userId, request.getNickname());
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> {
//                    log.error("사용자를 찾을 수 없음 - userId: {}", userId);
//                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");
//                });
//
//        // 닉네임 수정
//        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
//            // 중복 검사
//            if (!user.getNickname().equals(request.getNickname())) {
//                boolean nicknameDuplicate = userRepository.existsByNickname(request.getNickname());
//                if (nicknameDuplicate) {
//                    log.warn("닉네임 중복 - nickname: {}", request.getNickname());
//                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다");
//                }
//            }
//            user.setNickname(request.getNickname());
//        }
//
//        // 프로필 이미지 수정
//        if (request.getProfileImageUrl() != null) {
//            user.setProfileImageUrl(request.getProfileImageUrl());
//        }
//
//        User savedUser = userRepository.save(user);
//
//        ProfileUpdateResponse response = ProfileUpdateResponse.builder()
//                .userId(savedUser.getId())
//                .nickname(savedUser.getNickname())
//                .profileImageUrl(savedUser.getProfileImageUrl())
//                .message("프로필이 수정되었습니다.")
//                .build();
//
//        log.info("프로필 수정 완료 - userId: {}, nickname: {}", userId, savedUser.getNickname());
//
//        return response;
//    }
}

