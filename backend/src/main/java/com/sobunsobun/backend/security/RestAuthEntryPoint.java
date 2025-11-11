package com.sobunsobun.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.Map;

/**
 * 인증 실패 시 처리 핸들러
 * 401 Unauthorized 에러를 JSON 형태로 반환
 */
public class RestAuthEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper om = new ObjectMapper();
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8"); // UTF-8 인코딩 설정
        om.writeValue(response.getWriter(),
                Map.of("error","unauthorized","message","인증이 필요합니다."));
    }
}
