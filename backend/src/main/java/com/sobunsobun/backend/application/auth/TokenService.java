package com.sobunsobun.backend.application.auth;

import com.sobunsobun.backend.domain.user.User;
import com.sobunsobun.backend.infrastructure.jwt.JwtTokenProvider;
import org.springframework.stereotype.Service;

/**
 * 애플리케이션(Service) 계층:
 * - "도메인 사용자"를 받아 Access/Refresh JWT를 발급
 * - 실제 서명 로직은 JwtTokenProvider(인프라 계층)에 위임
 */

@Service
public class TokenService {
    private final JwtTokenProvider jwt;
    public TokenService(JwtTokenProvider jwt){ this.jwt = jwt; }

    /** Access 토큰(짧은 만료) 발급: 매 요청 인증에 사용 */
    public String issueAccess(User u){ return jwt.createAccessToken(u.getId(), u.getRole().name()); }

    /** Refresh 토큰(긴 만료) 발급: 재발급 시나리오에 사용 */
    public String issueRefresh(User u){ return jwt.createRefreshToken(u.getId()); }
}
