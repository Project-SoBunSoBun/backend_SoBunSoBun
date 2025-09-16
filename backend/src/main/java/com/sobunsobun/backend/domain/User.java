package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "`user`")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=255)
    private String email;

    @Column(nullable=false, length=100)
    private String nickname;

    @Column(name="oauth_id", nullable=false, unique=true, length=100)
    private String oauthId;

    @Column(name="profile_image_url", length=500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private Role role;

    @CreationTimestamp
    @Column(name="create_dt", nullable=false)
    private LocalDateTime createDt;

    @UpdateTimestamp
    @Column(name="update_dt", nullable=false)
    private LocalDateTime updateDt;
}
