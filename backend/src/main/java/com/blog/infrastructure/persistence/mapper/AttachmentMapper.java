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

    @Select("<script>SELECT * FROM blog_attachment " + FILTERS + " ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<Attachment> list(@Param("kind") String kind, @Param("keyword") String keyword,
                          @Param("from") String from, @Param("until") String until,
                          @Param("limit") int limit, @Param("offset") long offset);

    @Select("<script>SELECT COUNT(*) FROM blog_attachment " + FILTERS + "</script>")
    long count(@Param("kind") String kind, @Param("keyword") String keyword,
               @Param("from") String from, @Param("until") String until);

    @Select("SELECT * FROM blog_attachment WHERE id = #{id}")
    Attachment find(@Param("id") String id);

    @Select("""
            SELECT f.* FROM blog_attachment f WHERE f.id = #{id} AND EXISTS (
                SELECT 1 FROM blog_article_attachment r JOIN blog_article a ON a.id = r.article_id
                WHERE r.attachment_id = f.id AND a.status = 'PUBLISHED'
            )
            """)
    Attachment findPublished(@Param("id") String id);

    @Insert("INSERT INTO blog_attachment VALUES (#{id}, #{name}, #{mediaType}, #{size}, #{createdAt})")
    void insert(Attachment attachment);

    @Update("UPDATE blog_attachment SET name = #{name} WHERE id = #{id}")
    int rename(@Param("id") String id, @Param("name") String name);

    @Delete("""
            DELETE FROM blog_attachment WHERE id = #{id}
            AND NOT EXISTS (SELECT 1 FROM blog_article_attachment WHERE attachment_id = #{id})
            """)
    int deleteIfUnused(@Param("id") String id);

    @Delete("DELETE FROM blog_article_attachment WHERE article_id = #{articleId}")
    void clearReferences(@Param("articleId") Long articleId);

    @Insert("INSERT OR IGNORE INTO blog_article_attachment (article_id, attachment_id) VALUES (#{articleId}, #{id})")
    void addReference(@Param("articleId") Long articleId, @Param("id") String id);
}
