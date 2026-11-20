package com.blog.application.article;

import com.blog.application.article.ArticleViews.ArchiveItem;
import com.blog.application.article.ArticleViews.ArticleCard;
import com.blog.application.article.ArticleViews.ArticleDetail;
import com.blog.application.article.ArticleViews.TagView;
import com.blog.domain.article.Article;
import com.blog.domain.article.ArticleArchive;
import com.blog.domain.article.ArticleQuery;
import com.blog.domain.article.ArticleRepository;
import com.blog.domain.attachment.AttachmentRepository;
import com.blog.domain.category.Category;
import com.blog.domain.category.CategoryRepository;
import com.blog.domain.comment.CommentRepository;
import com.blog.domain.common.DomainException;
import com.blog.domain.common.DateRange;
import com.blog.domain.common.PageResult;
import com.blog.domain.tag.Tag;
import com.blog.domain.tag.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文章用例编排：公开读侧（列表 / 详情 / 归档）与后台写侧（草稿 / 发布 / 置顶 / 删除）。
 */
@Service
public class ArticleApplicationService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachments;

    public ArticleApplicationService(ArticleRepository articleRepository,
                                     CategoryRepository categoryRepository,
                                     TagRepository tagRepository,
                                     CommentRepository commentRepository,
                                     AttachmentRepository attachments) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.commentRepository = commentRepository;
        this.attachments = attachments;
    }

    @Transactional(readOnly = true)
    public PageResult<ArticleCard> searchPublished(String keyword, Long categoryId, Long tagId, String sort, int page, int size) {
        return search(new ArticleQuery(keyword, categoryId, tagId, Article.Status.PUBLISHED, sort, page, size));
    }

    @Transactional(readOnly = true)
    public PageResult<ArticleCard> searchAll(String keyword, Long categoryId, Long tagId, String status, String sort, int page, int size) {
        return searchAll(keyword, categoryId, tagId, status, sort, page, size, new DateRange(null, null));
    }

    @Transactional(readOnly = true)
    public PageResult<ArticleCard> searchAll(String keyword, Long categoryId, Long tagId, String status, String sort, int page, int size, DateRange dates) {
        Article.Status parsed = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                ? null
                : Article.Status.of(status);
        return search(new ArticleQuery(keyword, categoryId, tagId, parsed, sort, page, size, dates.from(), dates.until()));
    }

    private PageResult<ArticleCard> search(ArticleQuery rawQuery) {
        ArticleQuery query = rawQuery.normalized();
        PageResult<Article> page = articleRepository.search(query);
        Map<Long, List<Tag>> tagsByArticle = resolveTags(page.items());
        Map<Long, String> categoryNames = resolveCategoryNames(page.items());
        Map<Long, Long> commentCounts = commentRepository.countApprovedByArticleIds(
                page.items().stream().map(Article::id).toList());

        List<ArticleCard> cards = page.items().stream()
                .map(article -> toCard(article,
                        categoryNames.get(article.categoryId()),
                        tagsByArticle.getOrDefault(article.id(), List.of()),
                        commentCounts.getOrDefault(article.id(), 0L)))
                .toList();
        return PageResult.of(cards, page.total(), page.page(), page.size());
    }

    /** 公开详情：草稿不可见；每次读取累加阅读量。 */
    @Transactional
    public ArticleDetail publishedDetail(Long id) {
        Article article = requireArticle(id);
        if (!article.isPublished()) {
            throw DomainException.notFound("文章不存在或尚未发布");
        }
        articleRepository.addViewCount(id);
        article.registerView();
        return toDetail(article);
    }

    /** 后台详情：可见草稿，不累加阅读量。 */
    @Transactional(readOnly = true)
    public ArticleDetail adminDetail(Long id) {
        return toDetail(requireArticle(id));
    }

    @Transactional
    public ArticleDetail create(ArticleCommand command) {
        Article article = Article.draft(command.title(), command.summary(), command.content(),
                command.cover(), command.categoryId(), command.tagIds());
        // 新建一律落地为草稿：命令里的 pinned 字段语义是「发布后生效」，
        // 不能因为勾选了置顶就把草稿直接发布出去。
        Article saved = articleRepository.save(article);
        attachments.replaceArticleReferences(saved.id(), article.cover(), article.content());
        return toDetail(saved);
    }

    @Transactional
    public ArticleDetail update(Long id, ArticleCommand command) {
        Article article = requireArticle(id);
        article.revise(command.title(), command.summary(), command.content(), command.cover(),
                command.categoryId(), command.tagIds());
        Article saved = articleRepository.save(article);
        attachments.replaceArticleReferences(saved.id(), article.cover(), article.content());
        if (command.pinned() != null && command.pinned() != saved.pinned() && saved.isPublished()) {
            saved.pin(command.pinned());
            saved = articleRepository.save(saved);
        }
        return toDetail(saved);
    }

    @Transactional
    public ArticleDetail publish(Long id, boolean pinned) {
        Article article = requireArticle(id);
        article.publish();
        if (pinned) {
            article.pin(true);
        }
        return toDetail(articleRepository.save(article));
    }

    @Transactional
    public ArticleDetail withdraw(Long id) {
        Article article = requireArticle(id);
        // 撤回同时会取消置顶（草稿不应处于置顶状态）
        article.withdraw();
        return toDetail(articleRepository.save(article));
    }

    @Transactional
    public ArticleDetail pin(Long id, boolean pinned) {
        Article article = requireArticle(id);
        article.pin(pinned);
        return toDetail(articleRepository.save(article));
    }

    @Transactional
    public void delete(Long id) {
        requireArticle(id);
        articleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ArchiveItem> archives() {
        List<ArticleArchive> archives = articleRepository.findArchives();
        return archives.stream()
                .map(a -> new ArchiveItem(a.year(), a.month(), a.count()))
                .toList();
    }

    /**
     * 置顶文章列表。
     * <p>
     * 必须由存储层直接过滤 pinned，而不是先取“最新 N 篇再筛选”——
     * 置顶文章可能早于最新 N 篇，先取后筛会漏掉。
     */
    @Transactional(readOnly = true)
    public List<ArticleCard> pinnedPublished() {
        List<Article> articles = articleRepository.findPinned();
        if (articles.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Tag>> tagsByArticle = resolveTags(articles);
        Map<Long, String> categoryNames = resolveCategoryNames(articles);
        Map<Long, Long> commentCounts = commentRepository.countApprovedByArticleIds(
                articles.stream().map(Article::id).toList());
        return articles.stream()
                .map(article -> toCard(article,
                        categoryNames.get(article.categoryId()),
                        tagsByArticle.getOrDefault(article.id(), List.of()),
                        commentCounts.getOrDefault(article.id(), 0L)))
                .toList();
    }

    private Article requireArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> DomainException.notFound("文章不存在: " + id));
    }

    private ArticleDetail toDetail(Article article) {
        Map<Long, List<Tag>> tags = resolveTags(List.of(article));
        String categoryName = resolveCategoryNames(List.of(article)).get(article.categoryId());
        Category category = article.categoryId() == null ? null
                : categoryRepository.findById(article.categoryId()).orElse(null);
        long commentCount = commentRepository.countApprovedByArticleIds(List.of(article.id()))
                .getOrDefault(article.id(), 0L);
        List<ArticleCard> related = articleRepository.findRelated(article.id(), article.categoryId(), 3).stream()
                .map(item -> toCard(item, resolveCategoryNames(List.of(item)).get(item.categoryId()),
                        List.of(), 0L))
                .toList();
        ArticleCard previous = articleRepository.findPrevious(article)
                .map(item -> toCard(item, resolveCategoryNames(List.of(item)).get(item.categoryId()),
                        List.of(), 0L))
                .orElse(null);
        ArticleCard next = articleRepository.findNext(article)
                .map(item -> toCard(item, resolveCategoryNames(List.of(item)).get(item.categoryId()),
                        List.of(), 0L))
                .orElse(null);
        int wordCount = countWords(article.content());
        return new ArticleDetail(
                article.id(),
                article.title(),
                article.summary(),
                article.content(),
                article.cover(),
                article.categoryId(),
                categoryName,
                category == null ? null : category.slug(),
                tags.getOrDefault(article.id(), List.of()).stream().map(this::toTagView).toList(),
                article.pinned(),
                article.viewCount(),
                commentCount,
                article.status().name(),
                wordCount,
                Math.max(1, (int) Math.ceil(wordCount / 400.0)),
                article.publishedAt(),
                article.createdAt(),
                article.updatedAt(),
                previous,
                next,
                related);
    }

    private ArticleCard toCard(Article article, String categoryName, List<Tag> tags, long commentCount) {
        return new ArticleCard(
                article.id(),
                article.title(),
                article.summary(),
                article.cover(),
                article.categoryId(),
                categoryName,
                tags.stream().map(this::toTagView).toList(),
                article.pinned(),
                article.viewCount(),
                commentCount,
                article.status().name(),
                article.publishedAt(),
                article.createdAt(),
                article.updatedAt());
    }

    private TagView toTagView(Tag tag) {
        return new TagView(tag.id(), tag.name(), tag.color());
    }

    private Map<Long, List<Tag>> resolveTags(Collection<Article> articles) {
        List<Long> tagIds = articles.stream()
                .flatMap(article -> article.tagIds().stream())
                .distinct()
                .toList();
        if (tagIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Tag> tags = tagRepository.findByIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::id, Function.identity()));
        Map<Long, List<Tag>> result = new LinkedHashMap<>();
        for (Article article : articles) {
            List<Tag> resolved = article.tagIds().stream()
                    .map(tags::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            result.put(article.id(), resolved);
        }
        return result;
    }

    /**
     * 分类 id → 分类名。
     * <p>
     * 必须返回允许 null 键的 Map：调用方会用 {@code article.categoryId()} 直接取值，
     * 未分类文章的 categoryId 为 null，而 {@code Map.of()} 的 {@code get(null)} 会抛 NPE。
     */
    private Map<Long, String> resolveCategoryNames(Collection<Article> articles) {
        Map<Long, String> result = new HashMap<>();
        List<Long> categoryIds = articles.stream()
                .map(Article::categoryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return result;
        }
        for (Category category : categoryRepository.findAll()) {
            if (categoryIds.contains(category.id())) {
                result.put(category.id(), category.name());
            }
        }
        return result;
    }

    /** 中文字符按字计，西文按词计，用于阅读时长估算。 */
    static int countWords(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        String plain = content.replaceAll("```[\\s\\S]*?```", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("[#*_>`~\\-|\\[\\]()]", " ");
        int cjk = 0;
        int words = 0;
        StringBuilder latin = new StringBuilder();
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                cjk++;
                if (!latin.isEmpty()) {
                    words++;
                    latin.setLength(0);
                }
            } else if (Character.isLetterOrDigit(c)) {
                latin.append(c);
            } else if (!latin.isEmpty()) {
                words++;
                latin.setLength(0);
            }
        }
        if (!latin.isEmpty()) {
            words++;
        }
        return cjk + words;
    }
}
