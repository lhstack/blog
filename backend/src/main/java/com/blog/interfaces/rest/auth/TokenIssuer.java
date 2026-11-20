package com.blog.interfaces.rest.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.blog.application.auth.AuthViews.LoginResult;
import com.blog.application.auth.AuthViews.ProfileView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 凭证签发/注销适配器。
 * <p>
 * 应用层不感知鉴权框架，token 的生成与失效只发生在这一层。
 */
@Component
public class TokenIssuer {

    private final String tokenName;

    public TokenIssuer(@Value("${sa-token.token-name}") String tokenName) {
        this.tokenName = tokenName;
    }

    public LoginResult issue(ProfileView profile) {
        StpUtil.login(profile.id());
        return new LoginResult(StpUtil.getTokenValue(), tokenName, profile);
    }

    public void revoke() {
        StpUtil.logout();
    }

    public Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
