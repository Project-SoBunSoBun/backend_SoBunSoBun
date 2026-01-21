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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 댓글 비즈니스 로직 서비스
 * 댓글 생성, 조회, 수정, 삭제 기능을 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final GroupPostRepository postRepository;

    /**
     * 댓글 생성
     *
     * 부모 댓글이 없으면 새로운 댓글 생성
     * 부모 댓글이 있으면 대댓글 생성 (1 depth만 허용)
     * 부모 댓글이 삭제되어도 대댓글은 생성 가능 (고아 대댓글)
     *
     * @param postId 게시글 ID
     * @param request 댓글 생성 요청
     * @param user 현재 로그인 사용자
     * @return 생성된 댓글 정보
     * @throws PostException 게시글을 찾을 수 없는 경우
     * @throws CommentException 부모 댓글을 찾을 수 없거나 잘못된 요청인 경우
     */
    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest request, User user) {
        log.info("댓글 생성 시작 - postId: {}, userId: {}", postId, user.getId());

        // 게시글 조회
        GroupPost post = postRepository.findById(postId)
            .orElseThrow(PostException::notFound);

        Comment parentComment = null;

        // 대댓글인 경우 부모 댓글 조회
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                .orElseThrow(CommentException::notFound);

            // 부모 댓글이 같은 게시글의 댓글인지 확인
            if (!parentComment.getPost().getId().equals(postId)) {
                throw CommentException.badRequest("부모 댓글이 같은 게시글의 댓글이 아닙니다.");
            }

            // 대댓글의 대댓글은 불가능 (1 depth만 허용)
            if (parentComment.getParentComment() != null) {
                throw CommentException.badRequest("대댓글에 대한 대댓글은 작성할 수 없습니다.");
            }
        }

        // 댓글 생성
        Comment comment = Comment.builder()
            .post(post)
            .user(user)
            .parentComment(parentComment)
            .content(request.getContent())
            .deleted(false)
            .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("댓글 생성 완료 - commentId: {}", savedComment.getId());

        return CommentResponse.from(savedComment);
    }

    /**
     * 게시글의 모든 활성 댓글 조회
     * 부모 댓글 → 대댓글 구조로 반환
     * 최신순 정렬 (부모 댓글 기준)
     *
     * @param postId 게시글 ID
     * @return 댓글 목록 (트리 구조)
     * @throws PostException 게시글을 찾을 수 없는 경우
     */
    public List<CommentResponse> getCommentsByPostId(Long postId) {
        log.debug("게시글의 댓글 조회 - postId: {}", postId);

        // 게시글 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw PostException.notFound();
        }

        // 활성 부모 댓글 조회 (최신순)
        List<Comment> parentComments = commentRepository.findActiveParentCommentsByPostId(postId);

        // 부모 댓글과 대댓글을 함께 DTO로 변환
        return parentComments.stream()
            .map(CommentResponse::fromWithChildren)
            .collect(Collectors.toList());
    }

    /**
     * 단일 댓글 조회
     *
     * @param commentId 댓글 ID
     * @return 댓글 정보
     * @throws CommentException 댓글을 찾을 수 없는 경우
     */
    public CommentResponse getCommentById(Long commentId) {
        log.debug("댓글 조회 - commentId: {}", commentId);

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(CommentException::notFound);

        return CommentResponse.from(comment);
    }

    /**
     * 댓글 수정
     * 작성자 본인만 수정 가능
     *
     * @param commentId 댓글 ID
     * @param request 수정 요청
     * @param user 현재 로그인 사용자
     * @return 수정된 댓글 정보
     * @throws CommentException 댓글을 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, User user) {
        log.info("댓글 수정 시작 - commentId: {}, userId: {}", commentId, user.getId());

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(CommentException::notFound);

        // 삭제된 댓글은 수정 불가
        if (comment.getDeleted()) {
            throw CommentException.alreadyDeleted();
        }

        // 권한 체크 (작성자만 수정 가능)
        if (!comment.isAuthor(user)) {
            throw CommentException.forbidden();
        }

        // 내용 수정
        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);

        log.info("댓글 수정 완료 - commentId: {}", updatedComment.getId());

        return CommentResponse.from(updatedComment);
    }

    /**
     * 댓글 삭제 (Soft Delete)
     * 작성자 본인만 삭제 가능
     *
     * 부모 댓글 삭제:
     * - deleted = true로 표시
     * - 대댓글은 유지됨 (고아 대댓글)
     *
     * 대댓글 삭제:
     * - deleted = true로 표시
     * - 부모 댓글에는 영향 없음
     *
     * @param commentId 댓글 ID
     * @param user 현재 로그인 사용자
     * @throws CommentException 댓글을 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public void deleteComment(Long commentId, User user) {
        log.info("댓글 삭제 시작 - commentId: {}, userId: {}", commentId, user.getId());

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(CommentException::notFound);

        // 이미 삭제된 댓글
        if (comment.getDeleted()) {
            throw CommentException.alreadyDeleted();
        }

        // 권한 체크 (작성자만 삭제 가능)
        if (!comment.isAuthor(user)) {
            throw CommentException.forbidden();
        }

        // Soft Delete 처리
        comment.setDeleted(true);
        commentRepository.save(comment);

        log.info("댓글 삭제 완료 - commentId: {}", commentId);
    }

    /**
     * 게시글의 활성 댓글 개수 조회
     *
     * @param postId 게시글 ID
     * @return 댓글 개수
     */
    public long getCommentCountByPostId(Long postId) {
        return commentRepository.countActiveCommentsByPostId(postId);
    }

    /**
     * 사용자가 작성한 활성 댓글 개수
     *
     * @param userId 사용자 ID
     * @return 댓글 개수
     */
    public long getCommentCountByUserId(Long userId) {
        return commentRepository.countActiveCommentsByUserId(userId);
    }
}

