package com.sobunsobun.backend.application.search;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.PostStatus;
import com.sobunsobun.backend.dto.post.PostListResponse;
import com.sobunsobun.backend.dto.post.PostResponse;
import com.sobunsobun.backend.dto.post.PostSearchRequest;
import com.sobunsobun.backend.repository.search.SearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 검색 서비스
 * 공동구매 게시글 검색 비즈니스 로직 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final SearchRepository searchRepository;

    /**
     * 검색어로 게시글 검색 (OPEN 상태만)
     * title, categories, itemsText, locationName 중 하나라도 포함되면 검색됨
     *
     * @param request 검색 요청 데이터
     * @return 검색 결과 (페이징)
     */
    public PostListResponse searchPosts(PostSearchRequest request) {
        log.info("[검색] 게시글 검색 시작 (OPEN 상태만) - 키워드: '{}', 정렬: {}, 페이지: {}, 크기: {}",
                request.getKeyword(), request.getSortBy(), request.getPage(), request.getSize());

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize()
        );

        Page<GroupPost> postPage;
        if ("deadline".equalsIgnoreCase(request.getSortBy())) {
            // 마감임박순
            postPage = searchRepository.searchByKeywordOrderByDeadline(request.getKeyword(), pageable);
        } else {
            // 최신순 (기본값)
            postPage = searchRepository.searchByKeyword(request.getKeyword(), pageable);
        }

        log.info("[검색] 검색 완료 - 총 {}건 발견, 현재 페이지: {}/{}",
                postPage.getTotalElements(),
                postPage.getNumber() + 1,
                postPage.getTotalPages());

        return convertToListResponse(postPage);
    }

    /**
     * 특정 상태의 게시글만 검색
     *
     * @param keyword 검색 키워드
     * @param status 게시글 상태 (OPEN, CLOSED, CANCELLED)
     * @param sortBy 정렬 기준 (latest: 최신순, deadline: 마감임박순)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색 결과 (페이징)
     */
    public PostListResponse searchPostsByStatus(String keyword, String status, String sortBy, int page, int size) {
        log.info("[검색] 상태별 게시글 검색 시작 - 키워드: '{}', 상태: {}, 정렬: {}, 페이지: {}, 크기: {}",
                keyword, status, sortBy, page, size);

        Pageable pageable = PageRequest.of(page, size);
        PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
        Page<GroupPost> postPage;

        if ("deadline".equalsIgnoreCase(sortBy)) {
            // 마감임박순
            postPage = searchRepository.searchByKeywordAndStatusOrderByDeadline(keyword, postStatus, pageable);
        } else {
            // 최신순 (기본값)
            postPage = searchRepository.searchByKeywordAndStatus(keyword, postStatus, pageable);
        }

        log.info("[검색] 검색 완료 - 총 {}건 발견, 현재 페이지: {}/{}",
                postPage.getTotalElements(),
                postPage.getNumber() + 1,
                postPage.getTotalPages());

        return convertToListResponse(postPage);
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

