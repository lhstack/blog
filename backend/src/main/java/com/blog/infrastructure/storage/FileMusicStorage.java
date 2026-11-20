package com.blog.infrastructure.storage;

import com.blog.domain.common.DomainException;
import com.blog.domain.setting.MusicStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Component
public class FileMusicStorage implements MusicStorage {
    private static final int[] MPEG1_LAYER3_BITRATES = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0};
    private static final int[] MPEG2_LAYER3_BITRATES = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0};
    private static final int[] MPEG1_SAMPLE_RATES = {44100, 48000, 32000};

    private final Path directory;

    public FileMusicStorage(@Value("${blog.data-dir}") String dataDir) {
        this.directory = Path.of(dataDir).toAbsolutePath().normalize().resolve("background-music");
    }

    @Override
    public void store(UUID id, InputStream source) {
        try {
            Files.createDirectories(directory);
            Path upload = Files.createTempFile(directory, "upload-", ".tmp");
            try {
                Files.copy(source, upload, StandardCopyOption.REPLACE_EXISTING);
                if (!hasMp3Frames(upload)) {
                    throw DomainException.invalid("文件不是有效的 MP3 音频");
                }
                Files.move(upload, path(id));
            } finally {
                Files.deleteIfExists(upload);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("保存背景音乐失败", ex);
        }
    }

    @Override
    public Optional<Path> find(UUID id) {
        Path file = path(id);
        return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) ? Optional.of(file) : Optional.empty();
    }

    @Override
    public void remove(UUID id) {
        try {
            Files.deleteIfExists(path(id));
        } catch (IOException ex) {
            throw new UncheckedIOException("清理背景音乐文件失败", ex);
        }
    }

    private Path path(UUID id) {
        return directory.resolve(id + ".mp3");
    }

    // 跳过可选的 ID3v2 标签，检查前两个连续的 MPEG Layer III 帧；不信任浏览器提供的 MIME 类型。
    private static boolean hasMp3Frames(Path file) throws IOException {
        try (RandomAccessFile audio = new RandomAccessFile(file.toFile(), "r")) {
            if (audio.length() < 10) {
                return false;
            }
            long start = audioStart(audio);
            if (start < 0 || start + 4 > audio.length()) {
                return false;
            }
            audio.seek(start);
            int first = audio.readInt();
            int length = frameLength(first);
            long next = start + length;
            if (length == 0 || next + 4 > audio.length()) {
                return false;
            }
            audio.seek(next);
            int second = audio.readInt();
            int secondLength = frameLength(second);
            // 两帧允许不同的比特率，但音频版本和采样率必须一致。
            return secondLength > 0 && next + secondLength <= audio.length()
                    && (first & 0x00180C00) == (second & 0x00180C00);
        }
    }

    private static long audioStart(RandomAccessFile audio) throws IOException {
        if (audio.readUnsignedByte() != 'I' || audio.readUnsignedByte() != 'D' || audio.readUnsignedByte() != '3') {
            return 0;
        }
        int version = audio.readUnsignedByte();
        audio.readUnsignedByte(); // minor version
        int flags = audio.readUnsignedByte();
        int size = 0;
        for (int i = 0; i < 4; i++) {
            int part = audio.readUnsignedByte();
            if (part > 127) {
                return -1;
            }
            size = (size << 7) | part;
        }
        return 10L + size + (version == 4 && (flags & 0x10) != 0 ? 10 : 0);
    }

    private static int frameLength(int header) {
        int version = (header >>> 19) & 3;
        int layer = (header >>> 17) & 3;
        int sampleIndex = (header >>> 10) & 3;
        if ((header & 0xFFE00000) != 0xFFE00000 || version == 1 || layer != 1 || sampleIndex == 3) {
            return 0;
        }
        boolean mpeg1 = version == 3;
        int bitrate = (mpeg1 ? MPEG1_LAYER3_BITRATES : MPEG2_LAYER3_BITRATES)[(header >>> 12) & 15];
        if (bitrate == 0) {
            return 0;
        }
        int sampleRate = MPEG1_SAMPLE_RATES[sampleIndex] >> (mpeg1 ? 0 : version == 2 ? 1 : 2);
        return (mpeg1 ? 144000 : 72000) * bitrate / sampleRate + ((header >>> 9) & 1);
    }
}
