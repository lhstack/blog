package com.blog.infrastructure.persistence.setting;

import com.blog.domain.setting.SiteSetting;
import com.blog.domain.setting.SiteSettingRepository;
import com.blog.infrastructure.persistence.mapper.SiteSettingMapper;
import com.blog.infrastructure.persistence.po.SiteSettingPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SiteSettingRepositoryImpl implements SiteSettingRepository {

    private final SiteSettingMapper mapper;

    public SiteSettingRepositoryImpl(SiteSettingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<SiteSetting> find() {
        return Optional.ofNullable(mapper.selectById(SiteSetting.SINGLETON_ID)).map(this::toDomain);
    }

    @Override
    public SiteSetting save(SiteSetting setting) {
        SiteSettingPo po = toPo(setting);
        // 单行表：先查存在性再决定 insert / update，避免依赖数据库方言的 upsert 语法。
        if (mapper.selectById(setting.id()) == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    private SiteSetting toDomain(SiteSettingPo po) {
        return new SiteSetting(po.getId(), po.getIcpNumber(), po.getPoliceNumber(), po.getPoliceUrl(), po.getUpdatedAt());
    }

    private SiteSettingPo toPo(SiteSetting setting) {
        SiteSettingPo po = new SiteSettingPo();
        po.setId(setting.id());
        po.setIcpNumber(setting.icpNumber());
        po.setPoliceNumber(setting.policeNumber());
        po.setPoliceUrl(setting.policeUrl());
        po.setUpdatedAt(setting.updatedAt());
        return po;
    }
}