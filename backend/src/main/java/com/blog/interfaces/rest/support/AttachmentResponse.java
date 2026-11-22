package com.blog.interfaces.rest.support;

import com.blog.application.attachment.AttachmentApplicationService.Download;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

public final class AttachmentResponse {
    private AttachmentResponse() {}

    public static ResponseEntity<Resource> of(Download download) {
        return of(download, CacheControl.noStore());
    }

    public static ResponseEntity<Resource> of(Download download, CacheControl cacheControl) {
        var file = download.attachment();
        var disposition = file.kind().equals("download") ? ContentDisposition.attachment() : ContentDisposition.inline();
        return ResponseEntity.ok().cacheControl(cacheControl)
                .contentType(MediaType.parseMediaType(file.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(new FileSystemResource(download.path()));
    }
}
