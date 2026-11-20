package com.blog.domain.setting;

import java.util.Optional;

/**
 * 站点运营信息仓储（单例行）。
 */
public interface SiteSettingRepository {

    /** 未初始化时返回 Optional.empty()，由调用方决定如何呈现空状态。 */
    Optional<SiteSetting> find();

    SiteSetting save(SiteSetting setting);
}