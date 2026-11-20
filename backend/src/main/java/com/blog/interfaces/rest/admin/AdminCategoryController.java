package com.blog.interfaces.rest.admin;

import com.blog.application.article.ArticleViews.CategoryView;
import com.blog.application.category.CategoryApplicationService;
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

@Tag(name = "后台-分类", description = "分类维护")
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryApplicationService categoryService;

    public AdminCategoryController(CategoryApplicationService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表")
    @GetMapping
    public List<CategoryView> list() {
        return categoryService.listAll();
    }

    @Operation(summary = "新建分类")
    @PostMapping
    public CategoryView create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request.name(), request.slug(), request.description(),
                request.sortOrder() == null ? 0 : request.sortOrder());
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public CategoryView update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request.name(), request.slug(), request.description(),
                request.sortOrder() == null ? 0 : request.sortOrder());
    }

    @Operation(summary = "删除分类", description = "分类下存在文章时拒绝删除")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }
}

record CategoryRequest(
        @NotBlank(message = "分类名称不能为空") @Size(max = 40, message = "分类名称不能超过 40 个字符") String name,
        @Size(max = 60, message = "别名过长") String slug,
        @Size(max = 200, message = "描述不能超过 200 个字符") String description,
        Integer sortOrder) {
}
