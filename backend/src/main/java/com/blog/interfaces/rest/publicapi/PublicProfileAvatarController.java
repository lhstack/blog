package com.blog.interfaces.rest.publicapi;

import com.blog.application.auth.AuthApplicationService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PublicProfileAvatarController {
    private final AuthApplicationService service;

    public PublicProfileAvatarController(AuthApplicationService service) { this.service = service; }

    @GetMapping(AuthApplicationService.AVATAR_PATH + "{id}")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        var image = service.avatar(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.mediaType()))
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .body(image.content());
    }
}
