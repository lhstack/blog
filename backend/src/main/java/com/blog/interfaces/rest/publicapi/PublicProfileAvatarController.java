package com.blog.interfaces.rest.publicapi;

import com.blog.application.auth.AuthApplicationService;
import com.blog.domain.attachment.AttachmentPaths;
import com.blog.interfaces.rest.support.AttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicProfileAvatarController {
    private final AuthApplicationService service;

    public PublicProfileAvatarController(AuthApplicationService service) { this.service = service; }

    @GetMapping(AttachmentPaths.PROFILE_AVATAR + "{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        return AttachmentResponse.of(service.avatar(filename));
    }
}
