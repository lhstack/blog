package com.blog.application.setting;

import com.blog.application.attachment.AttachmentApplicationService.Download;
import com.blog.domain.attachment.AttachmentPaths;
import com.blog.domain.common.DomainException;
import com.blog.domain.setting.SiteAvatarRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

@Service
public class SiteAvatarApplicationService {
    public static final int MAX_SIZE = 5 * 1024 * 1024;
    public static final int URL_LIMIT = 2048;
    private static final int MAX_PIXELS = 16_000_000;

    private final SiteAvatarRepository repository;
    private final AvatarFiles avatarFiles;
    private final String defaultAvatar;

    public SiteAvatarApplicationService(SiteAvatarRepository repository, AvatarFiles avatarFiles,
                                        @Value("${blog.site.avatar}") String defaultAvatar) {
        this.repository = repository;
        this.avatarFiles = avatarFiles;
        this.defaultAvatar = defaultAvatar;
    }

    public AvatarView current() {
        return toView(repository.location());
    }

    public AvatarView updateUrl(String input) {
        String url = input == null ? "" : input.strip();
        if (!url.isEmpty() && !isWebUrl(url)) {
            throw DomainException.invalid("请输入有效的 HTTP(S) 图片 URL");
        }
        String location = url.isEmpty() ? null : url;
        repository.save(location);
        return toView(location);
    }

    @Transactional
    public AvatarView upload(String filename, byte[] content) {
        String location = AttachmentPaths.SITE_AVATAR + avatarFiles.upload(filename, content);
        repository.save(location);
        return toView(location);
    }

    @Transactional(readOnly = true)
    public Download image(String filename) {
        return avatarFiles.image(filename, (AttachmentPaths.SITE_AVATAR + filename).equals(repository.location()));
    }

    private AvatarView toView(String location) {
        return new AvatarView(location == null ? "" : location, location == null ? defaultAvatar : location);
    }

    public static boolean isWebUrl(String url) {
        if (url.length() > URL_LIMIT) return false;
        try {
            URI uri = URI.create(url);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    static void validateImage(byte[] content) {
        if (content.length == 0 || content.length > MAX_SIZE) {
            throw DomainException.invalid("请选择不超过 5 MB 的头像图片");
        }
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidImage();
            var reader = readers.next();
            try {
                switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg", "jpg", "png", "webp" -> { }
                    default -> throw invalidImage();
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw DomainException.invalid("头像不能超过 1600 万像素");
                }
                if (reader.read(0) == null) throw invalidImage();
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException ex) {
            throw invalidImage();
        }
    }

    private static DomainException invalidImage() {
        return DomainException.invalid("请选择有效的 JPEG、PNG 或 WebP 图片");
    }

    public record AvatarView(String location, String avatar) {}
}
