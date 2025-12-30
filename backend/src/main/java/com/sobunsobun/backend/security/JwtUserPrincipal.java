package com.sobunsobun.backend.security;

import com.sobunsobun.backend.domain.Role;
import org.springframework.security.core.Authentication;

import java.security.Principal;

public record JwtUserPrincipal(Long id, Role role) {

    public static JwtUserPrincipal from(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        if (!(authentication.getPrincipal() instanceof JwtUserPrincipal jwtUser)) {
            throw new IllegalArgumentException("JwtUserPrincipal 타입이 아닙니다.");
        }

        return jwtUser;
    }
}


