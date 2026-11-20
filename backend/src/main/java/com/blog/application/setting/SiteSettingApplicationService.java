package com.blog.application.setting;

import com.blog.domain.setting.SiteSetting;
import com.blog.domain.setting.SiteSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站点运营信息用例：读取与更新备案信息。
 */
@Service
public class SiteSettingApplicationService {

    private final SiteSettingRepository repository;

    public SiteSettingApplicationService(SiteSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SettingViews.SiteSettingView current() {
        return repository.find().map(this::toView).orElseGet(SiteSettingApplicationService::emptyView);
    }

    @Transactional
    public SettingViews.SiteSettingView update(String icpNumber, String policeNumber, String policeUrl) {
        SiteSetting setting = repository.find().orElseGet(SiteSetting::empty);
        setting.revise(icpNumber, policeNumber, policeUrl);
        return toView(repository.save(setting));
    }

    private SettingViews.SiteSettingView toView(SiteSetting setting) {
        return new SettingViews.SiteSettingView(setting.icpNumber(), setting.policeNumber(),
                setting.policeUrl(), setting.updatedAt());
    }

    private static SettingViews.SiteSettingView emptyView() {
        SiteSetting empty = SiteSetting.empty();
        return new SettingViews.SiteSettingView(empty.icpNumber(), empty.policeNumber(),
                empty.policeUrl(), empty.updatedAt());
    }
}