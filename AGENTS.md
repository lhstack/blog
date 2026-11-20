# AGENTS.md

## 项目概述

个人博客系统，前后端分离，采用 DDD（领域驱动设计）分层架构。

- `backend/`：Spring Boot 3.5 + Java 25 单体后端，领域逻辑集中在 `com.blog.domain`，不含任何框架依赖（无 Spring / MyBatis 注解）。
- `frontend/`：Vite + Vue 3 + Ant Design Vue（Ant Design 5 设计体系）单页应用，含动态背景、鼠标跟随、点击特效等视觉层。

## 工程环境与主要工具

| 项目 | 要求 |
| --- | --- |
| JDK | 25（以 `backend/pom.xml` 的 `java.version` 为准） |
| Maven | 3.9；运行 `mvn -v` 确认 Maven 使用 JDK 25 |
| Node.js | 建议 22.12+；前端使用 `npm ci` 安装锁定依赖 |

从仓库根目录构建后端：

```sh
mvn -f backend/pom.xml package
```

依赖版本均为 Maven Central 实际发布版本（已在仓库元数据中确认）：

- Spring Boot 3.5.16
- MyBatis-Plus 3.5.18（`mybatis-plus-spring-boot3-starter` + `mybatis-plus-jsqlparser`）
- Sa-Token 1.46.0（`sa-token-spring-boot3-starter`）
- sqlite-jdbc 3.54.1.0
- springdoc-openapi 2.8.13
- Reactor Core 由 Spring Boot BOM 管理

## 目录与模块结构

```
backend/src/main/java/com/blog
├── domain/            # 领域层：实体、值对象、仓储接口，纯 Java
│   ├── article/       # 文章聚合（Article 为聚合根）+ ArticleRepository
│   ├── category/      # 分类聚合
│   ├── tag/           # 标签聚合
│   ├── comment/       # 评论聚合
│   ├── user/          # 管理员账号聚合
│   └── common/        # PageSlice、DomainException 等共享领域构件
├── application/       # 应用层：用例编排、命令/读模型（View）
├── infrastructure/    # 基础设施层：MyBatis-Plus 持久化实现、配置、启动初始化
└── interfaces/rest/   # 接口层：Controller、请求 DTO、Reactive 适配
frontend/src
├── api/               # axios 客户端与接口封装
├── stores/            # Pinia（鉴权状态）
├── router/            # vue-router
├── layouts/           # 前台 / 后台布局
├── views/             # 页面
└── components/effects # 背景动画、鼠标跟随、点击涟漪
```

## 分层架构与依赖方向

依赖方向严格单向：`interfaces → application → domain`，`infrastructure → domain`（实现仓储），`application` 通过仓储接口访问数据，不直接依赖 MyBatis。

- 领域层不得出现 `org.springframework`、`com.baomidou`、`cn.dev33` 导入。
- 仓储接口定义在 `domain/*/`，实现放在 `infrastructure/persistence/*/`。
- Sa-Token 只在 `interfaces/rest`（以及 `infrastructure/config` 的拦截器注册）中出现；应用层只负责凭证校验，不签发 token。
- Reactor 只在 `interfaces/rest` 与 `infrastructure/config` 的调度器配置中出现，用于公开读接口与 SSE 流；应用层保持阻塞式 API。

## 构建、测试和验证方式

- 后端验证：`mvn -f backend/pom.xml test`；编译、测试与打包：`mvn -f backend/pom.xml package`。
- 后端启动：`java -jar backend/target/blog-backend-1.0.0.jar --spring.profiles.active=dev`（端口 8080）。
- 数据目录：由 `BLOG_DATA_DIR` 控制（默认 `./data`，随启动工作目录解析）。验证时用隔离的临时数据目录，不写现有站点数据。
- 前端依赖安装与构建：`npm ci`、`npm run build`（在 `frontend/` 内）。单 JAR 发布时先构建前端，再执行 Maven 打包。
- 前端开发：`npm run dev`（端口 5173，`/api`、`/rss.xml` 代理到 8080）。
- API 冒烟检查：`GET http://localhost:8080/api/public/site`。
- UI 验证：`node tools/verify-ui.mjs`（CDP 驱动 headless Chrome；运行前按本机环境调整脚本内的 Chrome、截图与 profile 路径，并使用独立的 Chrome profile）。

## 接口文档安全开关

文档入口默认关闭，由三层同时控制，改动时必须保持一致：

1. `blog.api-doc.enabled`（默认 `false`）→ 驱动 springdoc 是否注册 `/v3/api-docs`、`/swagger-ui`；`OpenApiConfig` 用 `@ConditionalOnProperty` 跟随该开关。
2. `ApiDocGuardFilter`（`interfaces/rest/support`）→ 关闭时对 `/doc.html`、`/v3/api-docs`、`/v2/api-docs`、`/swagger-ui`、`/swagger-resources`、`/webjars`、`/knife4j` 返回 **404**（非 403，避免暴露目标存在）。路径匹配按段比较，不做朴素 startsWith。
3. `application-prod.yml` 再显式置 `false`。

开发时用 `--spring.profiles.active=dev` 打开。

## 已知版本冲突（勿回退）

`knife4j-openapi3-jakarta-spring-boot-starter:4.5.0` 编译期绑定 springdoc 2.3.0（`SpringDocConfigProperties#getGroupConfigs()` 返回 `List`），而 Spring Boot 3.5 需要 springdoc 2.8.x（返回 `Set`），运行时会抛 `NoSuchMethodError`。

因此只引入纯静态资源的 `knife4j-openapi3-ui:4.5.0`（提供 `/doc.html` 与 webjars），不引入 starter；文档能力由 `springdoc-openapi-starter-webmvc-ui:2.8.13` 提供。

其他已确认可用于本项目的版本：Spring Boot 3.5.16、MyBatis-Plus 3.5.18、Sa-Token 1.46.0、sqlite-jdbc 3.54.1.0。

## 数据与启动约定

- `db/schema.sql` 必须保持幂等；默认不加载演示数据。
- 管理员账号由 `DataInitializer` 在账号不存在时创建，已存在则跳过，绝不重置密码。
- SQL 文本中的单引号必须转义为 `''`；`spring.sql.init.encoding` 必须为 UTF-8。

## 项目编码约定

- Java 25 语言特性可用（record、switch 表达式、文本块）。
- 领域实体使用流式访问器（`article.title()`），不使用 getter 前缀。
- 读模型（View）与应用命令均为 `record`，定义在 `application` 包内。
- 依赖注入使用构造器注入，不使用字段 `@Autowired`。
- 时间统一用 `java.time.LocalDateTime`，数据库中存 `yyyy-MM-dd HH:mm:ss` 文本，由自定义 TypeHandler 转换。
- SQL 只出现在 `infrastructure/persistence` 的 Mapper 中。

## 错误处理约定

- 领域层抛 `DomainException`（含错误码），不抛框架异常。
- 应用/接口层通过 `BizException` 表示可预期的业务错误。
- `GlobalExceptionHandler` 的 JSON 错误响应为 `ApiResponse{code,message,data,timestamp}`，HTTP 状态与 `code` 保持一致。
- 未预期异常记录日志，响应仍可写时返回 500；异步连接已不可写时不再写状态码或响应体。

## 版本控制

提交源代码、锁文件和文档；运行数据、日志、依赖与本地构建产物不进入 Git。

## 当前无法确认的事项

- 部署方式（容器 / 反向代理 / HTTPS）
- 生产环境数据库规模与备份策略
- 是否需要多用户与权限角色体系（当前仅单管理员）
