package com.blog.infrastructure.persistence.mapper;

import com.blog.domain.user.ProfileAvatarRepository;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProfileAvatarMapper extends ProfileAvatarRepository {
    @Override
    @Select("SELECT EXISTS (SELECT 1 FROM blog_user WHERE avatar = #{location})")
    boolean isReferenced(@Param("location") String location);
}
