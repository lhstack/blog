package com.blog.infrastructure.persistence.mapper;

import com.blog.domain.article.ArticleFeedEntry;
import com.blog.infrastructure.persistence.po.ArticlePo;
import lombok.Data;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章复杂查询。简单 CRUD 走 MyBatis-Plus（{@link ArticleBaseMapper}），
 * 聚合与相邻导航走显式 SQL，避免把 SQL 语义泄漏到领域层。
 */
@Mapper
public interface ArticleMapper {

    /** 归档：按年月截取文本时间并统计已发布文章。 */
    @Select("""
            SELECT substr(COALESCE(published_at, created_at), 1, 4) AS year,
                   substr(COALESCE(published_at, created_at), 6, 2) AS month,
                   COUNT(*) AS total
            FROM blog_article
            WHERE status = 'PUBLISHED'
            GROUP BY year, month
            ORDER BY year DESC, month DESC
            """)
    List<ArchiveRow> selectArchives();

    @Select("""
            SELECT id, title, summary, published_at
            FROM blog_article
            WHERE status = 'PUBLISHED'
            ORDER BY published_at DESC, id DESC
            LIMIT #{limit}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "published_at", javaType = LocalDateTime.class)
    })
    List<ArticleFeedEntry> selectLatestPublishedEntries(@Param("limit") int limit);

    /** 阅读量自增：单条 UPDATE 原子完成，避免读改写丢失。 */
    @Update("UPDATE blog_article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Select("SELECT COALESCE(SUM(view_count), 0) FROM blog_article")
    long sumViewCount();

    /** 同分类下的相关文章。 */
    @Select("""
            SELECT * FROM blog_article
            WHERE status = 'PUBLISHED' AND id <> #{articleId} AND category_id = #{categoryId}
            ORDER BY view_count DESC
            LIMIT #{limit}
            """)
    List<ArticlePo> selectRelatedInCategory(@Param("articleId") Long articleId,
                                            @Param("categoryId") Long categoryId,
                                            @Param("limit") int limit);

    /** 无同分类文章时的兜底：按热度取其它已发布文章。 */
    @Select("""
            SELECT * FROM blog_article
            WHERE status = 'PUBLISHED' AND id <> #{articleId}
            ORDER BY view_count DESC
            LIMIT #{limit}
            """)
    List<ArticlePo> selectPopularExcluding(@Param("articleId") Long articleId, @Param("limit") int limit);

    /** 上一篇：发布时间更早（或同时间 id 更小）的最近一篇。 */
    @Select("""
            SELECT * FROM blog_article
            WHERE status = 'PUBLISHED'
              AND (published_at < #{publishedAt} OR (published_at = #{publishedAt} AND id < #{id}))
            ORDER BY published_at DESC, id DESC
            LIMIT 1
            """)
    ArticlePo selectPrevious(@Param("publishedAt") String publishedAt, @Param("id") Long id);

    /** 下一篇：发布时间更晚（或同时间 id 更大）的最近一篇。 */
    @Select("""
            SELECT * FROM blog_article
            WHERE status = 'PUBLISHED'
              AND (published_at > #{publishedAt} OR (published_at = #{publishedAt} AND id > #{id}))
            ORDER BY published_at ASC, id ASC
            LIMIT 1
            """)
    ArticlePo selectNext(@Param("publishedAt") String publishedAt, @Param("id") Long id);

    /** 首页置顶：直接在存储层过滤 pinned，避免“先取最新 N 篇再筛选”而漏掉旧置顶。 */
    @Select("""
            SELECT * FROM blog_article
            WHERE status = 'PUBLISHED' AND pinned = 1
            ORDER BY COALESCE(published_at, created_at) DESC, id DESC
            """)
    List<ArticlePo> selectPinned();

    @Data
    class ArchiveRow {
        private String year;
        private String month;
        private Long total;
    }
}
