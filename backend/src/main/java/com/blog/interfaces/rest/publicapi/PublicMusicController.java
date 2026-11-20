package com.blog.interfaces.rest.publicapi;

import com.blog.application.setting.SitePresentationApplicationService;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/music")
public class PublicMusicController {
    private final SitePresentationApplicationService service;

    public PublicMusicController(SitePresentationApplicationService service) {
        this.service = service;
    }

    @GetMapping(value = "/{id}", produces = "audio/mpeg")
    public ResponseEntity<Resource> audio(@PathVariable UUID id) {
        // 使用文件资源交给框架处理 Range 请求，不把整首音频加载到内存。
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new PathResource(service.audioFile(id)));
    }
}
