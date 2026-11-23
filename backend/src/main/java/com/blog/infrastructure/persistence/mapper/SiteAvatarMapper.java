package com.blog.infrastructure.persistence.mapper;

import com.blog.domain.setting.SiteAvatarRepository;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SiteAvatarMapper extends SiteAvatarRepository {
    @Override
    @Select("SELECT location FROM blog_site_avatar WHERE id = 1")
    String location();

    @Override
    @Insert("""
            INSERT INTO blog_site_avatar (id, location)
            VALUES (1, #{location,jdbcType=VARCHAR})
            ON CONFLICT(id) DO UPDATE SET location = excluded.location
            """)
    @Insert(value = """
            INSERT INTO blog_site_avatar (id, location) VALUES (1, #{location,jdbcType=VARCHAR})
            ON DUPLICATE KEY UPDATE location = #{location,jdbcType=VARCHAR}
            """, databaseId = "mysql")
    void save(@Param("location") String location);
}
