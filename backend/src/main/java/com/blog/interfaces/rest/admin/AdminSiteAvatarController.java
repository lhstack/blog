package com.blog.interfaces.rest.admin;

import com.blog.application.setting.SiteAvatarApplicationService;
import com.blog.application.setting.SiteAvatarApplicationService.AvatarView;
import com.blog.domain.common.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/settings/site/avatar")
public class AdminSiteAvatarController {
    private final SiteAvatarApplicationService service;

    public AdminSiteAvatarController(SiteAvatarApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public AvatarView current() {
        return service.current();
    }

    @PutMapping
    public AvatarView updateUrl(@Valid @RequestBody AvatarRequest request) {
        return service.updateUrl(request.url());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AvatarView upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.getSize() > SiteAvatarApplicationService.MAX_SIZE) {
            throw DomainException.invalid("头像不能超过 5 MB");
        }
        return service.upload(file.getBytes());
    }

    record AvatarRequest(@Size(max = SiteAvatarApplicationService.URL_LIMIT) String url) {}
}
