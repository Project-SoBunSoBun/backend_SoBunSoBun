package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.MannerReview;
import com.sobunsobun.backend.domain.MannerTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MannerReviewRepository extends JpaRepository<MannerReview, Long> {

    /**
     * 동일 거래에서 특정 태그로 이미 평가했는지 중복 체크
     *
     * @param senderId    평가자 ID
     * @param receiverId  피평가자 ID
     * @param groupPostId 거래 게시글 ID
     * @param tagCode     태그 코드
     */
    boolean existsBySenderIdAndReceiverIdAndGroupPostIdAndTagCode(
        Long senderId, Long receiverId, Long groupPostId, MannerTag tagCode
    );
}
