package com.blog.interfaces.rest.admin;

import com.blog.application.comment.CommentApplicationService;
import com.blog.application.comment.CommentViews.CommentView;
import com.blog.domain.common.DateRange;
import com.blog.domain.common.PageResult;
import com.blog.interfaces.rest.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "后台-评论", description = "评论审核与删除")
@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final CommentApplicationService commentService;

    public AdminCommentController(CommentApplicationService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "评论列表")
    @GetMapping
    public PageResult<CommentView> list(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return commentService.search(status, articleId, new DateRange(from, to), page, size);
    }

    @Operation(summary = "审核评论", description = "action: approve / reject")
    @PostMapping("/{id}/moderate")
    public CommentView moderate(@PathVariable Long id, @RequestParam String action) {
        return commentService.moderate(id, action);
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ApiResponse.ok();
    }
}
