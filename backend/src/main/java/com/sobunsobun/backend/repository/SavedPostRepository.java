package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.SavedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    /**
     * 사용자가 특정 게시글을 저장했는지 확인
     */
    Optional<SavedPost> findByUserIdAndPostId(Long userId, Long postId);

    /**
     * 사용자의 저장된 게시글 목록 (페이징)
     */
    Page<SavedPost> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 저장된 게시글 개수
     */
    long countByUserId(Long userId);

    /**
     * 특정 게시글이 저장된 횟수
     */
    long countByPostId(Long postId);

    /**
     * 사용자가 저장한 게시글 삭제
     */
    void deleteByUserIdAndPostId(Long userId, Long postId);

    /**
     * 특정 사용자의 모든 저장된 게시글 삭제 (회원탈퇴용)
     */
    void deleteByUserId(Long userId);
}
