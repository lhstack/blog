package com.blog.infrastructure.persistence.mapper;

import com.blog.infrastructure.persistence.po.ArticleTagPo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 文章-标签关联表。复合主键表不使用 BaseMapper，全部显式声明 SQL，避免依赖主键推断。
 */
@Mapper
public interface ArticleTagMapper {

    @Insert("""
            INSERT INTO blog_article_tag (article_id, tag_id) VALUES (#{articleId}, #{tagId})
            ON CONFLICT (article_id, tag_id) DO NOTHING
            """)
    @Insert(value = """
            INSERT INTO blog_article_tag (article_id, tag_id) VALUES (#{articleId}, #{tagId})
            ON DUPLICATE KEY UPDATE article_id = article_id
            """, databaseId = "mysql")
    int link(@Param("articleId") Long articleId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM blog_article_tag WHERE article_id = #{articleId}")
    int unlinkAll(@Param("articleId") Long articleId);

    @Delete("DELETE FROM blog_article_tag WHERE tag_id = #{tagId}")
    int unlinkByTag(@Param("tagId") Long tagId);

    @Select("SELECT tag_id FROM blog_article_tag WHERE article_id = #{articleId}")
    List<Long> selectTagIds(@Param("articleId") Long articleId);

    @Select("""
            <script>
            SELECT article_id, tag_id FROM blog_article_tag WHERE article_id IN
            <foreach collection="articleIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<ArticleTagPo> selectByArticleIds(@Param("articleIds") Collection<Long> articleIds);
}
