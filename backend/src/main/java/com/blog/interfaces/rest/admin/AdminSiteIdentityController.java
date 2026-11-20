package com.blog.interfaces.rest.admin;

import com.blog.application.setting.SiteIdentityApplicationService;
import com.blog.application.setting.SiteIdentityApplicationService.IdentityView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings/site/identity")
public class AdminSiteIdentityController {
    private final SiteIdentityApplicationService service;

    public AdminSiteIdentityController(SiteIdentityApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public IdentityView current() {
        return service.current();
    }

    @PutMapping
    public IdentityView update(@RequestBody IdentityView request) {
        return service.update(request.name(), request.title(), request.description());
    }
}
