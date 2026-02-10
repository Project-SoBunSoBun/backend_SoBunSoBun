package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 1:1 문의 엔티티
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inquiry",
       indexes = {
           @Index(name = "idx_inquiry_user_id", columnList = "user_id"),
           @Index(name = "idx_inquiry_status", columnList = "status"),
           @Index(name = "idx_inquiry_created_at", columnList = "created_at")
       })
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 문의자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inquiry_user"))
    private User user;

    /**
     * 문의 유형 코드
     */
    @Column(name = "type_code", nullable = false, length = 30)
    private String typeCode;

    /**
     * 문의 내용
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 답변 받을 이메일
     */
    @Column(name = "reply_email", nullable = false, length = 100)
    private String replyEmail;

    /**
     * 첨부 이미지 URL (JSON 배열 형식)
     */
    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    /**
     * 상태 (RECEIVED, IN_REVIEW, ANSWERED, CLOSED)
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "RECEIVED";

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
