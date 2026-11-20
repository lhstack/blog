package com.blog.application.article;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用层读模型：面向前端展示，不暴露领域实体内部结构。
 */
public final class ArticleViews {

    private ArticleViews() {
    }

    public record TagView(Long id, String name, String color) {
    }

    public record CategoryView(Long id, String name, String slug, String description, long articleCount) {
    }

    /** 列表卡片。 */
    public record ArticleCard(
            Long id,
            String title,
            String summary,
            String cover,
            Long categoryId,
            String categoryName,
            List<TagView> tags,
            boolean pinned,
            long viewCount,
            long commentCount,
            String status,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    /** 详情视图。 */
    public record ArticleDetail(
            Long id,
            String title,
            String summary,
            String content,
            String cover,
            Long categoryId,
            String categoryName,
            String categorySlug,
            List<TagView> tags,
            boolean pinned,
            long viewCount,
            long commentCount,
            String status,
            int wordCount,
            int readingMinutes,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            ArticleCard previous,
            ArticleCard next,
            List<ArticleCard> related) {
    }

    public record ArchiveItem(String year, String month, long count) {
    }
}
