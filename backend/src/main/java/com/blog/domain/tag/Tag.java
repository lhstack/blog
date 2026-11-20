package com.blog.domain.tag;

import com.blog.domain.common.DomainException;

/**
 * 标签聚合根。
 */
public class Tag {

    private final Long id;
    private String name;
    private String color;

    public Tag(Long id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public static Tag create(String name, String color) {
        if (name == null || name.isBlank()) {
            throw DomainException.invalid("标签名称不能为空");
        }
        return new Tag(null, name.trim(), color == null || color.isBlank() ? "blue" : color);
    }

    public void redefine(String name, String color) {
        if (name == null || name.isBlank()) {
            throw DomainException.invalid("标签名称不能为空");
        }
        this.name = name.trim();
        this.color = color == null || color.isBlank() ? this.color : color;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String color() {
        return color;
    }
}
