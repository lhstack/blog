package com.blog.domain.category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    List<Category> findAll();

    Optional<Category> findById(Long id);

    Optional<Category> findBySlug(String slug);

    Category save(Category category);

    void deleteById(Long id);

    boolean existsByName(String name, Long excludeId);
}
