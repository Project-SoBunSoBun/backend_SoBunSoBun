package com.sobunsobun.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.application.auth.AuthService;
import com.sobunsobun.backend.infrastructure.oauth.OAuthProvider;
import com.sobunsobun.backend.infrastructure.oauth.OAuthState;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class OAuthCallbackAliasController {

    private final AuthService auth;
    private final ObjectMapper om;

    @Value("${app.login.page:/login}")
    private String loginPage;

    /** 카카오 콘솔 등록값: /auth/callback/kakao */
    @Operation(summary = "OAuth 콜백(별칭: /auth/callback/kakao)")
    @GetMapping("/callback/kakao")
    public ResponseEntity<Void> callbackAlias(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam String state,
            HttpServletResponse res
    ) {
        var st = OAuthState.decode(state, om);
        String returnTo = st.returnTo();

        if (error != null || !StringUtils.hasText(code)) {
            String fail = auth.buildFailureRedirect(loginPage, returnTo, error != null ? error : "missing_code");
            res.setHeader(HttpHeaders.LOCATION, fail);
            return ResponseEntity.status(HttpStatus.FOUND).build();
        }

        String authCode = auth.onCallbackSuccess(OAuthProvider.KAKAO, code);
        String success = auth.buildSuccessRedirect(returnTo, authCode);
        res.setHeader(HttpHeaders.LOCATION, success);
        return ResponseEntity.status(HttpStatus.FOUND).build();
    }
}
