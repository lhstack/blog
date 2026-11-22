package com.blog.infrastructure.persistence.attachment;

import com.blog.domain.attachment.Attachment;
import com.blog.domain.attachment.AttachmentPaths;
import com.blog.domain.attachment.AttachmentQuery;
import com.blog.domain.attachment.AttachmentRepository;
import com.blog.domain.common.DomainException;
import com.blog.infrastructure.persistence.mapper.AttachmentMapper;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public class AttachmentRepositoryImpl implements AttachmentRepository {
    private static final Pattern REFERENCE = Pattern.compile(Pattern.quote(AttachmentPaths.PUBLIC)
            + "(" + AttachmentPaths.FILE_PATTERN + ")(?![\\w/.-])");
    private final AttachmentMapper mapper;

    public AttachmentRepositoryImpl(AttachmentMapper mapper) { this.mapper = mapper; }

    public List<Attachment> list(AttachmentQuery query, int limit, long offset) {
        return mapper.list(query.kind(), query.keyword(), start(query), end(query), limit, offset);
    }

    public long count(AttachmentQuery query) {
        return mapper.count(query.kind(), query.keyword(), start(query), end(query));
    }

    private static String start(AttachmentQuery query) {
        return query.dates().from() == null ? null : query.dates().from().toString();
    }

    private static String end(AttachmentQuery query) {
        return query.dates().to() == null ? null : query.dates().until().toString();
    }
    public Optional<Attachment> find(String id) { return Optional.ofNullable(mapper.find(id)); }
    public Optional<Attachment> findByFilename(String filename) {
        return Optional.ofNullable(mapper.findByFilename(AttachmentPaths.PUBLIC_STORAGE_PREFIX + filename,
                AttachmentPaths.AVATAR_STORAGE_PREFIX + filename, AttachmentPaths.STORAGE_PREFIX + filename));
    }
    public Optional<Attachment> findPublicByFilename(String filename) {
        return Optional.ofNullable(mapper.findPublicByFilename(AttachmentPaths.PUBLIC_STORAGE_PREFIX + filename,
                AttachmentPaths.STORAGE_PREFIX + filename, AttachmentPaths.SITE_AVATAR + filename, AttachmentPaths.PROFILE_AVATAR + filename));
    }
    public void insert(Attachment attachment) { mapper.insert(attachment); }
    public boolean rename(String id, String name) { return mapper.rename(id, name) == 1; }
    public boolean deleteIfUnused(String id, String filename) {
        return mapper.deleteIfUnused(id, AttachmentPaths.SITE_AVATAR + filename, AttachmentPaths.PROFILE_AVATAR + filename) == 1;
    }

    public void replaceArticleReferences(Long articleId, String cover, String content) {
        mapper.clearReferences(articleId);
        var seen = new HashSet<String>();
        var references = REFERENCE.matcher((cover == null ? "" : cover) + '\n' + content);
        while (references.find()) {
            String filename = references.group(1);
            if (!seen.add(filename)) continue;
            Attachment attachment = findByFilename(filename)
                    .filter(file -> !file.filePath().startsWith(AttachmentPaths.AVATAR_STORAGE_PREFIX))
                    .orElseThrow(() -> DomainException.invalid("正文或封面引用了不存在的普通附件"));
            mapper.addReference(articleId, attachment.id());
        }
    }
}
