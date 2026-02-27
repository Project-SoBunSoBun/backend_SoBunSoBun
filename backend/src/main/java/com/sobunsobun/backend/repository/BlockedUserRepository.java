package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // 차단 목록 조회용: blocked User를 fetch join해 N+1 방지
    @Query("SELECT b FROM BlockedUser b JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId")
    List<BlockedUser> findAllByBlockerIdWithBlocked(@Param("blockerId") Long blockerId);

    // 게시글/댓글 필터링용: 차단한 유저 ID 목록만 반환 (서브쿼리 대용)
    @Query("SELECT b.blocked.id FROM BlockedUser b WHERE b.blocker.id = :blockerId")
    List<Long> findBlockedIdsByBlockerId(@Param("blockerId") Long blockerId);
}
