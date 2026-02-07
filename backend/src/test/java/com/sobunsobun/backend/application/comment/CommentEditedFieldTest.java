package com.sobunsobun.backend.application.comment;

import com.sobunsobun.backend.domain.Comment;
import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.comment.CommentResponse;
import com.sobunsobun.backend.dto.comment.CreateCommentRequest;
import com.sobunsobun.backend.dto.comment.UpdateCommentRequest;
import com.sobunsobun.backend.repository.CommentRepository;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.CommentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comment edited 필드 통합 테스트
 *
 * 테스트 시나리오:
 * - 정책 1: 댓글 생성 시 deleted=false, edited=false
 * - 정책 2: 댓글 수정 시 deleted=false인 경우만 수정, edited=true로 변경
 * - 정책 3: 댓글 삭제 시 deleted=true, edited=false로 강제, content 변경
 * - 정책 4: 삭제된 댓글 수정 불가
 * - 정책 5: 수정 후 삭제된 댓글의 최종 상태는 deleted=true, edited=false
 */
@SpringBootTest
@Transactional
@DisplayName("Comment edited 필드 통합 테스트")
class CommentEditedFieldTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private GroupPostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User otherUser;
    private GroupPost testPost;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
            .username("testuser")
            .nickname("test_user")
            .email("test@example.com")
            .build();
        userRepository.save(testUser);

        otherUser = User.builder()
            .username("otheruser")
            .nickname("other_user")
            .email("other@example.com")
            .build();
        userRepository.save(otherUser);

        // 테스트 게시글 생성
        testPost = GroupPost.builder()
            .user(testUser)
            .title("Test Post")
            .content("Test content")
            .build();
        postRepository.save(testPost);
    }

    // =========================================================
    // 정책 1: 댓글 생성 시 deleted=false, edited=false
    // =========================================================

    @Test
    @DisplayName("정책 1: 댓글 생성 시 deleted=false, edited=false")
    void testCreateComment_InitialState() {
        // Given
        CreateCommentRequest request = new CreateCommentRequest("새로운 댓글입니다!");

        // When
        CommentResponse response = commentService.createComment(testPost.getId(), request, testUser);

        // Then
        assertNotNull(response.getId());
        assertEquals("새로운 댓글입니다!", response.getContent());
        assertEquals(false, response.getDeleted(), "deleted는 false여야 함");
        assertEquals(false, response.getEdited(), "edited는 false여야 함");
    }

    @Test
    @DisplayName("정책 1: 대댓글 생성도 deleted=false, edited=false")
    void testCreateReply_InitialState() {
        // Given: 부모 댓글 생성
        CreateCommentRequest parentRequest = new CreateCommentRequest("부모 댓글");
        CommentResponse parentResponse = commentService.createComment(testPost.getId(), parentRequest, testUser);

        // When: 대댓글 생성
        CreateCommentRequest replyRequest = new CreateCommentRequest("대댓글입니다!");
        replyRequest.setParentCommentId(parentResponse.getId());
        CommentResponse replyResponse = commentService.createComment(testPost.getId(), replyRequest, testUser);

        // Then
        assertEquals(false, replyResponse.getDeleted());
        assertEquals(false, replyResponse.getEdited());
        assertEquals(parentResponse.getId(), replyResponse.getParentCommentId());
    }

    // =========================================================
    // 정책 2: 댓글 수정 시 edited=true로 변경
    // =========================================================

    @Test
    @DisplayName("정책 2: 댓글 수정 시 edited=false → true 변경")
    void testUpdateComment_EditedFlagChange() {
        // Given: 댓글 생성
        CreateCommentRequest createRequest = new CreateCommentRequest("원본 내용");
        CommentResponse createdResponse = commentService.createComment(testPost.getId(), createRequest, testUser);
        Long commentId = createdResponse.getId();

        // 생성 직후 상태 확인
        assertEquals(false, createdResponse.getEdited());

        // When: 댓글 수정
        UpdateCommentRequest updateRequest = new UpdateCommentRequest("수정된 내용");
        CommentResponse updatedResponse = commentService.updateComment(commentId, updateRequest, testUser);

        // Then
        assertEquals("수정된 내용", updatedResponse.getContent());
        assertEquals(false, updatedResponse.getDeleted());
        assertEquals(true, updatedResponse.getEdited(), "edited는 true여야 함");
    }

    @Test
    @DisplayName("정책 2: 여러 번 수정해도 edited는 true 유지")
    void testUpdateComment_MultipleEdits() {
        // Given
        CreateCommentRequest createRequest = new CreateCommentRequest("원본");
        CommentResponse response1 = commentService.createComment(testPost.getId(), createRequest, testUser);
        Long commentId = response1.getId();

        // When: 1차 수정
        UpdateCommentRequest update1 = new UpdateCommentRequest("수정 1");
        CommentResponse response2 = commentService.updateComment(commentId, update1, testUser);

        // Then: 1차 수정 후
        assertEquals(true, response2.getEdited());

        // When: 2차 수정
        UpdateCommentRequest update2 = new UpdateCommentRequest("수정 2");
        CommentResponse response3 = commentService.updateComment(commentId, update2, testUser);

        // Then: 2차 수정 후
        assertEquals("수정 2", response3.getContent());
        assertEquals(true, response3.getEdited(), "edited는 여전히 true");
    }

    @Test
    @DisplayName("정책 2: 수정 시 updatedAt 자동 갱신")
    void testUpdateComment_UpdatedAtRefresh() throws InterruptedException {
        // Given
        CreateCommentRequest createRequest = new CreateCommentRequest("원본");
        CommentResponse createdResponse = commentService.createComment(testPost.getId(), createRequest, testUser);
        Long commentId = createdResponse.getId();

        var createdAtTime = createdResponse.getCreatedAt();
        var updatedAtTime1 = createdResponse.getUpdatedAt();

        // 시간 차이 생성
        Thread.sleep(100);

        // When: 수정
        UpdateCommentRequest updateRequest = new UpdateCommentRequest("수정된 내용");
        CommentResponse updatedResponse = commentService.updateComment(commentId, updateRequest, testUser);

        // Then
        var updatedAtTime2 = updatedResponse.getUpdatedAt();

        assertTrue(updatedAtTime2.isAfter(updatedAtTime1), "updatedAt이 변경되어야 함");
        assertEquals(createdAtTime, updatedResponse.getCreatedAt(), "createdAt은 변경되지 않아야 함");
    }

    // =========================================================
    // 정책 3: 댓글 삭제 시 deleted=true, edited=false로 강제
    // =========================================================

    @Test
    @DisplayName("정책 3: 댓글 삭제 시 deleted=true, edited=false로 강제")
    void testDeleteComment_StateChange() {
        // Given: 댓글 생성
        CreateCommentRequest createRequest = new CreateCommentRequest("삭제할 댓글");
        CommentResponse createdResponse = commentService.createComment(testPost.getId(), createRequest, testUser);
        Long commentId = createdResponse.getId();

        // When: 댓글 삭제
        commentService.deleteComment(commentId, testUser);

        // Then
        Comment deletedComment = commentRepository.findById(commentId)
            .orElseThrow(() -> new AssertionError("댓글을 찾을 수 없음"));

        assertEquals(true, deletedComment.getDeleted(), "deleted는 true여야 함");
        assertEquals(false, deletedComment.getEdited(), "edited는 false로 강제되어야 함");
        assertEquals("삭제된 댓글입니다", deletedComment.getContent(), "content는 변경되어야 함");
    }

    @Test
    @DisplayName("정책 3: 수정된 댓글 삭제 시에도 edited=false로 강제")
    void testDeleteEditedComment_EditedReset() {
        // Given: 댓글 생성 → 수정
        CreateCommentRequest createRequest = new CreateCommentRequest("원본");
        CommentResponse createdResponse = commentService.createComment(testPost.getId(), createRequest, testUser);
        Long commentId = createdResponse.getId();

        UpdateCommentRequest updateRequest = new UpdateCommentRequest("수정됨");
        CommentResponse updatedResponse = commentService.updateComment(commentId, updateRequest, testUser);

        // 수정 후 상태 확인
        assertEquals(true, updatedResponse.getEdited());

        // When: 삭제
        commentService.deleteComment(commentId, testUser);

        // Then
        Comment deletedComment = commentRepository.findById(commentId)
            .orElseThrow(() -> new AssertionError("댓글을 찾을 수 없음"));

        assertEquals(true, deletedComment.getDeleted());
        assertEquals(false, deletedComment.getEdited(), "edited는 false로 리셋되어야 함 (정책 5)");
    }

    // =========================================================
    // 정책 4: 삭제된 댓글 수정 불가
    // =========================================================

    @Test
    @DisplayName("정책 4: 삭제된 댓글은 수정 불가")
    void testCannotUpdateDeletedComment() {
        // Given: 댓글 생성 후 삭제
        CreateCommentRequest createRequest = new CreateCommentRequest("삭제할 댓글");
        CommentResponse createdResponse = commentService.createComment(testPost.getId(), createRequest, testUser);
        Long commentId = createdResponse.getId();

        commentService.deleteComment(commentId, testUser);

        // When & Then: 수정 시도 → Exception 발생
        UpdateCommentRequest updateRequest = new UpdateCommentRequest("수정 시도");

        assertThrows(CommentException.class, () -> {
            commentService.updateComment(commentId, updateRequest, testUser);
        }, "삭제된 댓글은 수정할 수 없어야 함");
    }

    @Test
    @DisplayName("정책 4: 삭제된 댓글 재삭제 불가")
    void testCannotDeleteAlreadyDeletedComment() {
        // Given: 댓글 생성 후 삭제
        CreateCommentRequest createRequest = new CreateCommentRequest("삭제할 댓글");
        CommentResponse createdResponse = commentService.createComment(testPost.getId(), createRequest, testUser);
        Long commentId = createdResponse.getId();

        commentService.deleteComment(commentId, testUser);

        // When & Then: 재삭제 시도 → Exception 발생
        assertThrows(CommentException.class, () -> {
            commentService.deleteComment(commentId, testUser);
        }, "이미 삭제된 댓글은 재삭제할 수 없어야 함");
    }

    // =========================================================
    // 권한 검증
    // =========================================================

    @Test
    @DisplayName("권한 검증: 다른 사용자는 수정 불가")
    void testCannotUpdateOtherUserComment() {
        // Given: testUser가 댓글 작성
        CreateCommentRequest createRequest = new CreateCommentRequest("테스트 댓글");
        CommentResponse createdResponse = commentService.createComment(testPost.getId(), createRequest, testUser);
        Long commentId = createdResponse.getId();

        // When & Then: otherUser가 수정 시도 → Exception 발생
        UpdateCommentRequest updateRequest = new UpdateCommentRequest("다른 사용자의 수정");

        assertThrows(CommentException.class, () -> {
            commentService.updateComment(commentId, updateRequest, otherUser);
        }, "다른 사용자는 댓글을 수정할 수 없어야 함");
    }

    @Test
    @DisplayName("권한 검증: 다른 사용자는 삭제 불가")
    void testCannotDeleteOtherUserComment() {
        // Given: testUser가 댓글 작성
        CreateCommentRequest createRequest = new CreateCommentRequest("테스트 댓글");
        CommentResponse createdResponse = commentService.createComment(testPost.getId(), createRequest, testUser);
        Long commentId = createdResponse.getId();

        // When & Then: otherUser가 삭제 시도 → Exception 발생
        assertThrows(CommentException.class, () -> {
            commentService.deleteComment(commentId, otherUser);
        }, "다른 사용자는 댓글을 삭제할 수 없어야 함");
    }

    // =========================================================
    // 정책 5: 수정 후 삭제 시나리오
    // =========================================================

    @Test
    @DisplayName("정책 5: 수정 후 삭제된 댓글의 최종 상태")
    void testPolicy5_DeleteAfterUpdate() {
        // Given: 댓글 생성 → 수정 → 삭제의 전체 생명주기

        // Step 1: 생성
        CreateCommentRequest createRequest = new CreateCommentRequest("좋은 게시글입니다!");
        CommentResponse createdResponse = commentService.createComment(testPost.getId(), createRequest, testUser);
        Long commentId = createdResponse.getId();

        assertEquals(false, createdResponse.getDeleted());
        assertEquals(false, createdResponse.getEdited());

        // Step 2: 수정
        UpdateCommentRequest updateRequest = new UpdateCommentRequest("정말 좋은 게시글입니다!");
        CommentResponse updatedResponse = commentService.updateComment(commentId, updateRequest, testUser);

        assertEquals(false, updatedResponse.getDeleted());
        assertEquals(true, updatedResponse.getEdited());

        // Step 3: 삭제
        commentService.deleteComment(commentId, testUser);

        // Then: 최종 상태 확인 (정책 5)
        Comment finalComment = commentRepository.findById(commentId)
            .orElseThrow(() -> new AssertionError("댓글을 찾을 수 없음"));

        assertEquals(true, finalComment.getDeleted(), "deleted = true (우선)");
        assertEquals(false, finalComment.getEdited(), "edited = false (강제)");
        assertEquals("삭제된 댓글입니다", finalComment.getContent());
    }

    // =========================================================
    // DTO 매핑 검증
    // =========================================================

    @Test
    @DisplayName("DTO 매핑: edited 필드가 올바르게 반환됨")
    void testDtoMapping_EditedField() {
        // Given: 댓글 생성
        CreateCommentRequest createRequest = new CreateCommentRequest("원본");
        CommentResponse response1 = commentService.createComment(testPost.getId(), createRequest, testUser);

        // Then: 생성 직후
        assertNotNull(response1.getEdited());
        assertEquals(false, response1.getEdited());

        // When: 수정
        UpdateCommentRequest updateRequest = new UpdateCommentRequest("수정");
        CommentResponse response2 = commentService.updateComment(response1.getId(), updateRequest, testUser);

        // Then: 수정 후
        assertNotNull(response2.getEdited());
        assertEquals(true, response2.getEdited());
    }
}
