package com.blog.interfaces.rest.support;

import org.springframework.core.NativeDetector;
import org.springframework.http.HttpStatus;

public final class AvatarUploadSupport {

    private AvatarUploadSupport() {
    }

    public static void requireImageDecoder() {
        // 头像上传必须完整解码校验，不能在缺少 AWT/ImageIO 支持时跳过验证。
        if (NativeDetector.inNativeImage()) {
            throw new BizException(HttpStatus.NOT_IMPLEMENTED.value(), "原生版本暂不支持上传头像，请填写图片 URL 或使用 JAR 版本");
        }
    }
}
