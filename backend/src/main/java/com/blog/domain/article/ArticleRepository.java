package com.blog.domain.article;

import com.blog.domain.common.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 文章仓储。查询条件用 {@link ArticleQuery} 表达，避免应用层拼装 SQL 语义。
 */
public interface ArticleRepository {

    Optional<Article> findById(Long id);

    /** 后台详情：包含草稿。 */
    List<Article> findByIds(List<Long> ids);

    PageResult<Article> search(ArticleQuery query);

    /** 最近发布的文章摘要，按发布时间倒序，不考虑置顶。 */
    List<ArticleFeedEntry> findLatestPublishedEntries(int limit);

    Article save(Article article);

    void deleteById(Long id);

    void addViewCount(Long id);

    List<ArticleArchive> findArchives();

    long countByStatus(Article.Status status);

    long totalViewCount();

    List<Article> findRelated(Long articleId, Long categoryId, int limit);

    /** 置顶的已发布文章，按发布时间倒序。 */
    List<Article> findPinned();

    /** 上一篇（发布时间更早）。仅导航已发布文章。 */
    Optional<Article> findPrevious(Article current);

    /** 下一篇（发布时间更晚）。仅导航已发布文章。 */
    Optional<Article> findNext(Article current);
}
