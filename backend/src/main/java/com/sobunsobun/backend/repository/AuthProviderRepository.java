package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * OAuth 인증 제공자 정보 리포지토리
 */
@Repository
public interface AuthProviderRepository extends JpaRepository<AuthProvider, Long> {

    /**
     * OAuth 제공자와 제공자 사용자 ID로 조회
     * @param provider OAuth 제공자 (KAKAO, APPLE 등)
     * @param providerUserId OAuth 제공자의 사용자 고유 ID
     * @return AuthProvider
     */
    Optional<AuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);

    /**
     * 사용자 ID와 OAuth 제공자로 조회
     * @param userId 사용자 ID
     * @param provider OAuth 제공자
     * @return AuthProvider
     */
    Optional<AuthProvider> findByUserIdAndProvider(Long userId, String provider);
}

