package com.blog.infrastructure.bootstrap;

import com.blog.domain.user.PasswordHasher;
import com.blog.domain.user.User;
import com.blog.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动引导：确保管理员账号存在。
 * <p>
 * 只在账号不存在时创建（幂等），已存在则不做任何修改，避免每次重启重置密码。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminNickname;

    public DataInitializer(UserRepository userRepository,
                           PasswordHasher passwordHasher,
                           @Value("${blog.admin.username}") String adminUsername,
                           @Value("${blog.admin.password}") String adminPassword,
                           @Value("${blog.admin.nickname}") String adminNickname) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminNickname = adminNickname;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            log.info("管理员账号已存在，跳过初始化: {}", adminUsername);
            return;
        }
        User admin = User.newAccount(adminUsername, passwordHasher.hash(adminPassword), adminNickname);
        userRepository.save(admin);
        log.info("已创建管理员账号: {}（请登录后立即修改默认密码）", adminUsername);
    }
}
