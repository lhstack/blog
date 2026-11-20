package com.blog.domain.comment;

import com.blog.domain.common.DomainException;

import java.time.LocalDateTime;

/**
 * 评论聚合根。访客可提交，管理员可审核 / 删除。
 */
public class Comment {

    public enum Status {
        PENDING, APPROVED, REJECTED;

        public static Status of(String raw) {
            if (raw == null || raw.isBlank()) {
                return PENDING;
            }
            return switch (raw.trim().toUpperCase()) {
                case "PENDING" -> PENDING;
                case "APPROVED" -> APPROVED;
                case "REJECTED" -> REJECTED;
                default -> throw DomainException.invalid("未知的评论状态: " + raw);
            };
        }
    }

    private final Long id;
    private final Long articleId;
    private final String author;
    private final String email;
    private final String website;
    private final String content;
    private final String ip;
    private Status status;
    private final LocalDateTime createdAt;

    public Comment(Long id, Long articleId, String author, String email, String website, String content,
                   String ip, Status status, LocalDateTime createdAt) {
        this.id = id;
        this.articleId = articleId;
        this.author = author;
        this.email = email;
        this.website = website;
        this.content = content;
        this.ip = ip;
        this.status = status == null ? Status.PENDING : status;
        this.createdAt = createdAt;
    }

    public static Comment submit(Long articleId, String author, String email, String website, String content, String ip) {
        if (articleId == null) {
            throw DomainException.invalid("评论必须归属一篇文章");
        }
        if (author == null || author.isBlank()) {
            throw DomainException.invalid("昵称不能为空");
        }
        if (content == null || content.isBlank()) {
            throw DomainException.invalid("评论内容不能为空");
        }
        if (content.length() > 2000) {
            throw DomainException.invalid("评论内容不能超过 2000 个字符");
        }
        return new Comment(null, articleId, author.trim(), email, website, content.trim(),
                ip, Status.PENDING, LocalDateTime.now());
    }

    public void approve() {
        this.status = Status.APPROVED;
    }

    public void reject() {
        this.status = Status.REJECTED;
    }

    public boolean isVisible() {
        return status == Status.APPROVED;
    }

    public Long id() {
        return id;
    }

    public Long articleId() {
        return articleId;
    }

    public String author() {
        return author;
    }

    public String email() {
        return email;
    }

    public String website() {
        return website;
    }

    public String content() {
        return content;
    }

    public String ip() {
        return ip;
    }

    public Status status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
