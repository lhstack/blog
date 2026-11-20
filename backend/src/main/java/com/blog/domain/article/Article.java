package com.blog.domain.article;

import com.blog.domain.common.DomainException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 文章聚合根。
 * <p>
 * 聚合内部维护：状态流转（草稿 / 已发布 / 置顶）、标签关联（值化为一组标签 id）、
 * 阅读计数与内容修订。持久化细节（多表映射）由基础设施层实现。
 */
public class Article {

    public enum Status {
        DRAFT, PUBLISHED;

        public static Status of(String raw) {
            if (raw == null || raw.isBlank()) {
                return DRAFT;
            }
            return switch (raw.trim().toUpperCase()) {
                case "PUBLISHED" -> PUBLISHED;
                case "DRAFT" -> DRAFT;
                default -> throw DomainException.invalid("未知的文章状态: " + raw);
            };
        }
    }

    private final Long id;
    private String title;
    private String summary;
    private String content;
    private String cover;
    private Long categoryId;
    private Set<Long> tagIds;
    private Status status;
    private boolean pinned;
    private long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;

    public Article(Long id, String title, String summary, String content, String cover, Long categoryId,
                   Set<Long> tagIds, Status status, boolean pinned, long viewCount,
                   LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime publishedAt) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.cover = cover;
        this.categoryId = categoryId;
        this.tagIds = tagIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tagIds);
        this.status = status == null ? Status.DRAFT : status;
        this.pinned = pinned;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
    }

    public static Article draft(String title, String summary, String content, String cover,
                                Long categoryId, List<Long> tagIds) {
        validateTitle(title);
        validateContent(content);
        LocalDateTime now = LocalDateTime.now();
        return new Article(null, title.trim(), summary, content, cover, categoryId,
                tagIds == null ? Set.of() : new LinkedHashSet<>(tagIds),
                Status.DRAFT, false, 0L, now, now, null);
    }

    /** 保存编辑内容；已发布文章保持发布时间不变。 */
    public void revise(String title, String summary, String content, String cover,
                       Long categoryId, List<Long> tagIds) {
        validateTitle(title);
        validateContent(content);
        this.title = title.trim();
        this.summary = summary;
        this.content = content;
        this.cover = cover;
        this.categoryId = categoryId;
        this.tagIds = tagIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tagIds);
        this.updatedAt = LocalDateTime.now();
        if (this.status == Status.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = this.updatedAt;
        }
    }

    /** 发布：草稿转已发布，首次发布记录发布时间；重复调用幂等。 */
    public void publish() {
        if (this.status == Status.PUBLISHED && this.publishedAt != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        this.status = Status.PUBLISHED;
        this.publishedAt = now;
        this.updatedAt = now;
    }

    /**
     * 撤回为草稿，同时取消置顶——草稿不应处于置顶状态。
     * 保留历史发布时间，以便再次发布时延续原始时间。
     */
    public void withdraw() {
        if (this.status == Status.DRAFT && !this.pinned) {
            return;
        }
        this.status = Status.DRAFT;
        this.pinned = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 置顶 / 取消置顶。
     * <p>
     * 只有「置顶」要求文章已发布；「取消置顶」任何状态都必须允许——
     * 否则撤回置顶文章时会因为状态已变回草稿而失败。
     */
    public void pin(boolean pinned) {
        if (pinned && this.status != Status.PUBLISHED) {
            throw DomainException.conflict("只有已发布文章可以置顶");
        }
        this.pinned = pinned;
        this.updatedAt = LocalDateTime.now();
    }

    public void registerView() {
        this.viewCount = this.viewCount + 1;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw DomainException.invalid("文章标题不能为空");
        }
        if (title.trim().length() > 120) {
            throw DomainException.invalid("文章标题不能超过 120 个字符");
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw DomainException.invalid("文章内容不能为空");
        }
    }

    public boolean isPublished() {
        return status == Status.PUBLISHED;
    }

    public Long id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String summary() {
        return summary;
    }

    public String content() {
        return content;
    }

    public String cover() {
        return cover;
    }

    public Long categoryId() {
        return categoryId;
    }

    public Set<Long> tagIds() {
        return Collections.unmodifiableSet(tagIds);
    }

    public Status status() {
        return status;
    }

    public boolean pinned() {
        return pinned;
    }

    public long viewCount() {
        return viewCount;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public LocalDateTime publishedAt() {
        return publishedAt;
    }
}
