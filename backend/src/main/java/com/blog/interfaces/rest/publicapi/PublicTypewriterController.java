package com.blog.interfaces.rest.publicapi;

import com.blog.application.setting.TypewriterApplicationService;
import com.blog.application.setting.TypewriterApplicationService.ActiveText;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/typewriter")
public class PublicTypewriterController {
    private final TypewriterApplicationService service;

    public PublicTypewriterController(TypewriterApplicationService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ActiveText> active() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.active());
    }
}
