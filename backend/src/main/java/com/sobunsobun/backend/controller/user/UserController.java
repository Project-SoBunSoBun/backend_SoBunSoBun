package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.UserService;
import com.sobunsobun.backend.dto.user.NicknameRequest;
import com.sobunsobun.backend.dto.user.ProfileUpdateRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 사용자 관리 REST API 컨트롤러
 *
 * 담당 기능:
 * - 닉네임 중복 확인 (공개 API)
 * - 사용자 프로필 관리 (인증 필요)
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {

    private final UserService userService;

    /**
     * 닉네임 중복 확인 API (공개)
     *
     * 회원가입/프로필 설정 시 닉네임 사용 가능 여부를 확인합니다.
     *
     * 검증 규칙:
     * - 1~8자 이내
     * - 한글, 영문, 숫자만 허용
     * - 특수문자, 공백 불허
     *
     * @param nickname 확인할 닉네임 (쿼리 파라미터)
     * @return 닉네임과 사용 가능 여부
     */
    @Operation(summary = "닉네임 중복 확인",
            description = "닉네임이 사용 가능한지 확인합니다. 회원가입 없이 호출 가능합니다.")
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Object>> checkNicknameAvailability(
            @Parameter(description = "확인할 닉네임 (1-8자, 한글/영문/숫자만)", example = "몽규명보 둘다 나가")
            @RequestParam
            @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
            @Size(max = 8, message = "닉네임은 최대 8자입니다.")
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글/영문/숫자만 가능합니다.")
            String nickname) {

        log.info("닉네임 중복 확인 요청: {}", nickname);

        // 닉네임 정규화 (공백 제거, 소문자 변환 등)
        String normalizedNickname = userService.normalizeNickname(nickname);
        boolean isAvailable = userService.isNicknameAvailable(normalizedNickname);

        log.info("닉네임 중복 확인 결과: {} -> 사용가능: {}", nickname, isAvailable);

        return ResponseEntity.ok(Map.of(
                "nickname", nickname,
                "normalizedNickname", normalizedNickname,
                "available", isAvailable
        ));
    }

    /**
     * 내 닉네임 변경 API (인증 필요)
     *
     * JWT 토큰으로 인증된 사용자의 닉네임을 변경합니다.
     *
     * 주의사항:
     * - 중복 확인을 먼저 수행하는 것을 권장
     * - 변경 후 프로필 캐시 갱신 필요
     *
     * @param principal JWT에서 추출된 사용자 정보
     * @param request 새로운 닉네임 정보
     * @return 변경된 닉네임 정보
     */
    @Operation(summary = "내 닉네임 변경",
            description = "인증된 사용자의 닉네임을 변경함. JWT 토큰 필요합니데이")
    @PatchMapping("/me/nickname")
    public ResponseEntity<Map<String, Object>> updateMyNickname(
            @Parameter(hidden = true) // Swagger에서 숨김 (JWT에서 자동 추출)
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "새로운 닉네임 정보")
            @RequestBody @Valid NicknameRequest request) {

        Long userId = principal.id();
        String newNickname = request.nickname();

        log.info("닉네임 변경 요청 - 사용자 ID: {}, 새 닉네임: {}", userId, newNickname);

        // 닉네임 정규화 및 변경
        String normalizedNickname = userService.normalizeNickname(newNickname);
        userService.updateUserNickname(userId, normalizedNickname);

        log.info("닉네임 변경 완료 - 사용자 ID: {}, 변경된 닉네임: {}", userId, normalizedNickname);

        return ResponseEntity.ok(Map.of(
                "nickname", normalizedNickname,
                "message", "닉네임이 성공적으로 변경되었습니다."
        ));
    }

    /**
     * 프로필 업데이트 API (닉네임 + 프로필 이미지) - 인증 필요
     *
     * 회원가입 완료 또는 프로필 수정 시 호출합니다.
     * 닉네임과 프로필 이미지를 함께 업데이트할 수 있습니다.
     *
     * 요청 형식: multipart/form-data
     * - nickname: 새로운 닉네임 (필수)
     * - profileImage: 프로필 이미지 파일 (선택, jpg/png/webp, 5MB 이하)
     *
     * @param principal JWT에서 추출된 사용자 정보
     * @param nickname 새로운 닉네임
     * @param profileImage 프로필 이미지 파일 (선택적)
     * @return 업데이트 결과
     */
    @Operation(summary = "프로필 업데이트 (닉네임 + 이미지)",
            description = "닉네임과 프로필 이미지를 업데이트합니다. 이미지를 보내지 않거나 빈 값을 보내면 null로 저장됩니다.")
    @PatchMapping(value = "/me/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updateMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal JwtUserPrincipal principal,

            @Parameter(description = "새로운 닉네임 (1-8자, 한글/영문/숫자)")
            @RequestParam
            @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
            @Size(max = 8, message = "닉네임은 최대 8자입니다.")
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글/영문/숫자만 가능합니다.")
            String nickname,

            @Parameter(description = "프로필 이미지 (jpg/png/webp, 5MB 이하, 선택사항 - 없으면 null 저장)")
            @RequestParam(required = false)
            MultipartFile profileImage) {

        Long userId = principal.id();

        String imageInfo;
        if (profileImage != null && !profileImage.isEmpty()) {
            imageInfo = profileImage.getOriginalFilename();
        } else {
            imageInfo = "null 저장 (이미지 삭제)";
        }

        log.info("프로필 업데이트 요청 - 사용자 ID: {}, 닉네임: {}, 이미지: {}",
                userId, nickname, imageInfo);

        // 프로필 업데이트 (닉네임 + 이미지) - 이미지가 없으면 null 저장
        userService.updateUserProfile(userId, nickname, profileImage);

        log.info("프로필 업데이트 완료 - 사용자 ID: {}", userId);

        return ResponseEntity.ok(Map.of(
                "message", "프로필이 성공적으로 업데이트되었습니다.",
                "nickname", userService.normalizeNickname(nickname)
        ));
    }

    /**
     * 프로필 이미지만 업데이트 API - 인증 필요
     *
     * 닉네임 변경 없이 프로필 이미지만 변경할 때 사용합니다.
     *
     * 요청 형식: multipart/form-data
     * - profileImage: 프로필 이미지 파일 (필수, jpg/png/webp, 5MB 이하)
     *
     * @param principal JWT에서 추출된 사용자 정보
     * @param profileImage 프로필 이미지 파일
     * @return 업데이트 결과
     */
    @Operation(summary = "프로필 이미지만 업데이트",
            description = "프로필 이미지만 변경합니다. 빈 값을 보내면 null로 저장됩니다.")
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updateMyProfileImage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal JwtUserPrincipal principal,

            @Parameter(description = "프로필 이미지 (jpg/png/webp, 5MB 이하, 비어있으면 null 저장)")
            @RequestParam(required = false)
            MultipartFile profileImage) {

        Long userId = principal.id();
        String fileName = (profileImage != null && !profileImage.isEmpty())
                ? profileImage.getOriginalFilename()
                : "빈 파일 (null 저장)";
        log.info("프로필 이미지 업데이트 요청 - 사용자 ID: {}, 이미지: {}", userId, fileName);

        // 이미지만 업데이트 (빈 파일이면 null 저장)
        userService.updateProfileImage(userId, profileImage);

        log.info("프로필 이미지 업데이트 완료 - 사용자 ID: {}", userId);

        return ResponseEntity.ok(Map.of(
                "message", "프로필 이미지가 성공적으로 업데이트되었습니다."
        ));
    }
}
