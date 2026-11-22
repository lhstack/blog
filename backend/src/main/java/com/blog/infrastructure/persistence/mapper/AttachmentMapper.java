package com.blog.infrastructure.persistence.mapper;

import com.blog.domain.attachment.Attachment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AttachmentMapper {
    String FILTERS = """
            <where>
                <if test="keyword != null">AND instr(lower(name), lower(#{keyword})) &gt; 0</if>
                <if test="from != null">AND created_at &gt;= #{from}</if>
                <if test="until != null">AND created_at &lt; #{until}</if>
                <choose>
                    <when test="kind == 'download'">
                        AND media_type NOT LIKE 'image/%' AND media_type NOT LIKE 'audio/%' AND media_type NOT LIKE 'video/%'
                    </when>
                    <when test="kind != null">AND media_type LIKE (#{kind} || '/%')</when>
                </choose>
            </where>
            """;

    String FIELDS = "a.id, a.name, a.media_type, a.size, a.created_at, a.file_path";
    String TABLE = " FROM blog_attachment a";

    @Select("<script>SELECT " + FIELDS + TABLE + FILTERS + " ORDER BY a.created_at DESC, a.id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<Attachment> list(@Param("kind") String kind, @Param("keyword") String keyword,
                          @Param("from") String from, @Param("until") String until,
                          @Param("limit") int limit, @Param("offset") long offset);

    @Select("<script>SELECT COUNT(*)" + TABLE + FILTERS + "</script>")
    long count(@Param("kind") String kind, @Param("keyword") String keyword,
               @Param("from") String from, @Param("until") String until);

    @Select("SELECT " + FIELDS + TABLE + " WHERE a.id = #{id}")
    Attachment find(@Param("id") String id);

    @Select("SELECT " + FIELDS + TABLE + " WHERE a.file_path IN (#{publicPath}, #{avatarPath}, #{existingPath}) LIMIT 1")
    Attachment findByFilename(@Param("publicPath") String publicPath, @Param("avatarPath") String avatarPath,
                              @Param("existingPath") String existingPath);

    @Select("SELECT " + FIELDS + TABLE + " " + """
            WHERE a.file_path = #{publicPath}
               OR (a.file_path = #{existingPath}
                   AND EXISTS (
                       SELECT 1 FROM blog_article_attachment r JOIN blog_article p ON p.id = r.article_id
                       WHERE r.attachment_id = a.id AND p.status = 'PUBLISHED'
                   )
                   AND NOT EXISTS (SELECT 1 FROM blog_site_avatar WHERE location = #{siteAvatar})
                   AND NOT EXISTS (SELECT 1 FROM blog_user WHERE avatar = #{profileAvatar}))
            ORDER BY CASE WHEN a.file_path = #{publicPath} THEN 0 ELSE 1 END
            LIMIT 1
            """)
    Attachment findPublicByFilename(@Param("publicPath") String publicPath, @Param("existingPath") String existingPath,
                                    @Param("siteAvatar") String siteAvatar, @Param("profileAvatar") String profileAvatar);

    @Insert("""
            INSERT INTO blog_attachment (id, name, media_type, size, created_at, file_path)
            VALUES (#{id}, #{name}, #{mediaType}, #{size}, #{createdAt}, #{filePath})
            """)
    void insert(Attachment attachment);

    @Update("UPDATE blog_attachment SET name = #{name} WHERE id = #{id}")
    int rename(@Param("id") String id, @Param("name") String name);

    @Delete("""
            DELETE FROM blog_attachment WHERE id = #{id}
            AND NOT EXISTS (SELECT 1 FROM blog_article_attachment WHERE attachment_id = #{id})
            AND NOT EXISTS (SELECT 1 FROM blog_site_avatar WHERE location = #{siteAvatar})
            AND NOT EXISTS (SELECT 1 FROM blog_user WHERE avatar = #{profileAvatar})
            """)
    int deleteIfUnused(@Param("id") String id, @Param("siteAvatar") String siteAvatar, @Param("profileAvatar") String profileAvatar);

    @Delete("DELETE FROM blog_article_attachment WHERE article_id = #{articleId}")
    void clearReferences(@Param("articleId") Long articleId);

    @Insert("INSERT OR IGNORE INTO blog_article_attachment (article_id, attachment_id) VALUES (#{articleId}, #{id})")
    void addReference(@Param("articleId") Long articleId, @Param("id") String id);
}
