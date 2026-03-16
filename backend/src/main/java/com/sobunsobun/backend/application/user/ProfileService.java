package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.SavedPost;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.post.PostListResponse;
import com.sobunsobun.backend.dto.post.PostResponse;
import com.sobunsobun.backend.dto.profile.MyProfileDetailResponse;
import com.sobunsobun.backend.dto.profile.PublicUserProfileResponse;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.SavedPostRepository;
import com.sobunsobun.backend.repository.UserReportRepository;
import com.sobunsobun.backend.repository.UserTagStatsRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.BusinessException;
import com.sobunsobun.backend.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 유저 프로필 조회 서비스
 *
 * 담당 기능:
 * - 내 프로필 탭별 게시글 조회 (내 글 / 댓글 단 글 / 저장한 글)
 * - 타 유저 프로필 조회 (작성 게시글 목록)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final GroupPostRepository groupPostRepository;
    private final SavedPostRepository savedPostRepository;
    private final UserTagStatsRepository userTagStatsRepository;
    private final UserReportRepository userReportRepository;

    /**
     * 내 프로필 조회 (탭별 페이징)
     *
     * @param userId 현재 인증된 사용자 ID
     * @param tab    조회 탭: "posts"(내 글) | "commented"(댓글 단 글) | "saved"(저장한 글)
     * @param page   페이지 번호 (0부터 시작)
     * @param size   페이지 크기
     */
    public MyProfileDetailResponse getMyProfile(Long userId, String tab, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PostListResponse posts = switch (tab.toLowerCase()) {
            case "commented" -> {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<GroupPost> result = groupPostRepository.findPostsCommentedByUser(userId, pageable);
                yield toPostListResponse(result);
            }
            case "saved" -> {
                Pageable pageable = PageRequest.of(page, size);
                Page<SavedPost> result = savedPostRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
                yield toPostListResponseFromSaved(result);
            }
            default -> { // "posts"
                Pageable pageable = PageRequest.of(page, size);
                Page<GroupPost> result = groupPostRepository.findByOwnerIdOrderByCreatedAtDesc(userId, pageable);
                yield toPostListResponse(result);
            }
        };

        int hostCount = (int) groupPostRepository.countByOwnerId(userId);
        int participationCount = 0;  // TODO: 참여 엔티티 구현 후 조회
        int tagCount = userTagStatsRepository.sumCountByReceiverId(userId);
        int reportedCount = (int) userReportRepository.countByTargetUserId(userId);
        int activityScore = hostCount * 3 + participationCount * 2 + tagCount - reportedCount * 5;

        List<MyProfileDetailResponse.MannerTagDto> mannerTags = userTagStatsRepository
                .findTop5ByReceiverIdOrderByCountDesc(userId)
                .stream()
                .map(stats -> MyProfileDetailResponse.MannerTagDto.builder()
                        .tagId(stats.getTagCode().getId())
                        .label(stats.getTagCode().getLabel())
                        .count(stats.getCount())
                        .build())
                .toList();

        log.info("내 프로필 조회 - userId: {}, tab: {}, totalElements: {}",
                userId, tab, posts.getPageInfo().getTotalElements());

        return MyProfileDetailResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .activityScore(activityScore)
                .hostCount(hostCount)
                .participationCount(participationCount)
                .mannerTags(mannerTags)
                .tab(tab.toLowerCase())
                .posts(posts)
                .build();
    }

    /**
     * 타 유저 프로필 조회
     *
     * @param targetUserId 조회할 사용자 ID
     * @param page         페이지 번호 (0부터 시작)
     * @param size         페이지 크기
     */
    public PublicUserProfileResponse getUserProfile(Long targetUserId, int page, int size) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int hostCount = (int) groupPostRepository.countByOwnerId(targetUserId);
        int participationCount = 0;  // TODO: 참여 엔티티 구현 후 조회
        int tagCount = userTagStatsRepository.sumCountByReceiverId(targetUserId);
        int reportedCount = (int) userReportRepository.countByTargetUserId(targetUserId);
        int activityScore = hostCount * 3 + participationCount * 2 + tagCount - reportedCount * 5;

        List<PublicUserProfileResponse.MannerTagDto> mannerTags = userTagStatsRepository
                .findTop5ByReceiverIdOrderByCountDesc(targetUserId)
                .stream()
                .map(stats -> PublicUserProfileResponse.MannerTagDto.builder()
                        .tagId(stats.getTagCode().getId())
                        .label(stats.getTagCode().getLabel())
                        .count(stats.getCount())
                        .build())
                .toList();

        Pageable pageable = PageRequest.of(page, size);
        Page<GroupPost> postPage = groupPostRepository.findByOwnerIdOrderByCreatedAtDesc(targetUserId, pageable);

        log.info("타 유저 프로필 조회 - userId: {}, totalPosts: {}",
                targetUserId, postPage.getTotalElements());

        return PublicUserProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .activityScore(activityScore)
                .hostCount(hostCount)
                .participationCount(participationCount)
                .mannerTags(mannerTags)
                .posts(toPostListResponse(postPage))
                .build();
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private PostListResponse toPostListResponse(Page<GroupPost> page) {
        return PostListResponse.builder()
                .posts(page.getContent().stream().map(this::toPostResponse).toList())
                .pageInfo(PostListResponse.PageInfo.builder()
                        .currentPage(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .first(page.isFirst())
                        .last(page.isLast())
                        .hasNext(page.hasNext())
                        .hasPrevious(page.hasPrevious())
                        .build())
                .build();
    }

    /** SavedPost 페이지 → 내부 GroupPost를 꺼내어 PostListResponse로 변환 */
    private PostListResponse toPostListResponseFromSaved(Page<SavedPost> page) {
        return PostListResponse.builder()
                .posts(page.getContent().stream()
                        .map(savedPost -> toPostResponse(savedPost.getPost()))
                        .toList())
                .pageInfo(PostListResponse.PageInfo.builder()
                        .currentPage(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .first(page.isFirst())
                        .last(page.isLast())
                        .hasNext(page.hasNext())
                        .hasPrevious(page.hasPrevious())
                        .build())
                .build();
    }

    private PostResponse toPostResponse(GroupPost post) {
        return PostResponse.builder()
                .id(post.getId())
                .owner(PostResponse.OwnerInfo.builder()
                        .id(post.getOwner().getId())
                        .nickname(post.getOwner().getNickname())
                        .profileImageUrl(post.getOwner().getProfileImageUrl())
                        .address(post.getOwner().getAddress())
                        .build())
                .title(post.getTitle())
                .categoryCode(post.getCategories())
                .content(post.getContent())
                .itemsText(post.getItemsText())
                .notesText(post.getNotesText())
                .locationName(post.getLocationName())
                .meetAt(post.getMeetAt())
                .deadlineAt(post.getDeadlineAt())
                .minMembers(post.getMinMembers())
                .maxMembers(post.getMaxMembers())
                .joinedMembers(post.getJoinedMembers())
                .status(post.getStatus())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
