package com.blog.domain.attachment;

import java.time.LocalDateTime;

public record Attachment(String id, String name, String mediaType, long size, LocalDateTime createdAt, String filePath) {
    public String filename() {
        return AttachmentPaths.storageFilename(filePath);
    }

    public String kind() {
        String type = mediaType.substring(0, mediaType.indexOf('/'));
        return switch (type) {
            case "image", "audio", "video" -> type;
            default -> "download";
        };
    }
}
