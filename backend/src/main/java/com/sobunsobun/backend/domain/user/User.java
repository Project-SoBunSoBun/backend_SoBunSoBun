package com.sobunsobun.backend.domain.user;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String oauthId;              // "kakao:{kakaoId}"

    private String email;
    private String nickname;
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private Role role;
}
