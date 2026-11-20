package com.blog.application.site;

import com.blog.application.article.ArticleApplicationService;
import com.blog.application.article.ArticleViews.ArchiveItem;
import com.blog.application.article.ArticleViews.ArticleCard;
import com.blog.application.article.ArticleViews.CategoryView;
import com.blog.application.article.ArticleViews.TagView;
import com.blog.application.comment.CommentApplicationService;
import com.blog.application.comment.CommentViews.CommentView;
import com.blog.application.category.CategoryApplicationService;
import com.blog.application.setting.SettingViews.SiteSettingView;
import com.blog.application.setting.SiteAvatarApplicationService;
import com.blog.application.setting.SiteIdentityApplicationService;
import com.blog.application.setting.SitePresentationApplicationService;
import com.blog.application.setting.SiteSettingApplicationService;
import com.blog.application.tag.TagApplicationService;
import com.blog.domain.article.Article;
import com.blog.domain.article.ArticleRepository;
import com.blog.domain.comment.Comment;
import com.blog.domain.comment.CommentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站点门户用例：站点信息、首页聚合、仪表盘统计。
 */
@Service
public class SiteApplicationService {

    private final ArticleApplicationService articleService;
    private final CategoryApplicationService categoryService;
    private final TagApplicationService tagService;
    private final CommentApplicationService commentService;
    private final SiteSettingApplicationService settingService;
    private final SitePresentationApplicationService presentationService;
    private final SiteAvatarApplicationService avatarService;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    private final SiteIdentityApplicationService identityService;
    private final String email;
    private final String github;
    private final int startYear;

    public SiteApplicationService(ArticleApplicationService articleService,
                                  CategoryApplicationService categoryService,
                                  TagApplicationService tagService,
                                  CommentApplicationService commentService,
                                  SiteSettingApplicationService settingService,
                                  SitePresentationApplicationService presentationService,
                                  SiteAvatarApplicationService avatarService,
                                  ArticleRepository articleRepository,
                                  CommentRepository commentRepository,
                                  SiteIdentityApplicationService identityService,
                                  @Value("${blog.site.email}") String email,
                                  @Value("${blog.site.github}") String github,
                                  @Value("${blog.site.start-year}") int startYear) {
        this.articleService = articleService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.commentService = commentService;
        this.settingService = settingService;
        this.presentationService = presentationService;
        this.avatarService = avatarService;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.identityService = identityService;
        this.email = email;
        this.github = github;
        this.startYear = startYear;
    }

    @Transactional(readOnly = true)
    public SiteViews.SiteInfo siteInfo() {
        SiteSettingView setting = settingService.current();
        var identity = identityService.current();
        return new SiteViews.SiteInfo(identity.name(), identity.title(), identity.description(), avatarService.current().avatar(), email, github, startYear,
                articleRepository.countByStatus(Article.Status.PUBLISHED),
                articleRepository.totalViewCount(),
                commentRepository.countByStatus(Comment.Status.APPROVED),
                categoryService.listAll().size(),
                tagService.listAll().size(),
                setting.icpNumber(),
                setting.policeNumber(),
                setting.policeUrl(),
                presentationService.current());
    }

    public SitePresentationApplicationService.AboutView about() {
        return presentationService.about();
    }

    /** 首页聚合：置顶 + 最新文章 + 热榜 + 归档 + 最新评论。 */
    @Transactional(readOnly = true)
    public SiteViews.HomeView home() {
        List<ArticleCard> latest = articleService.searchPublished(null, null, null, "latest", 1, 6).items();
        List<ArticleCard> pinned = articleService.pinnedPublished();
        List<ArticleCard> popular = articleService.searchPublished(null, null, null, "views", 1, 5).items();
        List<ArchiveItem> archives = articleService.archives();
        List<CommentView> recentComments = commentService.recentApproved(5);
        List<CategoryView> categories = categoryService.listAll();
        List<TagView> tags = tagService.listAll();
        return new SiteViews.HomeView(siteInfo(), pinned, latest, popular, categories, tags, archives, recentComments);
    }

    @Transactional(readOnly = true)
    public SiteViews.DashboardView dashboard() {
        long published = articleRepository.countByStatus(Article.Status.PUBLISHED);
        long drafts = articleRepository.countByStatus(Article.Status.DRAFT);
        long pendingComments = commentRepository.countByStatus(Comment.Status.PENDING);
        long approvedComments = commentRepository.countByStatus(Comment.Status.APPROVED);
        List<ArchiveItem> archives = articleService.archives();
        List<ArticleCard> recent = articleService.searchAll(null, null, null, "ALL", "latest", 1, 5).items();
        return new SiteViews.DashboardView(published, drafts, approvedComments, pendingComments,
                articleRepository.totalViewCount(), categoryService.listAll().size(),
                tagService.listAll().size(), archives, recent);
    }
}
