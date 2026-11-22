package com.blog.interfaces.rest.publicapi;

import com.blog.application.setting.SiteAvatarApplicationService;
import com.blog.domain.attachment.AttachmentPaths;
import com.blog.interfaces.rest.support.AttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class PublicSiteAvatarController {
    private final SiteAvatarApplicationService service;

    public PublicSiteAvatarController(SiteAvatarApplicationService service) {
        this.service = service;
    }

    @GetMapping(AttachmentPaths.SITE_AVATAR + "{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        return AttachmentResponse.of(service.image(filename), CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic());
    }
}
