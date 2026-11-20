package com.blog.application.setting;

import java.time.LocalDateTime;

/**
 * 站点运营信息读模型。
 */
public final class SettingViews {

    private SettingViews() {
    }

    public record SiteSettingView(
            String icpNumber,
            String policeNumber,
            String policeUrl,
            LocalDateTime updatedAt) {
    }
}