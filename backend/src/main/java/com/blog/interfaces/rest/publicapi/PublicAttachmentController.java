package com.blog.interfaces.rest.publicapi;

import com.blog.application.attachment.AttachmentApplicationService;
import com.blog.domain.attachment.AttachmentPaths;
import com.blog.interfaces.rest.support.AttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicAttachmentController {
    private final AttachmentApplicationService service;

    public PublicAttachmentController(AttachmentApplicationService service) { this.service = service; }

    @GetMapping(AttachmentPaths.PUBLIC + "{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        return AttachmentResponse.of(service.download(filename, false));
    }
}
