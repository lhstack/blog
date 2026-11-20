package com.blog.domain.category;

import com.blog.domain.common.DomainException;

/**
 * 分类聚合根。
 */
public class Category {

    private final Long id;
    private String name;
    private String slug;
    private String description;
    private int sortOrder;

    public Category(Long id, String name, String slug, String description, int sortOrder) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public static Category create(String name, String slug, String description, int sortOrder) {
        validate(name, slug);
        return new Category(null, name.trim(), slugOrDerive(slug, name), description, sortOrder);
    }

    public void redefine(String name, String slug, String description, int sortOrder) {
        validate(name, slug);
        this.name = name.trim();
        this.slug = slugOrDerive(slug, name);
        this.description = description;
        this.sortOrder = sortOrder;
    }

    private static void validate(String name, String slug) {
        if (name == null || name.isBlank()) {
            throw DomainException.invalid("分类名称不能为空");
        }
    }

    private static String slugOrDerive(String slug, String name) {
        if (slug != null && !slug.isBlank()) {
            return slug.trim().toLowerCase();
        }
        return name.trim().toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-");
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String slug() {
        return slug;
    }

    public String description() {
        return description;
    }

    public int sortOrder() {
        return sortOrder;
    }
}
