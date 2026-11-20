package com.blog.interfaces.rest.admin;

import com.blog.application.site.SiteApplicationService;
import com.blog.application.site.SiteViews.DashboardView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-仪表盘", description = "统计数据")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final SiteApplicationService siteService;

    public AdminDashboardController(SiteApplicationService siteService) {
        this.siteService = siteService;
    }

    @Operation(summary = "仪表盘统计")
    @GetMapping
    public DashboardView dashboard() {
        return siteService.dashboard();
    }
}
