package com.blog.application.category;

import com.blog.application.article.ArticleViews.CategoryView;
import com.blog.domain.article.Article;
import com.blog.domain.article.ArticleQuery;
import com.blog.domain.article.ArticleRepository;
import com.blog.domain.category.Category;
import com.blog.domain.category.CategoryRepository;
import com.blog.domain.common.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryApplicationService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    public CategoryApplicationService(CategoryRepository categoryRepository, ArticleRepository articleRepository) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> listAll() {
        return categoryRepository.findAll().stream().map(this::toView).toList();
    }

    @Transactional
    public CategoryView create(String name, String slug, String description, int sortOrder) {
        if (categoryRepository.existsByName(name, null)) {
            throw DomainException.conflict("分类已存在: " + name);
        }
        return toView(categoryRepository.save(Category.create(name, slug, description, sortOrder)));
    }

    @Transactional
    public CategoryView update(Long id, String name, String slug, String description, int sortOrder) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> DomainException.notFound("分类不存在: " + id));
        if (categoryRepository.existsByName(name, id)) {
            throw DomainException.conflict("分类已存在: " + name);
        }
        category.redefine(name, slug, description, sortOrder);
        return toView(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.findById(id)
                .orElseThrow(() -> DomainException.notFound("分类不存在: " + id));
        ArticleQuery query = new ArticleQuery(null, id, null, null, "latest", 1, 1);
        long inUse = articleRepository.search(query).total();
        if (inUse > 0) {
            throw DomainException.conflict("该分类下仍有关联 " + inUse + " 篇文章，无法删除");
        }
        categoryRepository.deleteById(id);
    }

    private CategoryView toView(Category category) {
        ArticleQuery query = new ArticleQuery(null, category.id(), null, Article.Status.PUBLISHED, "latest", 1, 1);
        long count = articleRepository.search(query).total();
        return new CategoryView(category.id(), category.name(), category.slug(), category.description(), count);
    }
}
