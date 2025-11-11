package com.sobunsobun.backend.repository.user;

import com.sobunsobun.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 엔티티 데이터 접근 레포지토리
 *
 * Spring Data JPA를 사용하여 User 엔티티에 대한
 * 기본적인 CRUD 및 커스텀 쿼리 메서드를 제공합니다.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     * @param email 사용자 이메일
     * @return 사용자 정보 (Optional)
     */
    Optional<User> findByEmail(String email);


    /**
     * 이메일 존재 여부 확인
     * @param email 확인할 이메일
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByEmail(String email);

    /**
     * 닉네임 존재 여부 확인
     * @param nickname 확인할 닉네임
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByNickname(String nickname);

    /**
     * 특정 사용자를 제외한 닉네임 중복 확인
     * 자신의 닉네임을 변경할 때 다른 사용자와의 중복만 확인하기 위해 사용
     * @param nickname 확인할 닉네임
     * @param id 제외할 사용자 ID
     * @return 중복되면 true, 없으면 false
     */
    boolean existsByNicknameAndIdNot(String nickname, Long id);
}
