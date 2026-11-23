package com.blog.infrastructure.persistence.article;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.domain.article.Article;
import com.blog.domain.article.ArticleArchive;
import com.blog.domain.article.ArticleFeedEntry;
import com.blog.domain.article.ArticleQuery;
import com.blog.domain.article.ArticleRepository;
import com.blog.domain.common.PageResult;
import com.blog.infrastructure.persistence.mapper.ArticleBaseMapper;
import com.blog.infrastructure.persistence.mapper.ArticleMapper;
import com.blog.infrastructure.persistence.mapper.ArticleTagMapper;
import com.blog.infrastructure.persistence.po.ArticlePo;
import com.blog.infrastructure.persistence.po.ArticleTagPo;
import com.blog.infrastructure.persistence.typehandler.LocalDateTimeTypeHandler;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章仓储实现。
 * <p>
 * 文章与标签的关联存放在关联表，聚合保存时整体替换关联集合（先删后插），
 * 保证聚合内一致性由一次事务覆盖。
 */
@Repository
public class ArticleRepositoryImpl implements ArticleRepository {

    private final ArticleBaseMapper baseMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;

    public ArticleRepositoryImpl(ArticleBaseMapper baseMapper,
                                 ArticleMapper articleMapper,
                                 ArticleTagMapper articleTagMapper) {
        this.baseMapper = baseMapper;
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
    }

