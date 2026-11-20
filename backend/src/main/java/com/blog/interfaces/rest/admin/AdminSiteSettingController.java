package com.blog.interfaces.rest.admin;

import com.blog.application.setting.SettingViews.SiteSettingView;
import com.blog.application.setting.SiteSettingApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-站点设置", description = "备案信息等站点运营信息")
@RestController
@RequestMapping("/api/admin/settings/site")
public class AdminSiteSettingController {

    private final SiteSettingApplicationService settingService;

    public AdminSiteSettingController(SiteSettingApplicationService settingService) {
        this.settingService = settingService;
    }

    @Operation(summary = "站点运营信息")
    @GetMapping
    public SiteSettingView current() {
        return settingService.current();
    }

    @Operation(summary = "更新站点运营信息", description = "空白串表示不展示该项")
    @PutMapping
    public SiteSettingView update(@Valid @RequestBody SiteSettingRequest request) {
        return settingService.update(request.icpNumber(), request.policeNumber(), request.policeUrl());
    }
}

record SiteSettingRequest(
        @Size(max = 60, message = "ICP 备案号不能超过 60 个字符") String icpNumber,
        @Size(max = 60, message = "公安备案号不能超过 60 个字符") String policeNumber,
        @Size(max = 200, message = "公安备案链接不能超过 200 个字符") String policeUrl) {
}