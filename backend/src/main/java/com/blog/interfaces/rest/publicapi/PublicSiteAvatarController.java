package com.blog.interfaces.rest.publicapi;

import com.blog.application.setting.SiteAvatarApplicationService;
import com.blog.domain.setting.SiteAvatarRepository.AvatarImage;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PublicSiteAvatarController {
    private final SiteAvatarApplicationService service;

    public PublicSiteAvatarController(SiteAvatarApplicationService service) {
        this.service = service;
    }

    @GetMapping("/api/public/site/avatar/{id}")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        AvatarImage image = service.image(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.mediaType()))
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.content());
    }
}
