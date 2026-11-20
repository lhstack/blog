package com.blog.infrastructure.persistence.mapper;

import com.blog.domain.setting.SiteAvatarRepository;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
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
    @Select("SELECT media_type, content FROM blog_site_avatar WHERE id = 1 AND location = #{location} AND content IS NOT NULL")
    @ConstructorArgs({@Arg(column = "media_type", javaType = String.class), @Arg(column = "content", javaType = byte[].class)})
    AvatarImage findImage(@Param("location") String location);

    @Override
    @Insert("""
            INSERT INTO blog_site_avatar (id, location, media_type, content)
            VALUES (1, #{location,jdbcType=VARCHAR}, #{image.mediaType,jdbcType=VARCHAR}, #{image.content,jdbcType=BLOB})
            ON CONFLICT(id) DO UPDATE SET location = excluded.location, media_type = excluded.media_type, content = excluded.content
            """)
    void save(@Param("location") String location, @Param("image") AvatarImage image);
}
