package com.sobunsobun.backend.application.post;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.PostStatus;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.post.*;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 공동구매 게시글 비즈니스 로직 서비스
 *
 * 담당 기능:
 * - 게시글 CRUD (생성, 조회, 수정, 삭제)
 * - 게시글 목록 조회 (페이징, 필터링)
 * - 게시글 상태 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final GroupPostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 생성
     *
     * @param userId 작성자 ID
     * @param request 게시글 생성 요청 데이터
     * @return 생성된 게시글 정보
     */
    @Transactional
    public PostResponse createPost(Long userId, PostCreateRequest request) {
        log.info("[사용자 작동] 게시글 생성 시도 - 사용자 ID: {}, 제목: {}", userId, request.getTitle());

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음 - ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");
                });

        // 2. 게시글 엔티티 생성
        GroupPost post = GroupPost.builder()
                .owner(user)
                .title(request.getTitle())
                .categories(request.getCategories())
                .itemsText(request.getItemsText())
                .notesText(request.getNotesText())
                .locationName(request.getLocationName())
                .verifyLocation(request.getVerifyLocation())
                .meetAt(request.getMeetAt())
                .deadlineAt(request.getDeadlineAt())
                .minMembers(request.getMinMembers())
                .maxMembers(request.getMaxMembers())
                .status(PostStatus.OPEN)
                .build();

        // 3. 저장
        GroupPost savedPost = postRepository.save(post);
        log.info("[사용자 작동] 게시글 생성 완료 - 게시글 ID: {}, 사용자 ID: {}", savedPost.getId(), userId);

        return convertToResponse(savedPost);
    }

    /**
     * 게시글 단건 조회
     *
     * @param postId 게시글 ID
     * @return 게시글 정보
     */
    public PostResponse getPost(Long postId) {
        log.info("[사용자 작동] 게시글 조회 - 게시글 ID: {}", postId);

        GroupPost post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("게시글을 찾을 수 없음 - ID: {}", postId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다");
                });

        return convertToResponse(post);
    }

    /**
     * 전체 게시글 목록 조회 (페이징, 최신순)
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 게시글 목록
     */
    public PostListResponse getAllPosts(int page, int size) {
        log.info("전체 게시글 목록 조회 - 페이지: {}, 크기: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<GroupPost> postPage = postRepository.findAll(pageable);

        log.info("DB 조회 결과 - 전체 게시글 수: {}, 현재 페이지 게시글 수: {}, 총 페이지: {}",
                 postPage.getTotalElements(), postPage.getNumberOfElements(), postPage.getTotalPages());

        return convertToListResponse(postPage);
    }

    /**
     * 상태별 게시글 목록 조회 (마감일 오름차순)
     *
     * @param status 게시글 상태 (OPEN, CLOSED, CANCELLED)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 페이징된 게시글 목록
     */
    public PostListResponse getPostsByStatus(String status, int page, int size) {
        log.info("상태별 게시글 목록 조회 - 상태: {}, 페이지: {}, 크기: {}", status, page, size);

        try {
            PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
            Pageable pageable = PageRequest.of(page, size);
            Page<GroupPost> postPage = postRepository.findByStatusOrderByDeadlineAtAsc(postStatus, pageable);

            return convertToListResponse(postPage);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 상태 값 입력 {}: {}", e.getClass().getSimpleName(), status);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 상태를 입력하세요 (OPEN, CLOSED, CANCELLED)");
        }
    }

    /**
     * 카테고리별 게시글 목록 조회 (모집 중만)
     *
     * @param categories 카테고리 코드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 페이징된 게시글 목록
     */
    public PostListResponse getPostsByCategories(String categories, int page, int size) {
        log.info("카테고리별 게시글 목록 조회 - 카테고리: {}, 페이지: {}, 크기: {}", categories, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<GroupPost> postPage = postRepository.findByCategoriesAndStatusOrderByCreatedAtDesc(
                categories, PostStatus.OPEN, pageable);

        log.info("DB 조회 결과 - 카테고리: {}, 전체: {}, 현재 페이지: {}, 총 페이지: {}",
                 categories, postPage.getTotalElements(), postPage.getNumberOfElements(), postPage.getTotalPages());

        return convertToListResponse(postPage);
    }

    /**
     * 여러 카테고리로 게시글 목록 조회 (모집 중만)
     *
     * @param categoriesList 카테고리 코드 리스트
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 페이징된 게시글 목록
     */
    public PostListResponse getPostsByMultipleCategories(List<String> categoriesList, int page, int size) {
        log.info("여러 카테고리 게시글 목록 조회 - 카테고리: {}, 페이지: {}, 크기: {}", categoriesList, page, size);

        // REGEXP 패턴 생성: "0001|0002|0003" 형태
        String categoryPattern = String.join("|", categoriesList);
        log.info("생성된 REGEXP 패턴: {}", categoryPattern);

        Pageable pageable = PageRequest.of(page, size);
        Page<GroupPost> postPage = postRepository.findByCategoriesInAndStatus(
                categoryPattern, PostStatus.OPEN.name(), pageable);

        log.info("DB 조회 결과 - 카테고리 패턴: {}, 전체: {}, 현재 페이지: {}, 총 페이지: {}",
                 categoryPattern, postPage.getTotalElements(), postPage.getNumberOfElements(), postPage.getTotalPages());

        return convertToListResponse(postPage);
    }

    /**
     * 내가 작성한 게시글 목록 조회 (최신순)
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 페이징된 게시글 목록
     */
    public PostListResponse getMyPosts(Long userId, int page, int size) {
        log.info("내 게시글 목록 조회 - 사용자 ID: {}, 페이지: {}, 크기: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<GroupPost> postPage = postRepository.findByOwnerIdOrderByCreatedAtDesc(userId, pageable);

        return convertToListResponse(postPage);
    }

    /**
     * 게시글 수정 (작성자만 가능)
     *
     * @param postId 게시글 ID
     * @param userId 요청 사용자 ID
     * @param request 수정 요청 데이터
     * @return 수정된 게시글 정보
     */
    @Transactional
    public PostResponse updatePost(Long postId, Long userId, PostUpdateRequest request) {
        log.info("[사용자 작동] 게시글 수정 시도 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);

        // 1. 게시글 조회
        GroupPost post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("게시글을 찾을 수 없음 - ID: {}", postId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다");
                });

        // 2. 작성자 확인
        if (!post.getOwner().getId().equals(userId)) {
            log.error("게시글 수정 권한 없음 - 게시글 작성자 ID: {}, 요청자 ID: {}", post.getOwner().getId(), userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게시글 수정 권한이 없습니다");
        }

        // 3. 수정 (null이 아닌 필드만 업데이트)
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getCategories() != null) {
            post.setCategories(request.getCategories());
        }
        if (request.getItemsText() != null) {
            post.setItemsText(request.getItemsText());
        }
        if (request.getNotesText() != null) {
            post.setNotesText(request.getNotesText());
        }
        if (request.getLocationName() != null) {
            post.setLocationName(request.getLocationName());
        }
        if (request.getVerifyLocation() != null) {
            post.setVerifyLocation(request.getVerifyLocation());
        }
        if (request.getMeetAt() != null) {
            post.setMeetAt(request.getMeetAt());
        }
        if (request.getDeadlineAt() != null) {
            post.setDeadlineAt(request.getDeadlineAt());
        }
        if (request.getMinMembers() != null) {
            post.setMinMembers(request.getMinMembers());
        }
        if (request.getMaxMembers() != null) {
            post.setMaxMembers(request.getMaxMembers());
        }
        if (request.getStatus() != null) {
            try {
                post.setStatus(PostStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.error("잘못된 상태 값 입력 {}: {}", e.getClass().getSimpleName(), request.getStatus());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 상태를 입력하세요 (OPEN, CLOSED, CANCELLED)");
            }
        }

        log.info("[사용자 작동] 게시글 수정 완료 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);
        return convertToResponse(post);
    }

    /**
     * 게시글 삭제 (작성자만 가능)
     *
     * @param postId 게시글 ID
     * @param userId 요청 사용자 ID
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        log.info("[사용자 작동] 게시글 삭제 시도 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);

        // 1. 게시글 조회
        GroupPost post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("게시글을 찾을 수 없음 - ID: {}", postId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다");
                });

        // 2. 작성자 확인
        if (!post.getOwner().getId().equals(userId)) {
            log.error("게시글 삭제 권한 없음 - 게시글 작성자 ID: {}, 요청자 ID: {}", post.getOwner().getId(), userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게시글 삭제 권한이 없습니다");
        }

        // 3. 삭제
        postRepository.delete(post);
        log.info("[사용자 작동] 게시글 삭제 완료 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);
    }

    /**
     * GroupPost 엔티티를 PostResponse DTO로 변환
     */
    private PostResponse convertToResponse(GroupPost post) {
        return PostResponse.builder()
                .id(post.getId())
                .owner(PostResponse.OwnerInfo.builder()
                        .id(post.getOwner().getId())
                        .nickname(post.getOwner().getNickname())
                        .profileImageUrl(post.getOwner().getProfileImageUrl())
                        .build())
                .title(post.getTitle())
                .categoryCode(post.getCategories())
                .content(post.getContent())
                .itemsText(post.getItemsText())
                .notesText(post.getNotesText())
                .locationName(post.getLocationName())
                .verifyLocation(post.getVerifyLocation())
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
     * Page<GroupPost>를 PostListResponse DTO로 변환
     */
    private PostListResponse convertToListResponse(Page<GroupPost> postPage) {
        return PostListResponse.builder()
                .posts(postPage.getContent().stream()
                        .map(this::convertToResponse)
                        .toList())
                .pageInfo(PostListResponse.PageInfo.builder()
                        .currentPage(postPage.getNumber())
                        .pageSize(postPage.getSize())
                        .totalElements(postPage.getTotalElements())
                        .totalPages(postPage.getTotalPages())
                        .isLast(postPage.isLast())
                        .build())
                .build();
    }
}


