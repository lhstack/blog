# 云间小筑 · 个人博客系统

一个使用 Spring Boot、SQLite 和 Vue 3 的个人博客。开发时前后端可以分别运行；部署时可打包为一个 JAR，同时提供网页、REST 接口和 RSS 订阅，无需单独部署数据库服务。

## 功能

- **前台**：文章列表、分类与标签筛选、归档、关于页、评论、RSS 订阅，以及首页置顶轮播、实时统计和背景音乐。页面支持移动端布局和系统「减少动态效果」设置。
- **内容管理**：Markdown 编辑与预览、文章发布／撤回／置顶、分类标签维护、评论审核和附件管理。文章、评论、附件的筛选在服务端分页前执行；后台预览不增加阅读量。
- **站点设置**：站点资料、头像、备案信息、背景音乐和账号资料。独立的「首页打字机」菜单支持最多 30 条文案，可按站点时区配置生效星期、时段和播放顺序；起止相同表示全天，跨午夜以勾选的星期为开始日。

## 技术栈与环境

| 用途 | 选型 |
| --- | --- |
| 后端构建 | **JDK 25**、Maven 3.9（以 [`backend/pom.xml`](backend/pom.xml) 为准） |
| 后端 | Spring Boot 3.5.16、Spring MVC、虚拟线程、Reactor |
| 持久化与认证 | SQLite（WAL）、MyBatis-Plus 3.5.18、Sa-Token 1.46.0 |
| 前端构建 | 建议 **Node.js 22.12+** 与 npm，使用 `package-lock.json` 锁定依赖 |
| 前端 | Vue 3.5、Vite 6、Ant Design Vue 4、Vue Router、Pinia |
| 接口文档 | springdoc-openapi 2.8.13、Knife4j UI 4.5.0（仅静态资源） |

如本机有多个 JDK，先运行 `mvn -v`，确认 Maven 使用的是 JDK 25。

## 本地开发

以下命令从仓库根目录执行，后端和前端各用一个终端。

**终端一：启动后端**

```sh
mvn -f backend/pom.xml package
java -jar backend/target/blog-backend-1.0.0.jar --spring.profiles.active=dev
```

- API 冒烟检查：<http://localhost:8080/api/public/site>
- 开发接口文档：<http://localhost:8080/doc.html>
- 首次启动自动建表，默认不导入演示数据。由仓库根目录启动时，SQLite 文件为 `./data/blog.db`；若从其他目录启动，`./data` 随工作目录变化。可用 `BLOG_DATA_DIR` 指定固定目录。

**终端二：启动前端**

```sh
cd frontend
npm ci
npm run dev
```

- 博客首页：<http://localhost:5173/>
- 管理后台：<http://localhost:5173/admin/login>
- Vite 将 `/api` 和 `/rss.xml` 代理到 `localhost:8080`。
- 本地首次初始化默认账号为 `admin` / `admin123`。仅用于本地开发；生产环境请在首次启动前配置强密码。初始化不会为已存在的同名账号重置密码。

## 打包与部署

先构建前端，再从根目录打包后端。**Maven 不会执行前端构建**，但会将已有的 `frontend/dist/` 并入 JAR 的 `classpath:/static`；没有构建前端也可以生成仅提供接口的 JAR。

```sh
cd frontend
npm ci
npm run build
cd ..
mvn -f backend/pom.xml clean package
```

产物为 `backend/target/blog-backend-1.0.0.jar`。配置好下表中的数据目录、站点地址和初始管理员后启动：

```sh
java -jar backend/target/blog-backend-1.0.0.jar --spring.profiles.active=prod
```

默认监听 `8080`；首页为 `/`，后台为 `/admin/login`。一体化部署能直接打开或刷新 `/articles/1` 等前端路由；API、文档路径和缺失的静态文件不会回落到首页。HTML 入口不缓存，带内容哈希的静态资源不受影响。

| 环境变量 | 用途 |
| --- | --- |
| `BLOG_DATA_DIR` | SQLite、附件和音乐文件目录，默认 `./data`。生产建议使用持久化的绝对路径。 |
| `BLOG_ADMIN_USERNAME` / `BLOG_ADMIN_PASSWORD` | 初始化管理员的用户名、密码。生产环境不要使用默认凭据；已有账号请通过后台修改密码。 |
| `BLOG_SITE_URL` | 访客访问的 HTTP(S) 根地址，用于生成 RSS 绝对链接，默认 `http://localhost:8080`。 |
| `BLOG_TYPEWRITER_ZONE` | 首页打字机的站点时区，默认 `Asia/Shanghai`。 |
| `BLOG_LOG_DIR` | 日志目录，默认 `./logs`。 |
| `SERVER_PORT` | 后端端口，默认 `8080`。 |

