package com.sobunsobun.backend.controller.auth;

import com.sobunsobun.backend.application.auth.AuthService;
import com.sobunsobun.backend.dto.auth.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;
    public AuthController(AuthService auth){ this.auth = auth; }

    /**
     * 카카오 동의 화면(Authorize)으로 이동할 URL을 생성해 반환.
     * - iOS는 이 URL을 열어 로그인/동의를 진행
     * - scope: 이메일/프로필 등 요청
     * - prompt=login: 로그인 화면 강제(이미 동의했을 때 화면이 안 뜨는 문제 방지)
     * - state: CSRF/리다이렉트 보호용 랜덤 문자열 (TODO: 서버에 저장 후 콜백에서 검증)
     */
    @GetMapping("/login/kakao")
    public ResponseEntity<Map<String, String>> kakaoLoginUrl() {
        // 환경변수 읽기 (application.yml -> ${KAKAO_CLIENT_ID}, ${KAKAO_REDIRECT_URI})
        String clientId   = System.getenv("KAKAO_CLIENT_ID");
        String redirectUri= System.getenv("KAKAO_REDIRECT_URI");

        // 안전장치: 환경변수 누락 시 500 대신 명확한 500 메시지
        if (clientId == null || clientId.isBlank() || redirectUri == null || redirectUri.isBlank()) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "kakao_config_missing",
                            "message", "KAKAO_CLIENT_ID 또는 KAKAO_REDIRECT_URI 환경변수가 비어 있습니다.")
            );
        }

        String scope  = "profile_nickname,profile_image,account_email";
        String prompt = "login";

        // (권장) state 값 생성 및 서버 저장(TTL) → 콜백에서 동일성 검증
        // String state = stateService.issue(); // TODO
        String state = null; // 샘플에선 생략

        // 파라미터 안전 조립 (자동 인코딩)
        String url = UriComponentsBuilder
                .fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("prompt", prompt)
                .queryParamIfPresent("state", java.util.Optional.ofNullable(state))
                .toUriString();

        return ResponseEntity.ok(Map.of("authorizeUrl", url));
    }

    /**
     * 카카오가 로그인/동의 완료 후 redirect_uri로 호출하는 콜백.
     * - 정상: ?code=... 로 들어옴 → 서비스가 토큰 교환/유저조회/가입or로그인/JWT발급 수행
     * - 실패/취소: ?error=..., error_description=... 으로 들어올 수 있음 → 클라이언트에 전달
     * - state 사용 시: 여기서 state 검증 필수 (재전송/위변조 방지)
     */
    @GetMapping("/callback/kakao")
    public ResponseEntity<?> kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @RequestParam(required = false) String state
    ){
        // 1) 에러/취소 케이스 우선 처리
        if (error != null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", error, "error_description", String.valueOf(errorDescription))
            );
        }

        // 2) state 검증 (사용한다면 필수)
        // if (!stateService.verify(state)) { return ResponseEntity.status(401).body(Map.of("error","invalid_state")); }

        // 3) code 누락 방어
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_code"));
        }

        // 4) 정상 플로우: code → (카카오)토큰 → (카카오)프로필 → (우리)upsert → (우리)JWT 발급
        AuthResponse tokens = auth.loginWithKakaoCode(code);
        return ResponseEntity.ok(tokens);
    }
}
