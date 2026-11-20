package com.blog.interfaces.rest.admin;

import com.blog.application.article.ArticleViews.TagView;
import com.blog.application.tag.TagApplicationService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-标签", description = "标签维护")
@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

    private final TagApplicationService tagService;

    public AdminTagController(TagApplicationService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "标签列表")
    @GetMapping
    public List<TagView> list() {
        return tagService.listAll();
    }

    @Operation(summary = "新建标签")
    @PostMapping
    public TagView create(@Valid @RequestBody TagRequest request) {
        return tagService.create(request.name(), request.color());
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public TagView update(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        return tagService.update(id, request.name(), request.color());
    }

    @Operation(summary = "删除标签", description = "标签被文章使用时拒绝删除")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ApiResponse.ok();
    }
}

record TagRequest(
        @NotBlank(message = "标签名称不能为空") @Size(max = 30, message = "标签名称不能超过 30 个字符") String name,
        @Size(max = 20, message = "颜色标识过长") String color) {
}
