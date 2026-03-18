package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    /** 특정 신고자가 특정 게시글에서 특정 대상을 이미 신고했는지 확인 */
    boolean existsByReporterIdAndTargetUserIdAndGroupPostId(Long reporterId, Long targetUserId, Long groupPostId);

    /** 특정 사용자가 받은 신고 수 */
    long countByTargetUserId(Long targetUserId);
}
