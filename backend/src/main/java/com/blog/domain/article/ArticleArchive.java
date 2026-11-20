package com.blog.domain.article;

/**
 * 归档条目：按年月聚合的文章数量。
 */
public record ArticleArchive(String year, String month, long count) {
}
