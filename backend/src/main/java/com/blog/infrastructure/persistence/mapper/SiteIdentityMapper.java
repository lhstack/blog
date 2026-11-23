package com.blog.infrastructure.persistence.mapper;

import com.blog.domain.setting.SiteIdentityRepository;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SiteIdentityMapper extends SiteIdentityRepository {
    @Override
    @Select("SELECT name, title, description FROM blog_site_identity WHERE id = 1")
    Identity find();

    @Override
    @Insert("""
            INSERT INTO blog_site_identity (id, name, title, description) VALUES (1, #{name}, #{title}, #{description})
            ON CONFLICT(id) DO UPDATE SET name = excluded.name, title = excluded.title, description = excluded.description
            """)
    @Insert(value = """
            INSERT INTO blog_site_identity (id, name, title, description) VALUES (1, #{name}, #{title}, #{description})
            ON DUPLICATE KEY UPDATE name = #{name}, title = #{title}, description = #{description}
            """, databaseId = "mysql")
    void save(Identity identity);
}
