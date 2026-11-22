package com.blog.domain.attachment;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AttachmentPaths {
    public static final String STORAGE_PREFIX = "attachments/";
    public static final String PUBLIC_STORAGE_PREFIX = STORAGE_PREFIX + "public/";
    public static final String AVATAR_STORAGE_PREFIX = STORAGE_PREFIX + "avatars/";
    public static final String PUBLIC = "/media/";
    public static final String SITE_AVATAR = PUBLIC + "site-avatar/";
    public static final String PROFILE_AVATAR = PUBLIC + "profile-avatar/";
    public static final String FILE_PATTERN = "[a-z0-9][a-z0-9-]{0,47}-[0-9a-f]{16}\\.[a-z0-9]{2,4}";
    private static final Pattern FILE = Pattern.compile(FILE_PATTERN);

    private AttachmentPaths() {}

    public static boolean isFile(String filename) {
        return FILE.matcher(filename).matches();
    }

    public static boolean isStoragePath(String filePath) {
        String filename = storageFilename(filePath);
        if (!isFile(filename)) return false;
        String prefix = filePath.substring(0, filePath.length() - filename.length());
        return STORAGE_PREFIX.equals(prefix) || PUBLIC_STORAGE_PREFIX.equals(prefix) || AVATAR_STORAGE_PREFIX.equals(prefix);
    }

    public static String storageFilename(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    public static String filename(String name, String suffix, String mediaType) {
        int dot = name.lastIndexOf('.');
        String stem = (dot > 0 ? name.substring(0, dot) : name).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (stem.isEmpty()) stem = "file";
        return stem.substring(0, Math.min(stem.length(), 48)) + '-' + suffix + '.' + switch (mediaType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "image/avif" -> "avif";
            case "audio/wav" -> "wav";
            case "audio/mpeg" -> "mp3";
            case "audio/mp4" -> "m4a";
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            default -> "bin";
        };
    }
}
