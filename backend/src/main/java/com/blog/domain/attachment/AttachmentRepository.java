package com.blog.domain.attachment;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository {
    List<Attachment> list(AttachmentQuery query, int limit, long offset);
    long count(AttachmentQuery query);
    Optional<Attachment> find(String id);
    Optional<Attachment> findPublished(String id);
    void insert(Attachment attachment);
    boolean rename(String id, String name);
    boolean deleteIfUnused(String id);
    void replaceArticleReferences(Long articleId, String cover, String content);
}
