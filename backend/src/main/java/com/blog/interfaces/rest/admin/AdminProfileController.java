package com.blog.interfaces.rest.admin;

import com.blog.application.auth.AuthApplicationService;
import com.blog.application.auth.AuthViews.ProfileView;
import com.blog.application.setting.SiteAvatarApplicationService;
import com.blog.domain.common.DomainException;
import com.blog.interfaces.rest.auth.TokenIssuer;
import com.blog.interfaces.rest.support.ApiResponse;
import com.blog.interfaces.rest.support.AvatarUploadSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-账号", description = "当前登录管理员信息与密码修改")
@RestController
@RequestMapping("/api/admin/profile")
public class AdminProfileController {

    private final AuthApplicationService authService;
    private final TokenIssuer tokenIssuer;

    public AdminProfileController(AuthApplicationService authService, TokenIssuer tokenIssuer) {
        this.authService = authService;
        this.tokenIssuer = tokenIssuer;
    }

    @Operation(summary = "当前管理员信息")
    @GetMapping
    public ProfileView current() {
        return authService.profile(tokenIssuer.currentUserId());
    }

    @Operation(summary = "修改资料")
    @PutMapping
    public ProfileView update(@Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(tokenIssuer.currentUserId(),
                request.nickname(), request.avatar(), request.email());
    }

    @PostMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileView uploadAvatar(@RequestPart("file") MultipartFile file) throws IOException {
        AvatarUploadSupport.requireImageDecoder();
        if (file.getSize() > SiteAvatarApplicationService.MAX_SIZE) {
            throw DomainException.invalid("请选择不超过 5 MB 的头像图片");
        }
        return authService.uploadAvatar(tokenIssuer.currentUserId(), file.getOriginalFilename(), file.getBytes());
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(tokenIssuer.currentUserId(), request.oldPassword(), request.newPassword());
        tokenIssuer.revoke();
        return ApiResponse.ok();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        tokenIssuer.revoke();
        return ApiResponse.ok();
    }
}

record UpdateProfileRequest(
        @NotBlank(message = "昵称不能为空") @Size(max = 40, message = "昵称不能超过 40 个字符") String nickname,
        String avatar,
        String email) {
}

record ChangePasswordRequest(
        @NotBlank(message = "原密码不能为空") String oldPassword,
        @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 64, message = "新密码长度需在 6-64 之间") String newPassword) {
}