若独立部署前端，请将 `/api/**` 和 `/rss.xml` 转发到后端，其余前端路由回落到 `index.html`。反向代理应关闭 SSE 路径的响应缓冲，避免实时统计被攒成批量输出。

### 数据、备份与日志

- 首次启动执行幂等的 [`schema.sql`](backend/src/main/resources/db/schema.sql)，不加载种子数据。使用 SQLite 文件库，无需单独运行数据库服务。
- 持久化数据放在 `BLOG_DATA_DIR`：`blog.db`、`attachments/` 和 `background-music/`。备份时建议先停服务再备份整个目录，避免 SQLite 的 WAL 与媒体文件不一致；恢复时一起还原。部署新 JAR 不需要替换数据目录。
- Log4j2 异步写入 `blog-info.log`、`blog-warn.log`、`blog-error.log`，按级别分文件。默认每 7 天或达到 100 MB 滚动压缩，可用 `BLOG_LOG_ROLL_DAYS`、`BLOG_LOG_ROLL_SIZE` 调整；旧归档不会自动删除。
- 附件单文件上限 20 MB，可用于 Markdown 或文章封面。只有已发布文章引用的附件可匿名读取；删除前需先解除文章引用。
- 本地数据库、上传文件、日志、依赖和构建产物都不纳入 Git。前端静态资源的源文件放在 `frontend/`，不手动复制构建产物到后端源码目录。

## 接口速查

| 请求 | 说明 |
| --- | --- |
| `GET /api/public/site`、`GET /api/public/home` | 站点信息和首页聚合 |
| `GET /api/public/articles`、`GET /api/public/articles/{id}` | 文章列表与详情 |
| `GET/POST /api/public/articles/{id}/comments` | 读取和提交评论 |
| `GET /api/public/site/stats`、`GET /api/public/site/stats/stream` | 统计快照与 SSE（每 8 秒推送） |
| `GET /rss.xml` | RSS 2.0：最多 20 篇已发布文章的摘要与链接，不增加阅读量 |
| `POST /api/auth/login` | 登录并获取 token |
| `/api/admin/**` | 管理接口，请求头需携带 `blog-token` |

JSON 错误响应使用 `{ code, message, data, timestamp }`，其中 `code` 对应 HTTP 状态。成功响应按接口返回相应数据；SSE、RSS 和文件下载使用各自的媒体类型。RSS 支持 ETag 和 5 分钟 HTTP 缓存，文章更新后订阅器可能短暂显示旧内容。

默认及 `prod` 环境关闭接口文档，访问相关路径返回 `404`；仅在本地以 `dev` 模式开启。

## 代码结构与验证

```text
backend/
├── src/main/java/com/blog/
│   ├── domain/          # 领域模型与仓储接口，不依赖框架
│   ├── application/     # 用例编排、事务、读模型
│   ├── infrastructure/  # 持久化实现、文件存储、Spring 配置
│   └── interfaces/rest/ # REST、认证、异常处理、SSE
├── src/main/resources/ # 应用配置、数据库 schema、日志配置
└── src/test/           # 后端回归测试
frontend/
├── public/             # 不经 Vite 转换的静态源文件
└── src/                # 组件、页面、布局、路由、API 与样式
tools/                  # 本地浏览器诊断脚本
```

依赖方向为 `interfaces → application → domain`，基础设施层实现领域仓储接口。源码约定见 [AGENTS.md](AGENTS.md)。

```sh
mvn -f backend/pom.xml test  # 后端回归测试
```

前端使用 `npm run build` 验证生产构建。一体化服务启动后，可检查首页、深链接、JSON 和 SSE：

```sh
curl -fsS http://localhost:8080/ -o /dev/null
curl -fsS http://localhost:8080/articles/1 -o /dev/null
curl -fsS http://localhost:8080/api/public/site
curl -N -H 'Accept: text/event-stream' http://localhost:8080/api/public/site/stats/stream
```

最后一条会保持连接，看到事件后可按 `Ctrl+C` 退出。
