package com.blog.interfaces.rest.admin;

import com.blog.application.article.ArticleApplicationService;
import com.blog.application.article.ArticleCommand;
import com.blog.application.article.ArticleViews.ArticleCard;
import com.blog.application.article.ArticleViews.ArticleDetail;
import com.blog.domain.common.DateRange;
import com.blog.domain.common.PageResult;
import com.blog.interfaces.rest.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "后台-文章", description = "文章增删改与状态流转")
@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final ArticleApplicationService articleService;

    public AdminArticleController(ArticleApplicationService articleService) {
        this.articleService = articleService;
    }

    @Operation(summary = "文章列表（含草稿）")
    @GetMapping
    public PageResult<ArticleCard> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return articleService.searchAll(keyword, categoryId, tagId, status, sort, page, size, new DateRange(from, to));
    }

    @Operation(summary = "文章详情（含草稿）")
    @GetMapping("/{id}")
    public ArticleDetail detail(@PathVariable Long id) {
        return articleService.adminDetail(id);
    }

    @Operation(summary = "新建草稿")
    @PostMapping
    public ArticleDetail create(@Valid @RequestBody ArticleRequest request) {
        return articleService.create(toCommand(request));
    }

    @Operation(summary = "更新文章")
    @PutMapping("/{id}")
    public ArticleDetail update(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return articleService.update(id, toCommand(request));
    }

    @Operation(summary = "发布文章")
    @PostMapping("/{id}/publish")
    public ArticleDetail publish(@PathVariable Long id,
                                 @RequestParam(defaultValue = "false") boolean pinned) {
        return articleService.publish(id, pinned);
    }

    @Operation(summary = "撤回为草稿")
    @PostMapping("/{id}/withdraw")
    public ArticleDetail withdraw(@PathVariable Long id) {
        return articleService.withdraw(id);
    }

    @Operation(summary = "置顶 / 取消置顶")
    @PostMapping("/{id}/pin")
    public ArticleDetail pin(@PathVariable Long id, @RequestParam boolean pinned) {
        return articleService.pin(id, pinned);
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ApiResponse.ok();
    }

    private ArticleCommand toCommand(ArticleRequest request) {
        return new ArticleCommand(request.title(), request.summary(), request.content(),
                request.cover(), request.categoryId(), request.tagIds(), request.pinned());
    }
}

record ArticleRequest(
        @NotBlank(message = "标题不能为空") @Size(max = 120, message = "标题不能超过 120 个字符") String title,
        @Size(max = 500, message = "摘要不能超过 500 个字符") String summary,
        @NotBlank(message = "正文不能为空") String content,
        String cover,
        Long categoryId,
        List<Long> tagIds,
        Boolean pinned) {
}
