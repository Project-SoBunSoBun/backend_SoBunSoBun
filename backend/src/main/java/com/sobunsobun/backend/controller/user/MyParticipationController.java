package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.common.PageResponse;
import com.sobunsobun.backend.dto.mypage.BookmarkItemResponse;
import com.sobunsobun.backend.dto.mypage.MyPostItemResponse;
import com.sobunsobun.backend.dto.mypage.ParticipationItemResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 참여 내역 관리 컨트롤러
 *
 * 담당 기능:
 * - 참여한 공동구매 목록 조회
 * - 내가 작성한 글 목록 조회
 * - 북마크한 글 목록 조회
 * - 북마크 추가/삭제
 *
 * TODO: MyParticipationService, BookmarkService 주입 및 구현
 */
@Slf4j
@Tag(name = "User - 활동 내역", description = "참여/작성/북마크 관리 API")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyParticipationController {

    // TODO: MyParticipationService, BookmarkService 주입
    // private final MyParticipationService myParticipationService;
    // private final BookmarkService bookmarkService;

    /**
     * 참여한 공동구매 목록 조회
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param pageable 페이지네이션 정보 (기본: 0페이지, 20개, 최신순)
     * @return 참여한 공동구매 목록
     */
    @Operation(
        summary = "참여한 공동구매 목록 조회",
        description = "사용자가 참여한 공동구매 목록을 페이지네이션하여 조회합니다."
    )
    @GetMapping("/participations")
    public ResponseEntity<ApiResponse<PageResponse<ParticipationItemResponse>>> getParticipations(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "페이지네이션 정보") Pageable pageable) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info(" 참여 목록 조회 요청 - 사용자 ID: {}, 페이지: {}", principal.id(), pageable.getPageNumber());

            // TODO: Service 호출로 교체
            // PageResponse<ParticipationItemResponse> response = myParticipationService.getParticipations(principal.id(), pageable);

            // 임시 응답
            PageResponse<ParticipationItemResponse> response = new PageResponse<>();

            log.info(" 참여 목록 조회 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error(" 참여 목록 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 내가 작성한 글 목록 조회
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param pageable 페이지네이션 정보 (기본: 0페이지, 20개, 최신순)
     * @return 작성한 글 목록
     */
    @Operation(
        summary = "내가 작성한 글 목록 조회",
        description = "사용자가 작성한 공동구매 글 목록을 페이지네이션하여 조회합니다."
    )
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PageResponse<MyPostItemResponse>>> getMyPosts(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "페이지네이션 정보") Pageable pageable) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info(" 작성한 글 목록 조회 요청 - 사용자 ID: {}, 페이지: {}", principal.id(), pageable.getPageNumber());

            // TODO: Service 호출로 교체
            // PageResponse<MyPostItemResponse> response = myParticipationService.getMyPosts(principal.id(), pageable);

            // 임시 응답
            PageResponse<MyPostItemResponse> response = new PageResponse<>();

            log.info(" 작성한 글 목록 조회 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error(" 작성한 글 목록 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 북마크한 글 목록 조회
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param pageable 페이지네이션 정보 (기본: 0페이지, 20개, 최신순)
     * @return 북마크한 글 목록
     */
    @Operation(
        summary = "북마크한 글 목록 조회",
        description = "사용자가 북마크한 공동구매 글 목록을 페이지네이션하여 조회합니다."
    )
    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<PageResponse<BookmarkItemResponse>>> getBookmarks(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "페이지네이션 정보") Pageable pageable) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info(" 북마크 목록 조회 요청 - 사용자 ID: {}, 페이지: {}", principal.id(), pageable.getPageNumber());

            // TODO: Service 호출로 교체
            // PageResponse<BookmarkItemResponse> response = bookmarkService.getBookmarks(principal.id(), pageable);

            // 임시 응답
            PageResponse<BookmarkItemResponse> response = new PageResponse<>();

            log.info(" 북마크 목록 조회 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error(" 북마크 목록 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 북마크 추가
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param postId 북마크할 게시글 ID
     * @return 성공 메시지
     */
    @Operation(
        summary = "북마크 추가",
        description = "특정 공동구매 글을 북마크에 추가합니다."
    )
    @PostMapping("/bookmarks/{postId}")
    public ResponseEntity<ApiResponse<String>> addBookmark(
            Authentication authentication,
            @PathVariable @Parameter(description = "게시글 ID") Long postId) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info(" 북마크 추가 요청 - 사용자 ID: {}, 게시글 ID: {}", principal.id(), postId);

            // TODO: Service 호출로 교체
            // bookmarkService.addBookmark(principal.id(), postId);

            log.info(" 북마크 추가 완료 - 사용자 ID: {}, 게시글 ID: {}", principal.id(), postId);

            return ResponseEntity.ok(ApiResponse.success("북마크가 추가되었습니다."));
        } catch (Exception e) {
            log.error(" 북마크 추가 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 북마크 삭제
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param postId 북마크 삭제할 게시글 ID
     * @return 성공 메시지
     */
    @Operation(
        summary = "북마크 삭제",
        description = "북마크에서 특정 공동구매 글을 제거합니다."
    )
    @DeleteMapping("/bookmarks/{postId}")
    public ResponseEntity<ApiResponse<String>> removeBookmark(
            Authentication authentication,
            @PathVariable @Parameter(description = "게시글 ID") Long postId) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info(" 북마크 삭제 요청 - 사용자 ID: {}, 게시글 ID: {}", principal.id(), postId);

            // TODO: Service 호출로 교체
            // bookmarkService.removeBookmark(principal.id(), postId);

            log.info(" 북마크 삭제 완료 - 사용자 ID: {}, 게시글 ID: {}", principal.id(), postId);

            return ResponseEntity.ok(ApiResponse.success("북마크가 삭제되었습니다."));
        } catch (Exception e) {
            log.error(" 북마크 삭제 중 오류 발생", e);
            throw e;
        }
    }
}

