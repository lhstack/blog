package com.blog.domain.attachment;

import java.time.LocalDateTime;

public record Attachment(String id, String name, String mediaType, long size, LocalDateTime createdAt) {
    public static final String URL_PREFIX = "/api/public/attachments/";

    public String kind() {
        int slash = mediaType.indexOf('/');
        return switch (mediaType.substring(0, slash)) {
            case "image", "audio", "video" -> mediaType.substring(0, slash);
            default -> "download";
        };
    }
}
