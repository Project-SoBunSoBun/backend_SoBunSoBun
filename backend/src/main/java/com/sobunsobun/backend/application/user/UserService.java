package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.user.Role;
import com.sobunsobun.backend.domain.user.User;
import com.sobunsobun.backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository repo;
    public UserService(UserRepository repo){ this.repo = repo; }

    public static record UpsertResult(User user, boolean isNew) {}

    @Transactional
    public UpsertResult upsertByKakaoProfile(Long kakaoId, String email, String nickname, String profileUrl){
        String oauthId = "kakao:" + kakaoId;
        return repo.findByOauthId(oauthId)
                .map(u -> new UpsertResult(u, false))
                .orElseGet(() -> {
                    User u = repo.save(User.builder()
                            .oauthId(oauthId)
                            .email(email)
                            .nickname(nickname != null ? nickname : "사용자")
                            .profileImageUrl(profileUrl)
                            .role(Role.USER)
                            .build());
                    return new UpsertResult(u, true);
                });
    }

    @Transactional
    public UpsertResult upsertFromOAuth(String providerId, String email, String nickname, String profileUrl){
        return repo.findByOauthId(providerId)
                .map(u -> new UpsertResult(u, false))
                .orElseGet(() -> {
                    User u = repo.save(User.builder()
                            .oauthId(providerId) // ex) "kakao:123", "google:abc", "apple:def"
                            .email(email)
                            .nickname(nickname != null ? nickname : "사용자")
                            .profileImageUrl(profileUrl)
                            .role(Role.USER)
                            .build());
                    return new UpsertResult(u, true);
                });
    }

}
