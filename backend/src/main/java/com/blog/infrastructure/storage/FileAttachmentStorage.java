package com.blog.infrastructure.storage;

import com.blog.domain.attachment.AttachmentStorage;
import com.blog.domain.common.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Component
public class FileAttachmentStorage implements AttachmentStorage {
    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private final Path directory;

    public FileAttachmentStorage(@Value("${blog.data-dir}") String dataDir) {
        directory = Path.of(dataDir).toAbsolutePath().normalize().resolve("attachments");
    }

    @Override
    public String store(UUID id, InputStream input) {
        try {
            Files.createDirectories(directory);
            Path temp = Files.createTempFile(directory, "upload-", ".tmp");
            try {
                long size = Files.copy(new BoundedInput(input), temp, StandardCopyOption.REPLACE_EXISTING);
                if (size == 0 || size > MAX_BYTES) throw DomainException.invalid("请选择不超过 20 MB 的文件");
                String type;
                try (var head = Files.newInputStream(temp)) { type = mediaType(head.readNBytes(32)); }
                Files.move(temp, path(id));
                return type;
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("保存附件失败", ex);
        }
    }

    @Override
    public Optional<Path> find(UUID id) {
        Path path = path(id);
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) ? Optional.of(path) : Optional.empty();
    }

    @Override
    public void remove(UUID id) {
        try { Files.deleteIfExists(path(id)); }
        catch (IOException ex) { throw new UncheckedIOException("清理附件失败", ex); }
    }

    private Path path(UUID id) { return directory.resolve(id.toString()); }

    private static String mediaType(byte[] head) {
        if (starts(head, 0, 0x89, 'P', 'N', 'G', 13, 10, 26, 10)) return "image/png";
        if (starts(head, 0, 0xff, 0xd8, 0xff)) return "image/jpeg";
        if (starts(head, 0, 'G', 'I', 'F', '8') && (starts(head, 4, '7', 'a') || starts(head, 4, '9', 'a'))) return "image/gif";
        if (starts(head, 0, 'R', 'I', 'F', 'F') && starts(head, 8, 'W', 'E', 'B', 'P')) return "image/webp";
        if (starts(head, 0, 'R', 'I', 'F', 'F') && starts(head, 8, 'W', 'A', 'V', 'E')) return "audio/wav";
        if (starts(head, 0, 'I', 'D', '3') || (head.length >= 4 && (head[0] & 255) == 255 && (head[1] & 0xe6) == 0xe2)) return "audio/mpeg";
        if (starts(head, 4, 'f', 't', 'y', 'p')) {
            if (starts(head, 8, 'M', '4', 'A', ' ')) return "audio/mp4";
            if (starts(head, 8, 'a', 'v', 'i', 'f') || starts(head, 8, 'a', 'v', 'i', 's')) return "image/avif";
            return "video/mp4";
        }
        if (starts(head, 0, 0x1a, 0x45, 0xdf, 0xa3)) return "video/webm";
        return "application/octet-stream";
    }

    private static boolean starts(byte[] head, int offset, int... signature) {
        if (head.length < offset + signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((head[offset + i] & 255) != signature[i]) return false;
        }
        return true;
    }

    private static final class BoundedInput extends InputStream {
        private final InputStream source;
        private long remaining = MAX_BYTES + 1;
        BoundedInput(InputStream source) { this.source = source; }
        public int read() throws IOException {
            if (remaining == 0) return -1;
            int value = source.read();
            if (value != -1) remaining--;
            return value;
        }
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (length == 0) return 0;
            if (remaining == 0) return -1;
            int count = source.read(bytes, offset, (int) Math.min(remaining, length));
            if (count > 0) remaining -= count;
            return count;
        }
    }
}
