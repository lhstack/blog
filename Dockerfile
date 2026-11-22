# syntax=docker/dockerfile:1
FROM --platform=$BUILDPLATFORM node:22-alpine AS frontend
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/index.html frontend/vite.config.js ./
COPY frontend/src/ src/
COPY frontend/public/ public/
RUN npm run build

FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS maven

FROM maven AS jar-builder
WORKDIR /src
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY --from=frontend /src/frontend/dist frontend/dist
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -f backend/pom.xml package && cp backend/target/blog-backend-*.jar /tmp/blog.jar

FROM eclipse-temurin:21-jre-noble AS jar-runtime
RUN apt-get update && apt-get install -y --no-install-recommends wget && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -g 10001 blog && useradd -u 10001 -g blog -M blog && \
    mkdir -p /var/lib/blog /var/log/blog && chown blog:blog /var/lib/blog /var/log/blog
WORKDIR /opt/blog
COPY --from=jar-builder /tmp/blog.jar ./blog.jar
USER 10001:10001
ENV BLOG_DATA_DIR=/var/lib/blog BLOG_LOG_DIR=/var/log/blog SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/opt/blog/blog.jar"]

FROM ghcr.io/graalvm/native-image-community:25.0.1-ol8 AS builder
ARG TARGETARCH
ENV MAVEN_HOME=/usr/share/maven
ENV PATH=$MAVEN_HOME/bin:$PATH
COPY --from=maven /usr/share/maven /usr/share/maven
WORKDIR /src
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY --from=frontend /src/frontend/dist frontend/dist
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -f backend/pom.xml -Pnative native:compile
RUN set -eu; \
    mkdir -p /out/lib /out/sqlite; cp backend/target/blog /out/blog; \
    case "$TARGETARCH" in \
      amd64) loader=ld-linux-x86-64.so.2; sqlite_arch=x86_64 ;; \
      arm64) loader=ld-linux-aarch64.so.1; sqlite_arch=aarch64 ;; \
      *) echo "Unsupported target: $TARGETARCH" >&2; exit 1 ;; \
    esac; \
    cd /tmp; jar xf /src/backend/target/blog-backend-*.jar; \
    jar xf BOOT-INF/lib/sqlite-jdbc-*.jar "org/sqlite/native/Linux/$sqlite_arch/libsqlitejdbc.so"; \
    cp "org/sqlite/native/Linux/$sqlite_arch/libsqlitejdbc.so" /out/sqlite/; \
    ldd /out/blog > /tmp/blog-libs; \
    ldd /out/sqlite/libsqlitejdbc.so >> /tmp/blog-libs; \
    if grep -q 'not found' /tmp/blog-libs; then cat /tmp/blog-libs >&2; exit 1; fi; \
    for lib in $(awk '$2 == "=>" && $3 ~ /^\// {print $3}' /tmp/blog-libs | sort -u); do \
      cp -L "$lib" /out/lib/; \
    done; \
    cp -L "$(awk -v loader="$loader" '$1 ~ ("/" loader "$") {print $1; exit}' /tmp/blog-libs)" /out/ld-linux.so

# The native image and SQLite JNI library are glibc-linked. Keep their loader and
# runtime together; do not use musl's loader or gcompat to load them.
FROM alpine:3.23 AS native-runtime
RUN apk add --no-cache ca-certificates && \
    addgroup -g 10001 blog && adduser -D -H -u 10001 -G blog blog && \
    mkdir -p /var/lib/blog /var/log/blog && chown blog:blog /var/lib/blog /var/log/blog
COPY --from=builder /out/ /opt/blog/
USER 10001:10001
WORKDIR /opt/blog
ENV BLOG_DATA_DIR=/var/lib/blog BLOG_LOG_DIR=/var/log/blog SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["/opt/blog/ld-linux.so", "--library-path", "/opt/blog/lib", "/opt/blog/blog", "-Dorg.sqlite.lib.path=/opt/blog/sqlite"]
