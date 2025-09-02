package com.sobunsobun.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.application.auth.AuthService;
import com.sobunsobun.backend.dto.auth.AuthResponse;
import com.sobunsobun.backend.infrastructure.oauth.OAuthProvider;
import com.sobunsobun.backend.infrastructure.oauth.OAuthState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth - OAuth (Kakao/Google/Apple)")
@RestController
@RequestMapping("/auth/login")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final ObjectMapper om;

    @Value("${app.login.page:/login}") private String loginPage;

    /** 시작: GET /auth/login/{provider}?returnTo=...&platform=web|ios */
    @Operation(summary = "OAuth 로그인 시작(리다이렉트)")
    @GetMapping("/{provider}")
    public ResponseEntity<Void> start(
            @PathVariable String provider,
            @RequestParam String returnTo,
            @RequestParam(defaultValue = "web") String platform,
            HttpServletResponse res
    ){
        String authorize = auth.createAuthorizeUrl(OAuthProvider.from(provider), returnTo, platform);
        res.setHeader(HttpHeaders.LOCATION, authorize);
        return ResponseEntity.status(HttpStatus.FOUND).build();
    }

    /** 콜백: GET /auth/login/{provider}/callback?code=...&state=... (실패시 error=...) */
    @Operation(summary = "OAuth 콜백")
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam String state,
            HttpServletResponse res
    ){
        var st = OAuthState.decode(state, om);
        String returnTo = st.returnTo();

        if (error != null || !StringUtils.hasText(code)) {
            String fail = auth.buildFailureRedirect(loginPage, returnTo, error!=null?error:"missing_code");
            res.setHeader(HttpHeaders.LOCATION, fail);
            return ResponseEntity.status(HttpStatus.FOUND).build();
        }

        String authCode = auth.onCallbackSuccess(OAuthProvider.from(provider), code);
        String success = auth.buildSuccessRedirect(returnTo, authCode);
        res.setHeader(HttpHeaders.LOCATION, success);
        return ResponseEntity.status(HttpStatus.FOUND).build();
    }

    /** 최종 교환: POST /auth/login/{provider}/finalize  { "code": "AUTH_CODE" } */
    @Operation(summary = "1회용 코드 → JWT 교환(finalize)")
    @PostMapping("/{provider}/finalize")
    public ResponseEntity<AuthResponse> finalizeLogin(
            @PathVariable String provider,
            @RequestBody Map<String,String> body
    ){
        String code = body.get("code");
        if (!StringUtils.hasText(code)) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(auth.exchangeAuthCode(code));
    }
}
