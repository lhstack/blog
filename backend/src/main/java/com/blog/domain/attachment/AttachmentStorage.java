package com.blog.domain.attachment;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentStorage {
    String store(UUID id, InputStream input);
    Optional<Path> find(UUID id);
    void remove(UUID id);
}
