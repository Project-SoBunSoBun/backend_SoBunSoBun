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

    /**
     * 특정 거래에서 해당 사용자가 리뷰를 하나라도 남겼는지 확인
     *
     * @param senderId    리뷰 작성자 ID
     * @param groupPostId 거래 게시글 ID
     */
    boolean existsBySenderIdAndGroupPostId(Long senderId, Long groupPostId);

    /**
     * 특정 거래에서 특정 사용자가 특정 상대에게 리뷰를 남겼는지 확인
     *
     * @param senderId    리뷰 작성자 ID
     * @param receiverId  피평가자 ID
     * @param groupPostId 거래 게시글 ID
     */
    boolean existsBySenderIdAndReceiverIdAndGroupPostId(Long senderId, Long receiverId, Long groupPostId);
}
