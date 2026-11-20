package com.blog.domain.setting;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public interface MusicStorage {
    void store(UUID id, InputStream source);

    Optional<Path> find(UUID id);

    void remove(UUID id);
}
