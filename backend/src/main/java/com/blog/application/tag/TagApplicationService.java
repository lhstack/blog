package com.blog.application.tag;

import com.blog.application.article.ArticleViews.TagView;
import com.blog.domain.article.Article;
import com.blog.domain.article.ArticleQuery;
import com.blog.domain.article.ArticleRepository;
import com.blog.domain.common.DomainException;
import com.blog.domain.tag.Tag;
import com.blog.domain.tag.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagApplicationService {

    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;

    public TagApplicationService(TagRepository tagRepository, ArticleRepository articleRepository) {
        this.tagRepository = tagRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<TagView> listAll() {
        return tagRepository.findAll().stream()
                .map(tag -> new TagView(tag.id(), tag.name(), tag.color()))
                .toList();
    }

    @Transactional
    public TagView create(String name, String color) {
        if (tagRepository.findByName(name).isPresent()) {
            throw DomainException.conflict("标签已存在: " + name);
        }
        return toView(tagRepository.save(Tag.create(name, color)));
    }

    @Transactional
    public TagView update(Long id, String name, String color) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> DomainException.notFound("标签不存在: " + id));
        tagRepository.findByName(name)
                .filter(existing -> !existing.id().equals(id))
                .ifPresent(existing -> {
                    throw DomainException.conflict("标签已存在: " + name);
                });
        tag.redefine(name, color);
        return toView(tagRepository.save(tag));
    }

    @Transactional
    public void delete(Long id) {
        tagRepository.findById(id)
                .orElseThrow(() -> DomainException.notFound("标签不存在: " + id));
        ArticleQuery query = new ArticleQuery(null, null, id, null, "latest", 1, 1);
        if (articleRepository.search(query).total() > 0) {
            throw DomainException.conflict("该标签仍被文章使用，无法删除");
        }
        tagRepository.deleteById(id);
    }

    private TagView toView(Tag tag) {
        return new TagView(tag.id(), tag.name(), tag.color());
    }

    /** 供站点统计使用：已发布文章数。 */
    @Transactional(readOnly = true)
    public long publishedArticleCount() {
        return articleRepository.countByStatus(Article.Status.PUBLISHED);
    }
}
