package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.UserService;
import com.sobunsobun.backend.dto.user.NicknameRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal; // principal에 id 들고오는 타입
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Object>> checkNickname(
            @RequestParam
            @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
            @Size(max = 8, message = "닉네임은 최대 8자입니다.")
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글/영문/숫자만 가능합니다.")
            String nickname
    ) {
        boolean available   = userService.isNicknameAvailable(nickname);
        return ResponseEntity.ok(Map.of("nickname", nickname, "available", available));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<Map<String, Object>> updateNickname(
            @AuthenticationPrincipal JwtUserPrincipal principal, // Security에서 id 제공
            @RequestBody @Valid NicknameRequest request
    ) {
        Long userId = principal.id();
        userService.setNickname(userId, request.nickname());
        return ResponseEntity.ok(Map.of("nickname", request.nickname()));
    }
}
