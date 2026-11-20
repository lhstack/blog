package com.blog.infrastructure.persistence.mapper;

import com.blog.domain.setting.MusicTrack;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SitePresentationMapper {
    @Select("SELECT typewriter_text FROM blog_site_presentation WHERE id = 1")
    String typewriterText();

    @Select("SELECT markdown FROM blog_site_about WHERE id = 1")
    String aboutMarkdown();

    @Insert("""
            INSERT INTO blog_site_about (id, markdown) VALUES (1, #{markdown})
            ON CONFLICT(id) DO UPDATE SET markdown = excluded.markdown
            """)
    void saveAboutMarkdown(@Param("markdown") String markdown);

    @Select("SELECT id, name FROM blog_background_music ORDER BY sort_order, id")
    @ConstructorArgs({@Arg(column = "id", javaType = String.class), @Arg(column = "name", javaType = String.class)})
    List<MusicTrack> tracks();

    // 容量检查与写入是同一条语句，并发上传也不能突破限制。
    @Insert("""
            INSERT INTO blog_background_music (id, name, sort_order)
            SELECT #{track.id}, #{track.name}, COALESCE(MAX(sort_order), 0) + 1
            FROM blog_background_music HAVING COUNT(*) < #{limit}
            """)
    int addTrack(@Param("track") MusicTrack track, @Param("limit") int limit);

    // 只在提交的 ID 恰好覆盖当前列表时更新，避免覆盖其他管理窗口刚上传或移除的曲目。
    @Update("""
            <script>
            UPDATE blog_background_music SET sort_order = CASE id
            <foreach collection="ids" item="id" index="position">
              WHEN #{id} THEN (#{position} + 1)
            </foreach>
            END
            WHERE (SELECT COUNT(*) FROM blog_background_music) = #{size}
              AND NOT EXISTS (SELECT 1 FROM blog_background_music WHERE id NOT IN
                <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
              )
            </script>
            """)
    int reorderTracks(@Param("ids") List<String> ids, @Param("size") int size);

    @Update("UPDATE blog_background_music SET name = #{name} WHERE id = #{id}")
    int renameTrack(@Param("id") String id, @Param("name") String name);

    @Select("SELECT EXISTS (SELECT 1 FROM blog_background_music WHERE id = #{id})")
    boolean hasTrack(@Param("id") String id);

    @Delete("DELETE FROM blog_background_music WHERE id = #{id}")
    int removeTrack(@Param("id") String id);
}
