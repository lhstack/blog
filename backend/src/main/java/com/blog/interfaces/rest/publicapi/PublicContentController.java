package com.blog.interfaces.rest.publicapi;

import com.blog.application.article.ArticleApplicationService;
import com.blog.application.article.ArticleViews.ArchiveItem;
import com.blog.application.article.ArticleViews.ArticleCard;
import com.blog.application.article.ArticleViews.ArticleDetail;
import com.blog.application.category.CategoryApplicationService;
import com.blog.application.comment.CommentApplicationService;
import com.blog.application.comment.CommentViews.CommentThread;
import com.blog.application.comment.CommentViews.CommentView;
import com.blog.application.setting.SitePresentationApplicationService.AboutView;
import com.blog.application.site.SiteApplicationService;
import com.blog.application.site.SiteViews.HomeView;
import com.blog.application.site.SiteViews.SiteInfo;
import com.blog.application.tag.TagApplicationService;
import com.blog.application.article.ArticleViews.CategoryView;
import com.blog.application.article.ArticleViews.TagView;
import com.blog.domain.article.Article;
import com.blog.domain.common.PageResult;
import com.blog.interfaces.rest.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台公开接口：站点信息、首页聚合、文章浏览、评论提交。
 * <p>
 * 这一层只做参数绑定与视图返回，业务判断全部在应用层。
 */
@Tag(name = "前台-内容", description = "无需登录的公开读接口")
@RestController
@RequestMapping("/api/public")
public class PublicContentController {

    private final SiteApplicationService siteService;
    private final ArticleApplicationService articleService;
    private final CategoryApplicationService categoryService;
    private final TagApplicationService tagService;
    private final CommentApplicationService commentService;

    public PublicContentController(SiteApplicationService siteService,
                                   ArticleApplicationService articleService,
                                   CategoryApplicationService categoryService,
                                   TagApplicationService tagService,
                                   CommentApplicationService commentService) {
        this.siteService = siteService;
        this.articleService = articleService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.commentService = commentService;
    }

    @Operation(summary = "站点信息")
    @GetMapping("/site")
    public SiteInfo site() {
        return siteService.siteInfo();
    }

    @Operation(summary = "关于页正文")
    @GetMapping("/about")
    public AboutView about() {
        return siteService.about();
    }

    @Operation(summary = "首页聚合数据")
    @GetMapping("/home")
    public HomeView home() {
        return siteService.home();
    }

    @Operation(summary = "文章分页检索")
    @GetMapping("/articles")
    public PageResult<ArticleCard> articles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int size) {
        return articleService.searchPublished(keyword, categoryId, tagId, sort, page, size);
    }

    @Operation(summary = "文章详情")
    @GetMapping("/articles/{id}")
    public ArticleDetail article(@PathVariable Long id) {
        return articleService.publishedDetail(id);
    }

    @Operation(summary = "文章归档")
    @GetMapping("/archives")
    public List<ArchiveItem> archives() {
        return articleService.archives();
    }

    @Operation(summary = "分类列表")
    @GetMapping("/categories")
    public List<CategoryView> categories() {
        return categoryService.listAll();
    }

    @Operation(summary = "标签列表")
    @GetMapping("/tags")
    public List<TagView> tags() {
        return tagService.listAll();
    }

    @Operation(summary = "文章评论列表")
    @GetMapping("/articles/{id}/comments")
    public CommentThread comments(@PathVariable Long id) {
        return commentService.listApproved(id);
    }

    @Operation(summary = "提交评论", description = "提交后进入待审核状态，管理员通过后才公开")
    @PostMapping("/articles/{id}/comments")
    public ApiResponse<CommentView> submitComment(@PathVariable Long id,
                                                  @Valid @RequestBody SubmitCommentRequest request,
                                                  HttpServletRequest servletRequest) {
        CommentView view = commentService.submit(id, request.author(), request.email(),
                request.website(), request.content(), clientIp(servletRequest));
        return ApiResponse.ok(view);
    }

    @Operation(summary = "按状态筛选（前台不使用）")
    @GetMapping("/status-options")
    public List<String> statusOptions() {
        return List.of(Article.Status.DRAFT.name(), Article.Status.PUBLISHED.name());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

record SubmitCommentRequest(
        @NotBlank(message = "昵称不能为空") @Size(max = 40, message = "昵称不能超过 40 个字符") String author,
        @Email(message = "邮箱格式不正确") @Size(max = 120, message = "邮箱过长") String email,
        @Size(max = 200, message = "网址过长") String website,
        @NotBlank(message = "评论内容不能为空") @Size(max = 2000, message = "评论内容不能超过 2000 个字符") String content) {
}
