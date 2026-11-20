package com.blog.interfaces.rest.auth;

import com.blog.application.auth.AuthApplicationService;
import com.blog.application.auth.AuthViews.LoginResult;
import com.blog.application.auth.AuthViews.ProfileView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "管理员登录")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authService;
    private final TokenIssuer tokenIssuer;

    public AuthController(AuthApplicationService authService, TokenIssuer tokenIssuer) {
        this.authService = authService;
        this.tokenIssuer = tokenIssuer;
    }

    @Operation(summary = "登录", description = "校验凭证并签发 Sa-Token")
    @PostMapping("/login")
    public LoginResult login(@Valid @RequestBody LoginRequest request) {
        ProfileView profile = authService.authenticate(request.username(), request.password())
                .orElseThrow(() -> new com.blog.domain.common.DomainException(
                        "UNAUTHORIZED", "用户名或密码不正确"));
        return tokenIssuer.issue(profile);
    }
}

record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") @Size(min = 1, max = 64, message = "密码长度不合法") String password) {
}
