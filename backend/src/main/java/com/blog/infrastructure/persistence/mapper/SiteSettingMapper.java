package com.blog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.infrastructure.persistence.po.SiteSettingPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SiteSettingMapper extends BaseMapper<SiteSettingPo> {
}