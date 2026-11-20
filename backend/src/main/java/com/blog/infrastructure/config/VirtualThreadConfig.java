package com.blog.infrastructure.config;

import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * 虚拟线程配置。
 * <p>
 * {@code spring.threads.virtual.enabled=true} 已让 Spring Boot 的默认执行器与
 * 异步请求切换为虚拟线程；此处显式配置 Tomcat 协议处理器，
 * 使每个 HTTP 连接都在虚拟线程上处理（不依赖 Boot 版本的默认行为）。
 * <p>
 * 注意：SQLite 写锁是真正的串行点，虚拟线程只解决 IO 等待的可伸缩性，
 * 不改变锁竞争结论。
 */
@Configuration
public class VirtualThreadConfig {

    @Bean
    public TomcatProtocolHandlerCustomizer<?> virtualThreadProtocolHandlerCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(
                Executors.newVirtualThreadPerTaskExecutor());
    }
}
