package com.blog.application.article;

/**
 * 文章写入命令。title 与 content 由领域层校验，此处只做承载。
 */
public record ArticleCommand(
        String title,
        String summary,
        String content,
        String cover,
        Long categoryId,
        java.util.List<Long> tagIds,
        Boolean pinned) {
}
