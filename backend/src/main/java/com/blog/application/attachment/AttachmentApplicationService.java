package com.blog.application.attachment;

import com.blog.domain.attachment.Attachment;
import com.blog.domain.attachment.AttachmentRepository;
import com.blog.domain.attachment.AttachmentStorage;
import com.blog.domain.common.DomainException;
import com.blog.domain.common.DateRange;
import com.blog.domain.common.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AttachmentApplicationService {
    private static final Logger log = LoggerFactory.getLogger(AttachmentApplicationService.class);
    private static final int PAGE_SIZE = 20;
    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private final AttachmentRepository repository;
    private final AttachmentStorage storage;

    public AttachmentApplicationService(AttachmentRepository repository, AttachmentStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public PageResult<AttachmentView> list(int page, String kind, String keyword, DateRange dates) {
        int safePage = Math.max(1, page);
        if (kind != null && !java.util.Set.of("image", "audio", "video", "download").contains(kind)) {
            throw DomainException.invalid("附件类型不正确");
        }
        var query = new com.blog.domain.attachment.AttachmentQuery(kind, keyword == null || keyword.isBlank() ? null : keyword.strip(), dates);
        return PageResult.of(repository.list(query, PAGE_SIZE, (safePage - 1L) * PAGE_SIZE).stream().map(AttachmentView::of).toList(),
                repository.count(query), safePage, PAGE_SIZE);
    }

    public AttachmentView upload(String filename, long size, InputStream input) {
        if (size <= 0 || size > MAX_BYTES) throw DomainException.invalid("请选择不超过 20 MB 的文件");
        String path = filename == null ? "" : filename.replace('\\', '/');
        String name = validateName(path.substring(path.lastIndexOf('/') + 1));
        UUID id = UUID.randomUUID();
        var attachment = new Attachment(id.toString(), name, storage.store(id, input), size, LocalDateTime.now());
        try {
            repository.insert(attachment);
        } catch (RuntimeException failure) {
            try { storage.remove(id); } catch (UncheckedIOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
        return AttachmentView.of(attachment);
    }

    @Transactional
    public AttachmentView rename(UUID id, String name) {
        if (!repository.rename(id.toString(), validateName(name))) throw notFound();
        return AttachmentView.of(repository.find(id.toString()).orElseThrow(AttachmentApplicationService::notFound));
    }

    public void delete(UUID id) {
        if (!repository.deleteIfUnused(id.toString())) {
            if (repository.find(id.toString()).isEmpty()) throw notFound();
            throw DomainException.conflict("附件已被文章引用，请先解除引用再删除");
        }
        try { storage.remove(id); }
        catch (UncheckedIOException ex) { log.warn("附件记录已删除，但磁盘清理失败：{}", id, ex); }
    }

    public Download download(UUID id, String kind, boolean admin) {
        var file = (admin ? repository.find(id.toString()) : repository.findPublished(id.toString()))
                .filter(entry -> entry.kind().equals(kind)).orElseThrow(AttachmentApplicationService::notFound);
        return new Download(file, storage.find(id).orElseThrow(AttachmentApplicationService::notFound));
    }

    private static String validateName(String input) {
        String name = input == null ? "" : input.strip();
        if (name.isEmpty() || name.length() > 255 || name.codePoints()
                .anyMatch(character -> Character.isISOControl(character) || character == '/' || character == '\\')) {
            throw DomainException.invalid("附件名称须为 1–255 个字符且不能含控制字符或路径分隔符");
        }
        return name;
    }

    private static DomainException notFound() { return DomainException.notFound("附件不存在"); }

    public record Download(Attachment attachment, Path path) {}
    public record AttachmentView(String id, String name, String mediaType, String kind, String url, long size, LocalDateTime createdAt) {
        static AttachmentView of(Attachment file) {
            return new AttachmentView(file.id(), file.name(), file.mediaType(), file.kind(),
                    Attachment.URL_PREFIX + file.id() + '/' + file.kind(), file.size(), file.createdAt());
        }
    }
}
