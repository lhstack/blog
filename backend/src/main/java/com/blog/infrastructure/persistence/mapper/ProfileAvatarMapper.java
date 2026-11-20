package com.blog.infrastructure.persistence.mapper;

import com.blog.domain.user.ProfileAvatarRepository;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.ByteArrayTypeHandler;

@Mapper
public interface ProfileAvatarMapper extends ProfileAvatarRepository {
    @Insert("""
            INSERT INTO blog_profile_avatar (user_id, id, media_type, content) VALUES (#{userId}, #{id}, #{mediaType}, #{content})
            ON CONFLICT(user_id) DO UPDATE SET id=excluded.id, media_type=excluded.media_type, content=excluded.content
            """)
    void save(@Param("userId") Long userId, @Param("id") String id, @Param("mediaType") String mediaType, @Param("content") byte[] content);

    @Delete("DELETE FROM blog_profile_avatar WHERE user_id = #{userId}")
    void clear(@Param("userId") Long userId);

    @Select("SELECT media_type, content FROM blog_profile_avatar WHERE id = #{id}")
    @ConstructorArgs({@Arg(column = "media_type", javaType = String.class), @Arg(column = "content", javaType = byte[].class, typeHandler = ByteArrayTypeHandler.class)})
    Image find(@Param("id") String id);
}
