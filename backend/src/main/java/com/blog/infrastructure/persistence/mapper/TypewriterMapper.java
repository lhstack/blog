package com.blog.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface TypewriterMapper {
    @Select("SELECT entries FROM blog_typewriter_config WHERE id = 1")
    String find();

    @Insert("""
            INSERT INTO blog_typewriter_config (id, entries) VALUES (1, #{entries})
            ON CONFLICT(id) DO UPDATE SET entries = excluded.entries
            """)
    void save(@Param("entries") String entries);
}
