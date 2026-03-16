package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.UserTagStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserTagStatsRepository extends JpaRepository<UserTagStats, Long> {

    /**
     * 특정 유저가 받은 태그를 카운트 높은 순으로 조회 (상위 5개)
     *
     * 프로필 조회 시 이 메서드를 사용합니다.
     * manner_review를 집계하는 대신 미리 계산된 통계 테이블을 읽으므로 성능 우수.
     */
    List<UserTagStats> findTop5ByReceiverIdOrderByCountDesc(Long receiverId);

    /**
     * 특정 유저가 받은 매너 평가 태그 총 횟수 합산
     */
    @Query("SELECT COALESCE(SUM(s.count), 0) FROM UserTagStats s WHERE s.receiverId = :receiverId")
    int sumCountByReceiverId(@Param("receiverId") Long receiverId);

    /**
     * 태그 카운트 UPSERT (Best Practice)
     *
     * [선택 이유]
     * - Dirty Checking: SELECT + UPDATE 2쿼리, 동시 요청 시 race condition 위험
     * - JPQL Bulk UPDATE: 행이 존재해야만 동작 (사전 INSERT 필요)
     * - 네이티브 UPSERT: 단 1쿼리, 원자적(atomic), race condition 없음 ✅
     *
     * UNIQUE(receiver_id, tag_code) 제약 조건을 기준으로 동작:
     * - 해당 (receiver_id, tag_code) 행이 없으면 count=1로 INSERT
     * - 이미 있으면 count를 +1 UPDATE
     *
     * @param receiverId 태그를 받은 사용자 ID
     * @param tagCode    태그 코드 문자열 (e.g., "TAG001")
     */
    @Modifying
    @Query(
        value = "INSERT INTO user_tag_stats (receiver_id, tag_code, count) " +
                "VALUES (:receiverId, :tagCode, 1) " +
                "ON DUPLICATE KEY UPDATE count = count + 1",
        nativeQuery = true
    )
    void upsertCount(@Param("receiverId") Long receiverId, @Param("tagCode") String tagCode);
}