    @Override
    public Optional<Article> findById(Long id) {
        ArticlePo po = baseMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po, Set.copyOf(articleTagMapper.selectTagIds(id))));
    }

    @Override
    public List<Article> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ArticlePo> pos = baseMapper.selectBatchIds(ids);
        Map<Long, Set<Long>> tags = tagsOf(ids);
        return pos.stream().map(po -> toDomain(po, tags.getOrDefault(po.getId(), Set.of()))).toList();
    }

    @Override
    public PageResult<Article> search(ArticleQuery query) {
        QueryWrapper<ArticlePo> wrapper = buildWrapper(query);
        IPage<ArticlePo> page = baseMapper.selectPage(
                Page.of(query.page(), query.size()), wrapper);

        List<ArticlePo> records = page.getRecords();
        Map<Long, Set<Long>> tags = tagsOf(records.stream().map(ArticlePo::getId).toList());
        List<Article> items = records.stream()
                .map(po -> toDomain(po, tags.getOrDefault(po.getId(), Set.of())))
                .toList();
        return PageResult.of(items, page.getTotal(), query.page(), query.size());
    }

    @Override
    public List<ArticleFeedEntry> findLatestPublishedEntries(int limit) {
        return articleMapper.selectLatestPublishedEntries(limit);
    }

    /** 转义 LIKE 元字符，使用不受数据库反斜杠规则影响的显式转义符。 */
    private static String escapeLike(String keyword) {
        return "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private QueryWrapper<ArticlePo> buildWrapper(ArticleQuery query) {
        QueryWrapper<ArticlePo> wrapper = new QueryWrapper<>();
        if (query.status() != null) {
            wrapper.eq("status", query.status().name());
        }
        if (query.categoryId() != null) {
            wrapper.eq("category_id", query.categoryId());
        }
        if (query.tagId() != null) {
            // 不用 inSql 拼接：inSql 是原样插入 SQL 片段，绕过所有参数绑定。
            // 这里用 apply 的 {0} 占位符，走 PreparedStatement，值由驱动转义。
            wrapper.apply("id IN (SELECT article_id FROM blog_article_tag WHERE tag_id = {0})", query.tagId());
        }
        if (query.keyword() != null) {
            // 用户搜索词的 % 与 _ 必须当字面量：MyBatis-Plus 的 like() 直接拼进 LIKE 模式，
            // 不转义会让搜 "S%" 命中所有以 S 开头的文章。
            String pattern = escapeLike(query.keyword());
            wrapper.and(w -> w.apply("LOWER(title) LIKE LOWER({0}) ESCAPE '!'", pattern)
                    .or().apply("LOWER(summary) LIKE LOWER({0}) ESCAPE '!'", pattern));
        }
        if (query.from() != null) wrapper.ge("updated_at", query.from().toString());
        if (query.until() != null) wrapper.lt("updated_at", query.until().toString());
        switch (query.sort()) {
            case "views" -> wrapper.orderByDesc("view_count").orderByDesc("id");
            case "oldest" -> wrapper.orderByAsc("COALESCE(published_at, created_at)")
                    .orderByAsc("id");
            case "title" -> wrapper.orderByAsc("title");
            default -> wrapper.orderByDesc("pinned")
                    .orderByDesc("COALESCE(published_at, created_at)")
                    .orderByDesc("id");
        }
        return wrapper;
    }

    @Override
    public Article save(Article article) {
        ArticlePo po = toPo(article);
        if (po.getId() == null) {
            baseMapper.insert(po);
        } else {
            baseMapper.updateById(po);
        }
        replaceTags(po.getId(), article.tagIds());
        return toDomain(po, article.tagIds());
    }

    private void replaceTags(Long articleId, Set<Long> tagIds) {
        articleTagMapper.unlinkAll(articleId);
        for (Long tagId : tagIds) {
            articleTagMapper.link(articleId, tagId);
        }
    }

    @Override
    public void deleteById(Long id) {
        articleTagMapper.unlinkAll(id);
        baseMapper.deleteById(id);
    }

    @Override
    public void addViewCount(Long id) {
        articleMapper.incrementViewCount(id);
    }

    @Override
    public List<ArticleArchive> findArchives() {
        return articleMapper.selectArchives().stream()
                .map(row -> new ArticleArchive(row.getYear(), row.getMonth(),
                        row.getTotal() == null ? 0L : row.getTotal()))
                .toList();
    }

    @Override
    public long countByStatus(Article.Status status) {
        LambdaQueryWrapper<ArticlePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticlePo::getStatus, status.name());
        return baseMapper.selectCount(wrapper);
    }

    @Override
    public long totalViewCount() {
        return articleMapper.sumViewCount();
    }

    @Override
    public List<Article> findRelated(Long articleId, Long categoryId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<ArticlePo> pos = categoryId == null
                ? List.of()
                : articleMapper.selectRelatedInCategory(articleId, categoryId, limit);
        if (pos.isEmpty()) {
            pos = articleMapper.selectPopularExcluding(articleId, limit);
        }
        List<Long> ids = pos.stream().map(ArticlePo::getId).toList();
        Map<Long, Set<Long>> tags = tagsOf(ids);
        return pos.stream().map(po -> toDomain(po, tags.getOrDefault(po.getId(), Set.of()))).toList();
    }

    /** 上一篇 / 下一篇：只在已发布文章间导航。 */
    @Override
    public Optional<Article> findPrevious(Article current) {
        String publishedAt = LocalDateTimeTypeHandler.format(
                current.publishedAt() == null ? current.createdAt() : current.publishedAt());
        ArticlePo po = articleMapper.selectPrevious(publishedAt, current.id());
        return po == null ? Optional.empty() : Optional.of(toDomain(po, Set.of()));
    }

    @Override
    public Optional<Article> findNext(Article current) {
        String publishedAt = LocalDateTimeTypeHandler.format(
                current.publishedAt() == null ? current.createdAt() : current.publishedAt());
        ArticlePo po = articleMapper.selectNext(publishedAt, current.id());
        return po == null ? Optional.empty() : Optional.of(toDomain(po, Set.of()));
    }

    @Override
    public List<Article> findPinned() {
        List<ArticlePo> pos = articleMapper.selectPinned();
        List<Long> ids = pos.stream().map(ArticlePo::getId).toList();
        Map<Long, Set<Long>> tags = tagsOf(ids);
        return pos.stream().map(po -> toDomain(po, tags.getOrDefault(po.getId(), Set.of()))).toList();
    }

    private Map<Long, Set<Long>> tagsOf(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        List<ArticleTagPo> links = articleTagMapper.selectByArticleIds(articleIds);
        Map<Long, Set<Long>> result = new HashMap<>();
        for (ArticleTagPo link : links) {
            result.computeIfAbsent(link.getArticleId(), key -> new LinkedHashSet<>())
                    .add(link.getTagId());
        }
        return result;
    }

    private Article toDomain(ArticlePo po, Set<Long> tagIds) {
        return new Article(
                po.getId(),
                po.getTitle(),
                po.getSummary(),
                po.getContent(),
                po.getCover(),
                po.getCategoryId(),
                new LinkedHashSet<>(tagIds),
                Article.Status.of(po.getStatus()),
                Integer.valueOf(1).equals(po.getPinned()),
                po.getViewCount() == null ? 0L : po.getViewCount(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getPublishedAt());
    }

    private ArticlePo toPo(Article article) {
        ArticlePo po = new ArticlePo();
        po.setId(article.id());
        po.setTitle(article.title());
        po.setSummary(article.summary());
        po.setContent(article.content());
        po.setCover(article.cover());
        po.setCategoryId(article.categoryId());
        po.setStatus(article.status().name());
        po.setPinned(article.pinned() ? 1 : 0);
        po.setViewCount(article.viewCount());
        po.setCreatedAt(article.createdAt());
        po.setUpdatedAt(article.updatedAt());
        po.setPublishedAt(article.publishedAt());
        return po;
    }
}
