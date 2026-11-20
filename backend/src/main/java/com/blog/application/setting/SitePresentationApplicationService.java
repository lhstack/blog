package com.blog.application.setting;

import com.blog.domain.common.DomainException;
import com.blog.domain.setting.MusicStorage;
import com.blog.domain.setting.MusicTrack;
import com.blog.domain.setting.SitePresentationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SitePresentationApplicationService {
    public static final int TRACK_LIMIT = 10;
    public static final int TRACK_NAME_LIMIT = 120;
    public static final int ABOUT_LIMIT = 50_000;
    public static final long TRACK_SIZE_LIMIT = 20L * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(SitePresentationApplicationService.class);
    private final SitePresentationRepository repository;
    private final MusicStorage storage;

    public SitePresentationApplicationService(SitePresentationRepository repository, MusicStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public PresentationView current() {
        return new PresentationView(tracks());
    }

    public AboutView about() {
        String markdown = repository.aboutMarkdown();
        return new AboutView(markdown == null ? "" : markdown);
    }

    public AboutView updateAbout(String markdown) {
        if (markdown == null || markdown.length() > ABOUT_LIMIT) {
            throw DomainException.invalid("关于页正文须提供且不得超过 " + ABOUT_LIMIT + " 个字符");
        }
        // 保留 Markdown 的缩进、空行与换行，不对正文做 trim。
        repository.saveAboutMarkdown(markdown);
        return new AboutView(markdown);
    }

    public List<TrackView> tracks() {
        return repository.tracks().stream().map(SitePresentationApplicationService::toView).toList();
    }

    public TrackView addTrack(String originalName, String displayName, long size, InputStream source) {
        if (size <= 0 || size > TRACK_SIZE_LIMIT) {
            throw DomainException.invalid("请上传不超过 20 MB 的 MP3 文件");
        }
        String fallbackName = trackName(originalName);
        String name = displayName == null || displayName.isBlank() ? fallbackName : validateTrackName(displayName);
        UUID id = UUID.randomUUID();
        MusicTrack track = new MusicTrack(id.toString(), name);
        storage.store(id, source);
        try {
            if (!repository.addTrack(track, TRACK_LIMIT)) {
                throw DomainException.conflict("最多配置 10 首背景音乐，请先移除一首");
            }
        } catch (RuntimeException failure) {
            try {
                storage.remove(id);
            } catch (UncheckedIOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
                log.warn("清理未加入播放列表的文件失败：{}", id, cleanupFailure);
            }
            throw failure;
        }
        return toView(track);
    }

    public List<TrackView> reorderTracks(List<UUID> order) {
        if (order.size() > TRACK_LIMIT || new HashSet<>(order).size() != order.size()) {
            throw DomainException.invalid("播放顺序必须包含每首曲目且不能重复");
        }
        if (!repository.reorderTracks(order.stream().map(UUID::toString).toList())) {
            throw DomainException.conflict("播放列表已变化，请重新载入后再排序");
        }
        return tracks();
    }

    public TrackView renameTrack(UUID id, String displayName) {
        String name = validateTrackName(displayName);
        if (!repository.renameTrack(id.toString(), name)) {
            throw DomainException.notFound("背景音乐不存在");
        }
        return new TrackView(id.toString(), name);
    }

    public void removeTrack(UUID id) {
        if (!repository.removeTrack(id.toString())) {
            throw DomainException.notFound("背景音乐不存在");
        }
        // 先提交列表变更；即使物理文件清理遇到 I/O 错误，也不再对外提供该曲目。
        try {
            storage.remove(id);
        } catch (UncheckedIOException ex) {
            log.warn("播放列表已移除曲目，但文件清理失败：{}", id, ex);
        }
    }

    public Path audioFile(UUID id) {
        if (!repository.hasTrack(id.toString())) {
            throw DomainException.notFound("背景音乐不存在");
        }
        return storage.find(id).orElseThrow(() -> DomainException.notFound("音频文件不存在"));
    }

    private static String trackName(String filename) {
        String name = filename == null ? "" : filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).strip();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
            throw DomainException.invalid("背景音乐仅支持 MP3 文件");
        }
        String title = name.substring(0, name.length() - 4).replaceAll("[\\p{Cc}\\p{Cf}]", "").strip();
        if (title.isEmpty()) {
            return "未命名曲目";
        }
        return title.substring(0, title.offsetByCodePoints(0, Math.min(TRACK_NAME_LIMIT, title.codePointCount(0, title.length()))));
    }

    private static String validateTrackName(String displayName) {
        String name = displayName.replaceAll("[\\p{Cc}]", "").strip();
        if (name.isEmpty() || name.codePointCount(0, name.length()) > TRACK_NAME_LIMIT) {
            throw DomainException.invalid("曲名必须为 1 至 120 个字符");
        }
        return name;
    }

    private static TrackView toView(MusicTrack track) {
        return new TrackView(track.id(), track.name());
    }

    public record PresentationView(List<TrackView> tracks) {}
    public record AboutView(String markdown) {}
    public record TrackView(String id, String name) {}
}
