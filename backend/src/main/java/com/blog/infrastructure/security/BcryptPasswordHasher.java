package com.blog.infrastructure.security;

import com.blog.domain.user.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码摘要适配器：领域层声明 {@link PasswordHasher} 端口，此处用 BCrypt 实现。
 */
@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null) {
            return false;
        }
        return delegate.matches(rawPassword, passwordHash);
    }
}
