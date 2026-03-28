package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.Comment;
import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.PostStatus;
import com.sobunsobun.backend.domain.SavedPost;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.post.PostListResponse;
import com.sobunsobun.backend.dto.post.PostResponse;
import com.sobunsobun.backend.dto.profile.MyCommentResponse;
import com.sobunsobun.backend.dto.profile.MyProfileDetailResponse;
import com.sobunsobun.backend.dto.profile.PublicUserProfileResponse;
import com.sobunsobun.backend.repository.BlockedUserRepository;
import com.sobunsobun.backend.repository.CommentRepository;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.SavedPostRepository;
import com.sobunsobun.backend.repository.UserReportRepository;
import com.sobunsobun.backend.repository.UserTagStatsRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final BlockedUserRepository blockedUserRepository;
    private final CommentRepository commentRepository;
    private final ChatMemberRepository chatMemberRepository;

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
                Page<SavedPost> result = savedPostRepository.findByUserIdOrderByCreatedAtDescSafe(userId, pageable);
                yield toPostListResponseFromSaved(result);
            }
            default -> { // "posts"
                Pageable pageable = PageRequest.of(page, size);
                Page<GroupPost> result = groupPostRepository.findByOwnerIdAndStatusNotOrderByCreatedAtDesc(userId, PostStatus.CANCELLED, pageable);
                yield toPostListResponse(result);
            }
        };

        int hostCount = (int) groupPostRepository.countByOwnerId(userId);
        int completedHostCount = (int) groupPostRepository.countByOwnerIdAndStatus(userId, PostStatus.COMPLETED);
        int participationCount = (int) chatMemberRepository.countParticipationByUserId(userId);
        int tagCount = userTagStatsRepository.sumCountByReceiverId(userId);
        int reportedCount = (int) userReportRepository.countByTargetUserId(userId);
        int activityScore = completedHostCount * 8 + participationCount * 3 + tagCount - reportedCount * 7;

        List<MyProfileDetailResponse.MannerTagDto> mannerTags = userTagStatsRepository
                .findTop5ByReceiverIdOrderByCountDesc(userId)
                .stream()
                .map(stats -> MyProfileDetailResponse.MannerTagDto.builder()
                        .tagId(stats.getTagCode().getId())
                        .label(stats.getTagCode().getLabel())
                        .count(stats.getCount())
                        .build())
                .toList();

        // 각 게시글에 내가 남긴 최신 댓글 1개 세팅 (batch 조회로 N+1 방지)
        List<Long> postIds = posts.getPosts().stream().map(PostResponse::getId).toList();
        if (!postIds.isEmpty()) {
            Map<Long, MyCommentResponse> latestCommentMap = commentRepository
                    .findLatestCommentsByUserIdAndPostIds(userId, postIds)
                    .stream()
                    .collect(Collectors.toMap(
                            c -> c.getPost().getId(),
                            MyCommentResponse::from,
                            (a, b) -> a  // createdAt DESC 정렬이므로 첫 번째 = 최신
                    ));
            posts.getPosts().forEach(p -> p.setLatestComment(latestCommentMap.get(p.getId())));
        }

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
     * @param currentUserId 현재 인증된 사용자 ID (null 가능 - 미인증 사용자)
     * @param targetUserId 조회할 사용자 ID
     * @param page         페이지 번호 (0부터 시작)
     * @param size         페이지 크기
     */
    public PublicUserProfileResponse getUserProfile(Long currentUserId, Long targetUserId, int page, int size) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int hostCount = (int) groupPostRepository.countByOwnerId(targetUserId);
        int completedHostCount = (int) groupPostRepository.countByOwnerIdAndStatus(targetUserId, PostStatus.COMPLETED);
        int participationCount = (int) chatMemberRepository.countParticipationByUserId(targetUserId);
        int tagCount = userTagStatsRepository.sumCountByReceiverId(targetUserId);
        int reportedCount = (int) userReportRepository.countByTargetUserId(targetUserId);
        int activityScore = completedHostCount * 8 + participationCount * 3 + tagCount - reportedCount * 7;

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
        Page<GroupPost> postPage = groupPostRepository.findByOwnerIdAndStatusNotOrderByCreatedAtDesc(targetUserId, PostStatus.CANCELLED, pageable);

        // 현재 사용자가 해당 타 유저를 차단했는지 확인
        Boolean isBlocked = false;
        if (currentUserId != null) {
            isBlocked = blockedUserRepository.existsByBlockerIdAndBlockedId(currentUserId, targetUserId);
        }

        log.info("타 유저 프로필 조회 - currentUserId: {}, targetUserId: {}, isBlocked: {}, totalPosts: {}",
                currentUserId, targetUserId, isBlocked, postPage.getTotalElements());

        return PublicUserProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .activityScore(activityScore)
                .hostCount(hostCount)
                .participationCount(participationCount)
                .mannerTags(mannerTags)
                .isBlocked(isBlocked)
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
        // flatMap으로 변환 중 예외 발생 시 해당 항목만 건너뜀
        // (DB 정합성 문제로 삭제된 GroupPost를 참조하는 SavedPost가 남아있는 경우 대비)
        List<PostResponse> posts = page.getContent().stream()
                .flatMap(savedPost -> {
                    try {
                        return Stream.of(toPostResponse(savedPost.getPost()));
                    } catch (Exception e) {
                        log.warn("저장된 게시글 조회 중 삭제된 게시글 건너뜀 - SavedPost id: {}", savedPost.getId());
                        return Stream.empty();
                    }
                })
                .toList();
        
        return PostListResponse.builder()
                .posts(posts)
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
