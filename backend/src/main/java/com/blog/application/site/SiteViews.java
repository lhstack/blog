package com.blog.application.site;

import com.blog.application.article.ArticleViews.ArchiveItem;
import com.blog.application.article.ArticleViews.ArticleCard;
import com.blog.application.article.ArticleViews.CategoryView;
import com.blog.application.article.ArticleViews.TagView;
import com.blog.application.comment.CommentViews.CommentView;
import com.blog.application.setting.SitePresentationApplicationService.PresentationView;

import java.util.List;

public final class SiteViews {

    private SiteViews() {
    }

    public record SiteInfo(
            String name,
            String title,
            String description,
            String avatar,
            String email,
            String github,
            int startYear,
            long articleCount,
            long viewCount,
            long commentCount,
            int categoryCount,
            int tagCount,
            /** 备案信息：未填写时为 null，前台据此决定是否渲染备案行。 */
            String icpNumber,
            String policeNumber,
            String policeUrl,
            PresentationView presentation) {
    }

    public record HomeView(
            SiteInfo site,
            List<ArticleCard> pinned,
            List<ArticleCard> latest,
            List<ArticleCard> popular,
            List<CategoryView> categories,
            List<TagView> tags,
            List<ArchiveItem> archives,
            List<CommentView> recentComments) {
    }

    public record DashboardView(
            long publishedArticles,
            long draftArticles,
            long approvedComments,
            long pendingComments,
            long totalViews,
            int categoryCount,
            int tagCount,
            List<ArchiveItem> archives,
            List<ArticleCard> recentArticles) {
    }
}
