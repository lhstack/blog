package com.blog.domain.article;

import java.time.LocalDateTime;

/** 订阅查询投影，不读取文章正文。 */
public record ArticleFeedEntry(Long id, String title, String summary, LocalDateTime publishedAt) {
}
