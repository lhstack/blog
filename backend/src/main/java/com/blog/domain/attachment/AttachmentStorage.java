package com.blog.domain.attachment;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public interface AttachmentStorage {
    StoredFile store(String name, InputStream input, boolean avatar);
    Optional<Path> find(String filePath);
    void remove(String filePath);

    record StoredFile(String filePath, String mediaType) {}
}
