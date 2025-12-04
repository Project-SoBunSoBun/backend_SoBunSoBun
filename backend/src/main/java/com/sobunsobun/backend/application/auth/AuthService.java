package com.sobunsobun.backend.application.auth;

import com.sobunsobun.backend.domain.AuthProvider;
import com.sobunsobun.backend.domain.Role;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.UserStatus;
import com.sobunsobun.backend.dto.auth.*;
import com.sobunsobun.backend.infrastructure.oauth.KakaoOAuthClient;
import com.sobunsobun.backend.repository.AuthProviderRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 인증/인가 비즈니스 로직 서비스
 *
 * 담당 기능:
 * - 카카오 OAuth 로그인/회원가입 처리
 * - JWT 액세스/리프레시 토큰 발급 및 갱신
 * - 이용약관 동의 기반 회원가입 플로우
 * - 토큰 생명주기 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private final Environment env;

    /** JWT 토큰 만료 시간 상수 */
    private static final long ACCESS_TOKEN_TTL = 30L * 60 * 1000;           // 액세스 토큰: 30분
    private static final long REFRESH_TOKEN_TTL = 60L * 24 * 60 * 60 * 1000; // 리프레시 토큰: 60일
    private static final long LOGIN_TOKEN_TTL = 10L * 60 * 1000;             // 임시 로그인 토큰: 10분

    /** 한국 시간대 및 포맷터 */
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KST_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").withZone(KST_ZONE);

    /**
     * 1단계: 카카오 토큰 검증 및 사용자 정보 확인
     *
     * 플로우:
     * 1. 카카오 API로 사용자 정보 조회
     * 2. 이메일 동의 여부 확인
     * 3. 기존 사용자 여부 판단
     * 4. 임시 로그인 토큰 발급 (이용약관 동의용)
     *
     * @param kakaoAccessToken 카카오에서 발급받은 액세스 토큰
     * @return 사용자 정보와 임시 로그인 토큰
     * @throws ResponseStatusException 카카오 API 오류, 이메일 동의 미완료 시
     */
    public KakaoVerifyResponse verifyKakaoToken(String kakaoAccessToken) {
        log.info("[사용자 작동] 카카오 로그인 시도");

        // 1. 카카오 API 호출하여 사용자 정보 조회
        var kakaoUser = kakaoOAuthClient.getUserInfo(kakaoAccessToken).block();
        if (kakaoUser == null) {
            log.error("카카오 사용자 정보 조회 실패");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 사용자 조회 실패");
        }

        // 2. 카카오 사용자 정보 추출
        String oauthId = String.valueOf(kakaoUser.getId());
        String email = String.valueOf(kakaoUser.getKakao_account().getEmail());

        log.info("[사용자 작동] 카카오 로그인 정보 확인 - 이메일: {}", email);

        // 3. 이메일 동의 필수 확인
        if (email == null || email.isBlank()) {
            log.warn("카카오 이메일 동의 미완료");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 이메일 동의가 필요합니다.");
        }

        // 4. 기존 사용자 여부 확인 (카카오 OAuth ID로 조회)
        boolean isNewUser = !authProviderRepository.findByProviderAndProviderUserId("KAKAO", oauthId).isPresent();
        log.info("사용자 상태 확인 - 신규 사용자: {}", isNewUser);

        // 5. 임시 로그인 토큰 발급 (이용약관 동의용, 10분 만료)
        String loginToken = jwtTokenProvider.createLoginToken(email, oauthId, LOGIN_TOKEN_TTL);
        log.info("임시 로그인 토큰 발급 완료");

        return KakaoVerifyResponse.builder()
                .email(email)
                .loginToken(loginToken)
                .isNewUser(isNewUser)
                .build();
    }

    /**
     * 2단계: 이용약관 동의 후 회원가입/로그인 완료
     *
     * 플로우:
     * 1. 임시 로그인 토큰 검증
     * 2. 필수 약관 동의 여부 확인
     * 3. 사용자 등록/업데이트
     * 4. JWT 액세스/리프레시 토큰 발급
     *
     * @param request 약관 동의 정보 및 임시 토큰
     * @return JWT 토큰 4개 정보 (액세스, 리프레시, 만료시간 2개)
     * @throws ResponseStatusException 토큰 무효, 약관 미동의 시
     */
    @Transactional
    public AuthResponse completeSignupWithTerms(TermsAgreementRequest request) {
        //백엔드 어드민 계정 로그인 (백도어)
        String adminLogin = env.getProperty("ADMIN_TOKEN");
        String adminLogin2 = env.getProperty("ADMIN2_TOKEN");
        String providedToken = request.getLoginToken();

        if (adminLogin != null && adminLogin.equals(providedToken)) {
            log.info("[관리자 작동] 어드민 로그인 시도");

            try {
                String email = env.getProperty("ADMIN_EMAIL");
                String oauthId = env.getProperty("ADMIN_OAUTH_ID");

                // 3. 사용자 등록 또는 업데이트
                User user = findOrCreateUser(email, oauthId);
                log.info("[관리자 작동] 어드민 로그인 성공 - 사용자 ID: {}", user.getId());

                // 4. JWT 토큰 발급
                return generateJwtTokenResponse(user);

            } catch (Exception e) {
                if (e instanceof ResponseStatusException) {
                    throw e;
                }
                log.error("어드민 로그인 처리 중 오류 발생 {}: {}", e.getClass().getSimpleName(), e.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 토큰이 유효하지 않습니다.");
            }

        } else if (adminLogin2 != null && adminLogin2.equals(providedToken)) {
            log.info("[관리자2 작동] 어드민2 로그인 시도");

            try {
                String email = env.getProperty("ADMIN2_EMAIL");
                String oauthId = env.getProperty("ADMIN2_OAUTH_ID");

                // 3. 사용자 등록 또는 업데이트
                User user = findOrCreateUser(email, oauthId);
                log.info("[관리자2 작동] 어드민2 로그인 성공 - 사용자 ID: {}", user.getId());

                // 4. JWT 토큰 발급
                return generateJwtTokenResponse(user);

            } catch (Exception e) {
                if (e instanceof ResponseStatusException) {
                    throw e;
                }
                log.error("어드민2 로그인 처리 중 오류 발생 {}: {}", e.getClass().getSimpleName(), e.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 토큰이 유효하지 않습니다.");
            }
        } else {
            log.info("[사용자 작동] 회원가입 이용약관 동의 처리 시작");

            try {
                // 1. 임시 로그인 토큰 검증 및 정보 추출
                Claims claims = jwtTokenProvider.parse(request.getLoginToken()).getBody();
                String email = claims.get("email", String.class);
                String oauthId = claims.get("oauthId", String.class);

                if (email == null || oauthId == null) {
                    log.debug("임시 로그인 토큰에서 정보 추출 실패");
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 로그인 토큰입니다.");
                }

                log.info("임시 토큰 검증 완료 - 이메일: {}", email);

                // 2. 필수 약관 동의 여부 확인
                validateTermsAgreement(request);

                // 3. 사용자 등록 또는 업데이트
                User user = findOrCreateUser(email, oauthId);
                log.info("[사용자 작동] 회원가입 완료 - 사용자 ID: {}, 이메일: {}", user.getId(), email);

                // 4. JWT 토큰 발급
                return generateJwtTokenResponse(user);

            } catch (JwtException e) {
                // JWT 관련 예외는 간단한 로그만 기록
                log.error("회원가입 완료 처리 중 오류 발생 {}: {}", e.getClass().getSimpleName(), e.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 로그인 토큰입니다.");
            } catch (Exception e) {
                if (e instanceof ResponseStatusException) {
                    throw e;
                }
                log.error("회원가입 완료 처리 중 오류 발생 {}: {}", e.getClass().getSimpleName(), e.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 토큰이 유효하지 않습니다.");
            }
        }
    }


    /**
     * JWT 리프레시 토큰으로 액세스 토큰 갱신
     *
     * 보안 정책:
     * - 리프레시 토큰은 재발급하지 않음 (보안 강화)
     * - 사용자 존재 및 활성 상태 확인
     * - 새로운 액세스 토큰만 발급
     *
     * @param refreshToken 리프레시 토큰
     * @return 새로운 액세스 토큰 정보
     * @throws ResponseStatusException 토큰 무효, 사용자 없음 시
     */
    public AccessOnlyResponse refreshAccessOnly(String refreshToken) {
        log.info("액세스 토큰 갱신 요청");

        try {
            // 1. 리프레시 토큰 검증 (만료, 서명 확인)
            Jws<Claims> jws = jwtTokenProvider.parse(refreshToken);
            Claims claims = jws.getBody();

            // 2. 사용자 ID 추출 및 존재 확인
            Long userId = Long.valueOf(claims.getSubject());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("토큰 갱신 중 사용자 없음 - 사용자 ID: {}", userId);
                        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다.");
                    });

            log.info("토큰 갱신 사용자 확인 완료 - 사용자 ID: {}", userId);

            // 3. 새로운 액세스 토큰 발급 (리프레시 토큰은 재발급하지 않음)
            String newAccessToken = jwtTokenProvider.createAccessToken(
                    String.valueOf(user.getId()),
                    user.getRole().name(),
                    ACCESS_TOKEN_TTL
            );

            // 4. 만료 시간 추출 및 KST 변환
            long accessExpireTime = jwtTokenProvider.parse(newAccessToken).getBody().getExpiration().getTime();
            String accessExpireKst = formatToKst(accessExpireTime);

            log.info("[사용자 작동] 액세스 토큰 갱신 완료 - 사용자 ID: {}, 만료시간: {}", userId, accessExpireKst);

            return AccessOnlyResponse.builder()
                    .tokenType("Bearer")
                    .accessToken(newAccessToken)
                    .expiresIn(ACCESS_TOKEN_TTL / 1000) // 초 단위로 변환
                    .accessTokenExpiresAtKst(accessExpireKst)
                    .build();

        } catch (Exception e) {
            if (e instanceof ResponseStatusException) {
                throw e;
            }
            log.error("토큰 갱신 중 오류 발생 {}: {}", e.getClass().getSimpleName(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
        }
    }


    /**
     * JWT 토큰 응답 생성 (공통 로직)
     */
    private AuthResponse generateJwtTokenResponse(User user) {
        log.debug("JWT 토큰 응답 생성 시작 - 사용자 ID: {}", user.getId());

        // 1. 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                String.valueOf(user.getId()),
                user.getRole().name(),
                ACCESS_TOKEN_TTL
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(
                String.valueOf(user.getId()),
                REFRESH_TOKEN_TTL
        );

        // 2. 만료 시간 추출 및 KST 변환
        long accessExpireTime = jwtTokenProvider.parse(accessToken).getBody().getExpiration().getTime();
        long refreshExpireTime = jwtTokenProvider.parse(refreshToken).getBody().getExpiration().getTime();

        String accessExpireKst = formatToKst(accessExpireTime);
        String refreshExpireKst = formatToKst(refreshExpireTime);

        log.debug("JWT 토큰 생성 완료 - 액세스 만료: {}, 리프레시 만료: {}", accessExpireKst, refreshExpireKst);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .profileImageUrl(user.getProfileImageUrl())
                        .address(user.getAddress())
                        .role(user.getRole())
                        .build())
                .accessTokenExpiresAtKst(accessExpireKst)
                .refreshTokenExpiresAtKst(refreshExpireKst)
                .build();
    }

    /**
     * 사용자 검색 또는 생성 (신규 회원가입용)
     * AuthProvider로 카카오 OAuth ID 관리
     */
    private User findOrCreateUser(String email, String oauthId) {
        // 1. AuthProvider로 기존 사용자 확인
        return authProviderRepository.findByProviderAndProviderUserId("KAKAO", oauthId)
                .map(AuthProvider::getUser)
                .orElseGet(() -> createNewUserWithAuthProvider(email, oauthId));
    }

    /**
     * 새 사용자 생성 및 AuthProvider 연결 (카카오 로그인)
     */
    private User createNewUserWithAuthProvider(String email, String oauthId) {
        log.info("새 사용자 생성 시작 - 이메일: {}", email);

        // 1. User 엔티티 생성
        User newUser = User.builder()
                .email(email)
                .nickname(null) // 프로필 설정 화면에서 설정
                .mannerScore(BigDecimal.ZERO)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("사용자 저장 완료 - 사용자 ID: {}", savedUser.getId());

        // 2. AuthProvider 생성 및 연결
        AuthProvider authProvider = AuthProvider.builder()
                .user(savedUser)
                .provider("KAKAO")
                .providerUserId(oauthId)
                .build();

        authProviderRepository.save(authProvider);
        log.info("카카오 AuthProvider 연결 완료");

        return savedUser;
    }

    /**
     * 약관 동의 여부 검증
     */
    private void validateTermsAgreement(TermsAgreementRequest request) {
        if (!request.isServiceTermsAgreed() || !request.isPrivacyPolicyAgreed()) {
            log.warn("필수 약관 미동의 - 서비스: {}, 개인정보: {}",
                    request.isServiceTermsAgreed(), request.isPrivacyPolicyAgreed());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 약관에 동의해야 합니다.");
        }
        log.debug("약관 동의 확인 완료 - 마케팅: {}", request.isMarketingOptionalAgreed());
    }


    /**
     * 시간을 KST 문자열로 변환
     */
    private String formatToKst(long epochMillis) {
        return KST_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }
}
