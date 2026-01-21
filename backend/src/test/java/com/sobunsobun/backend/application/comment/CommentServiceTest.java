package com.sobunsobun.backend.application.comment;

import com.sobunsobun.backend.domain.Comment;
import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.comment.CommentResponse;
import com.sobunsobun.backend.dto.comment.CreateCommentRequest;
import com.sobunsobun.backend.dto.comment.UpdateCommentRequest;
import com.sobunsobun.backend.repository.CommentRepository;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.support.exception.CommentException;
import com.sobunsobun.backend.support.exception.PostException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CommentService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 테스트")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private GroupPostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    private User testUser;
    private User otherUser;
    private GroupPost testPost;
    private Comment parentComment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .nickname("테스트유저")
            .build();

        otherUser = User.builder()
            .id(2L)
            .email("other@example.com")
            .nickname("다른유저")
            .build();

        testPost = GroupPost.builder()
            .id(1L)
            .title("테스트 게시글")
            .owner(testUser)
            .build();

        parentComment = Comment.builder()
            .id(1L)
            .post(testPost)
            .user(testUser)
            .content("부모 댓글")
            .deleted(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("댓글 생성 - 부모 댓글 생성 성공")
    void createComment_ParentComment_Success() {
        // Given
        CreateCommentRequest request = CreateCommentRequest.builder()
            .content("새로운 댓글")
            .parentCommentId(null)
            .build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(2L);
            return comment;
        });

        // When
        CommentResponse response = commentService.createComment(1L, request, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("새로운 댓글");
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        assertThat(response.getParentCommentId()).isNull();

        verify(postRepository, times(1)).findById(1L);
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 생성 - 대댓글 생성 성공")
    void createComment_ChildComment_Success() {
        // Given
        CreateCommentRequest request = CreateCommentRequest.builder()
            .content("대댓글")
            .parentCommentId(1L)
            .build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(3L);
            return comment;
        });

        // When
        CommentResponse response = commentService.createComment(1L, request, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("대댓글");
        assertThat(response.getParentCommentId()).isEqualTo(1L);

        verify(commentRepository, times(1)).findById(1L);
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 생성 - 게시글 없음 실패")
    void createComment_PostNotFound_Failure() {
        // Given
        CreateCommentRequest request = CreateCommentRequest.builder()
            .content("새로운 댓글")
            .build();

        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(999L, request, testUser))
            .isInstanceOf(PostException.class);
    }

    @Test
    @DisplayName("댓글 조회 - 게시글 댓글 목록 조회")
    void getCommentsByPostId_Success() {
        // Given
        when(postRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findActiveParentCommentsByPostId(1L))
            .thenReturn(List.of(parentComment));

        // When
        List<CommentResponse> responses = commentService.getCommentsByPostId(1L);

        // Then
        assertThat(responses).isNotEmpty();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getContent()).isEqualTo("부모 댓글");

        verify(postRepository, times(1)).existsById(1L);
        verify(commentRepository, times(1)).findActiveParentCommentsByPostId(1L);
    }

    @Test
    @DisplayName("댓글 조회 - 게시글 없음 실패")
    void getCommentsByPostId_PostNotFound_Failure() {
        // Given
        when(postRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> commentService.getCommentsByPostId(999L))
            .isInstanceOf(PostException.class);
    }

    @Test
    @DisplayName("댓글 수정 - 작성자 수정 성공")
    void updateComment_Success() {
        // Given
        UpdateCommentRequest request = UpdateCommentRequest.builder()
            .content("수정된 댓글 내용")
            .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(parentComment);

        // When
        CommentResponse response = commentService.updateComment(1L, request, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("수정된 댓글 내용");

        verify(commentRepository, times(1)).findById(1L);
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 수정 - 작성자가 아님 실패")
    void updateComment_NotAuthor_Failure() {
        // Given
        UpdateCommentRequest request = UpdateCommentRequest.builder()
            .content("수정된 내용")
            .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(parentComment));

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(1L, request, otherUser))
            .isInstanceOf(CommentException.class)
            .hasMessageContaining("권한");
    }

    @Test
    @DisplayName("댓글 수정 - 삭제된 댓글 실패")
    void updateComment_AlreadyDeleted_Failure() {
        // Given
        parentComment.setDeleted(true);
        UpdateCommentRequest request = UpdateCommentRequest.builder()
            .content("수정된 내용")
            .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(parentComment));

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(1L, request, testUser))
            .isInstanceOf(CommentException.class)
            .hasMessageContaining("삭제");
    }

    @Test
    @DisplayName("댓글 삭제 - 작성자 삭제 성공")
    void deleteComment_Success() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(parentComment);

        // When
        commentService.deleteComment(1L, testUser);

        // Then
        assertThat(parentComment.getDeleted()).isTrue();
        verify(commentRepository, times(1)).findById(1L);
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 삭제 - 작성자가 아님 실패")
    void deleteComment_NotAuthor_Failure() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parentComment));

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(1L, otherUser))
            .isInstanceOf(CommentException.class)
            .hasMessageContaining("권한");
    }

    @Test
    @DisplayName("댓글 삭제 - 이미 삭제된 댓글 실패")
    void deleteComment_AlreadyDeleted_Failure() {
        // Given
        parentComment.setDeleted(true);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parentComment));

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(1L, testUser))
            .isInstanceOf(CommentException.class)
            .hasMessageContaining("삭제");
    }

    @Test
    @DisplayName("댓글 개수 조회 - 성공")
    void getCommentCountByPostId_Success() {
        // Given
        when(commentRepository.countActiveCommentsByPostId(1L)).thenReturn(5L);

        // When
        long count = commentService.getCommentCountByPostId(1L);

        // Then
        assertThat(count).isEqualTo(5L);
        verify(commentRepository, times(1)).countActiveCommentsByPostId(1L);
    }
}

