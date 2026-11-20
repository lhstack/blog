package com.blog.domain.user;

/**
 * 密码摘要端口。领域层只声明契约，具体算法由基础设施层提供。
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
