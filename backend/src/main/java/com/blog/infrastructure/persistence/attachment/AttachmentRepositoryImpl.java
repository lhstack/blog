package com.blog.infrastructure.persistence.attachment;

import com.blog.domain.attachment.Attachment;
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
    private static final Pattern REFERENCE = Pattern.compile(Pattern.quote(Attachment.URL_PREFIX)
            + "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/(image|audio|video|download)(?![\\w/-])");
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
    public Optional<Attachment> findPublished(String id) { return Optional.ofNullable(mapper.findPublished(id)); }
    public void insert(Attachment attachment) { mapper.insert(attachment); }
    public boolean rename(String id, String name) { return mapper.rename(id, name) == 1; }
    public boolean deleteIfUnused(String id) { return mapper.deleteIfUnused(id) == 1; }

    public void replaceArticleReferences(Long articleId, String cover, String content) {
        mapper.clearReferences(articleId);
        var seen = new HashSet<String>();
        var references = REFERENCE.matcher((cover == null ? "" : cover) + '\n' + content);
        while (references.find()) {
            String id = references.group(1);
            String kind = references.group(2);
            if (!seen.add(id + "/" + kind)) continue;
            Attachment attachment = find(id).orElseThrow(() -> DomainException.invalid("正文或封面引用了不存在的附件"));
            if (!kind.equals(attachment.kind())) throw DomainException.invalid("附件的引用类型不正确");
            mapper.addReference(articleId, id);
        }
    }
}
