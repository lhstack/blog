package com.blog.application.auth;

public final class AuthViews {

    private AuthViews() {
    }

    public record ProfileView(Long id, String username, String nickname, String avatar, String email) {
    }

    /** 登录结果：凭证由接口层签发后回填。 */
    public record LoginResult(String token, String tokenName, ProfileView profile) {
    }
}
