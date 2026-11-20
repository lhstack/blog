package com.blog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.infrastructure.persistence.po.ArticlePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章基础 CRUD（MyBatis-Plus）。复杂查询见 {@link ArticleMapper}。
 */
@Mapper
public interface ArticleBaseMapper extends BaseMapper<ArticlePo> {
}
