package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 약관 엔티티
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "terms",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_type_version", columnNames = {"type", "version"})
       },
       indexes = {
           @Index(name = "idx_type_effective", columnList = "type, effective_date DESC")
       })
public class Terms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 약관 유형 (SERVICE, PRIVACY)
     */
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    /**
     * 버전 (예: 1.0.0, 1.1.0)
     */
    @Column(name = "version", nullable = false, length = 20)
    private String version;

    /**
     * 제목
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 내용
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 시행일
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

