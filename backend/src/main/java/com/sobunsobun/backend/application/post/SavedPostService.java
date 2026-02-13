package com.sobunsobun.backend.application.post;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.SavedPost;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.post.SavedPostDto;
import com.sobunsobun.backend.dto.post.PostResponse;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.SavedPostRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 저장된 게시글 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SavedPostService {

    private final SavedPostRepository savedPostRepository;
    private final GroupPostRepository groupPostRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 저장
     */
    public SavedPostDto.Response savePost(Long userId, Long postId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 게시글 조회
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다"));

        // 자신의 게시글 저장 방지
        if (post.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("자신의 게시글은 저장할 수 없습니다");
        }

        // 이미 저장했는지 확인
        Optional<SavedPost> existingSave = savedPostRepository.findByUserIdAndPostId(userId, postId);
        if (existingSave.isPresent()) {
            // 이미 저장된 경우 기존 저장 항목 반환
            return convertToResponse(existingSave.get());
        }

        // 저장 생성
        SavedPost savedPost = SavedPost.builder()
                .user(user)
                .post(post)
                .createdAt(LocalDateTime.now())
                .build();

        SavedPost savedRecord = savedPostRepository.save(savedPost);

        log.info("게시글 저장 - 사용자: {}, 게시글: {}", userId, postId);

        return convertToResponse(savedRecord);
    }

    /**
     * 저장된 게시글 조회
     */
    @Transactional(readOnly = true)
    public SavedPostDto.Response getSavedPost(Long savedPostId) {
        SavedPost savedPost = savedPostRepository.findById(savedPostId)
                .orElseThrow(() -> new IllegalArgumentException("저장된 게시글을 찾을 수 없습니다"));

        return convertToResponse(savedPost);
    }

    /**
     * 사용자의 저장된 게시글 목록 조회 (PostResponse 형식)
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getMySavedPostsAsPostResponse(Long userId, Pageable pageable) {
        return savedPostRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::convertSavedPostToPostResponse);
    }

    /**
     * 사용자의 저장된 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<SavedPostDto.ListResponse> getMySavedPosts(Long userId, Pageable pageable) {
        return savedPostRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::convertToListResponse);
    }

    /**
     * 게시글 저장 해제
     */
    public void unsavePost(Long userId, Long postId) {
        // 저장 여부 확인
        SavedPost savedPost = savedPostRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new IllegalArgumentException("저장된 게시글이 없습니다"));

        savedPostRepository.delete(savedPost);

        log.info("게시글 저장 해제 - 사용자: {}, 게시글: {}", userId, postId);
    }

    /**
     * 게시글 저장 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isSaved(Long userId, Long postId) {
        return savedPostRepository.findByUserIdAndPostId(userId, postId).isPresent();
    }

    /**
     * 사용자의 저장된 게시글 개수
     */
    @Transactional(readOnly = true)
    public long countSavedPosts(Long userId) {
        return savedPostRepository.countByUserId(userId);
    }

    /**
     * 게시글의 저장 횟수 (인기도 지표)
     */
    @Transactional(readOnly = true)
    public long countPostSaves(Long postId) {
        return savedPostRepository.countByPostId(postId);
    }

    /**
     * 저장된 게시글 통계
     */
    @Transactional(readOnly = true)
    public SavedPostDto.StatisticsResponse getSavedPostStatistics(Long userId) {
        long totalSavedPosts = savedPostRepository.countByUserId(userId);

        // 활성 게시글과 종료된 게시글 개수 집계
        Page<SavedPost> savedPosts = savedPostRepository.findByUserIdOrderByCreatedAtDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));

        long activePosts = 0;
        long closedPosts = 0;

        for (SavedPost saved : savedPosts) {
            if ("OPEN".equals(saved.getPost().getStatus().name())) {
                activePosts++;
            } else {
                closedPosts++;
            }
        }

        return SavedPostDto.StatisticsResponse.builder()
                .userId(userId)
                .totalSavedPosts(totalSavedPosts)
                .activePosts(activePosts)
                .closedPosts(closedPosts)
                .build();
    }

    /**
     * SavedPost를 PostResponse로 변환
     */
    private PostResponse convertSavedPostToPostResponse(SavedPost savedPost) {
        GroupPost post = savedPost.getPost();
        User owner = post.getOwner();

        return PostResponse.builder()
                .id(post.getId())
                .owner(PostResponse.OwnerInfo.builder()
                        .id(owner.getId())
                        .nickname(owner.getNickname())
                        .profileImageUrl(owner.getProfileImageUrl())
                        .address(owner.getAddress())
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

    /**
     * SavedPost를 Response DTO로 변환
     */
    private SavedPostDto.Response convertToResponse(SavedPost savedPost) {
        return SavedPostDto.Response.builder()
                .id(savedPost.getId())
                .postId(savedPost.getPost().getId())
                .postTitle(savedPost.getPost().getTitle())
                .postOwnerId(savedPost.getPost().getOwner().getId())
                .postOwnerName(savedPost.getPost().getOwner().getNickname())
                .postCategory(savedPost.getPost().getCategories())
                .postCreatedAt(savedPost.getPost().getCreatedAt())
                .savedAt(savedPost.getCreatedAt())
                .build();
    }

    /**
     * SavedPost를 ListResponse DTO로 변환
     */
    private SavedPostDto.ListResponse convertToListResponse(SavedPost savedPost) {
        return SavedPostDto.ListResponse.builder()
                .id(savedPost.getId())
                .postId(savedPost.getPost().getId())
                .postTitle(savedPost.getPost().getTitle())
                .postOwnerId(savedPost.getPost().getOwner().getId())
                .postOwnerName(savedPost.getPost().getOwner().getNickname())
                .postCategory(savedPost.getPost().getCategories())
                .joinedMembers(savedPost.getPost().getJoinedMembers())
                .maxMembers(savedPost.getPost().getMaxMembers())
                .postCreatedAt(savedPost.getPost().getCreatedAt())
                .savedAt(savedPost.getCreatedAt())
                .build();
    }
}
