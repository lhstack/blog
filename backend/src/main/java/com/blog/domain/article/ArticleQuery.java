package com.blog.domain.article;

import java.time.LocalDate;

/**
 * 文章检索条件。null / blank 字段表示不过滤。
 *
 * @param keyword    标题或摘要模糊匹配
 * @param categoryId 分类过滤
 * @param tagId      标签过滤
 * @param status     状态过滤，null 表示全部（后台用）
 * @param sort       排序：latest / oldest / views / title
 */
public record ArticleQuery(String keyword, Long categoryId, Long tagId, Article.Status status,
                           String sort, int page, int size, LocalDate from, LocalDate until) {

    public ArticleQuery(String keyword, Long categoryId, Long tagId, Article.Status status, String sort, int page, int size) {
        this(keyword, categoryId, tagId, status, sort, page, size, null, null);
    }

    public static ArticleQuery published(int page, int size) {
        return new ArticleQuery(null, null, null, Article.Status.PUBLISHED, "latest", page, size);
    }

    public ArticleQuery normalized() {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSort = sort == null || sort.isBlank() ? "latest" : sort.trim().toLowerCase();
        String safeKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return new ArticleQuery(safeKeyword, categoryId, tagId, status, safeSort, safePage, safeSize, from, until);
    }

    public long offset() {
        return (long) (page - 1) * size;
    }
}
