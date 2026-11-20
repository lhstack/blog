package com.blog.domain.setting;

import java.util.List;
import java.util.Optional;

public interface TypewriterRepository {
    Optional<List<TypewriterEntry>> find();
    void save(List<TypewriterEntry> entries);
}
