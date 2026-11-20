package com.blog.domain.tag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TagRepository {

    List<Tag> findAll();

    List<Tag> findByIds(Collection<Long> ids);

    Optional<Tag> findById(Long id);

    Optional<Tag> findByName(String name);

    Tag save(Tag tag);

    void deleteById(Long id);
}
