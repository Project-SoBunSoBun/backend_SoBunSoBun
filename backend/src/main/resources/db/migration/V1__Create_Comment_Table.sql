-- Comment 테이블 생성 스크립트
-- 댓글/대댓글 기능 구현
-- JPA 엔티티 기반으로 생성

CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '댓글 ID',
    `post_id` BIGINT NOT NULL COMMENT '게시글 ID (외래키)',
    `user_id` BIGINT NOT NULL COMMENT '작성자 ID (외래키)',
    `parent_comment_id` BIGINT COMMENT '부모 댓글 ID (대댓글인 경우만 설정)',
    `content` LONGTEXT NOT NULL COMMENT '댓글 내용',
    `deleted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Soft Delete 여부 (true: 삭제됨, false: 활성)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

    -- 외래키 제약조건
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `group_post`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_comment_id`) REFERENCES `comment`(`id`) ON DELETE SET NULL,

    -- 인덱스
    INDEX `idx_comment_post` (`post_id`),
    INDEX `idx_comment_user` (`user_id`),
    INDEX `idx_comment_parent` (`parent_comment_id`),
    INDEX `idx_comment_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='댓글/대댓글 테이블';

-- 추가 인덱스: post_id + deleted (삭제되지 않은 댓글 조회 최적화)
CREATE INDEX `idx_comment_post_deleted` ON `comment`(`post_id`, `deleted`);

-- 추가 인덱스: parent_id + deleted (대댓글 조회 최적화)
CREATE INDEX `idx_comment_parent_deleted` ON `comment`(`parent_comment_id`, `deleted`);

