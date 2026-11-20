package com.blog.domain.setting;

import com.blog.domain.common.DomainException;

import java.time.LocalDateTime;

/**
 * 站点运营信息聚合根（单例，固定 id = 1）。
 * <p>
 * 只承载随运营需要变更、但不属于内容的数据；站点名称/简介等静态文案仍在配置文件里。
 * 备案号允许为空：未备案或内网部署时前台不展示，不做默认值与占位符。
 */
public class SiteSetting {

    /** 单行表固定主键。 */
    public static final Long SINGLETON_ID = 1L;

    private static final int MAX_NUMBER_LENGTH = 60;
    private static final int MAX_URL_LENGTH = 200;

    private final Long id;
    private String icpNumber;
    private String policeNumber;
    private String policeUrl;
    private LocalDateTime updatedAt;

    public SiteSetting(Long id, String icpNumber, String policeNumber, String policeUrl, LocalDateTime updatedAt) {
        this.id = id;
        this.icpNumber = icpNumber;
        this.policeNumber = policeNumber;
        this.policeUrl = policeUrl;
        this.updatedAt = updatedAt;
    }

    /** 未配置时的空状态：所有字段为 null，updatedAt 为 null。 */
    public static SiteSetting empty() {
        return new SiteSetting(SINGLETON_ID, null, null, null, null);
    }

    public void revise(String icpNumber, String policeNumber, String policeUrl) {
        this.icpNumber = normalize(icpNumber, "ICP 备案号", MAX_NUMBER_LENGTH);
        this.policeNumber = normalize(policeNumber, "公安备案号", MAX_NUMBER_LENGTH);
        this.policeUrl = normalize(policeUrl, "公安备案链接", MAX_URL_LENGTH);
        if (this.policeUrl != null && !isHttpUrl(this.policeUrl)) {
            throw DomainException.invalid("公安备案链接必须以 http:// 或 https:// 开头");
        }
        // 链接只是备案号的跳转地址，没有号码时前台无法展示这一行。
        // 允许这种组合存进去等于静默丢弃用户输入，因此在领域层直接拒绝。
        if (this.policeUrl != null && this.policeNumber == null) {
            throw DomainException.invalid("填写公安备案链接时必须同时填写公安备案号");
        }
        this.updatedAt = LocalDateTime.now();
    }

    /** 空白串视为“未填写”，存 null，避免前台把空串当有值渲染出空链接。 */
    private static String normalize(String raw, String field, int maxLength) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw DomainException.invalid(field + "不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    private static boolean isHttpUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    public Long id() {
        return id;
    }

    public String icpNumber() {
        return icpNumber;
    }

    public String policeNumber() {
        return policeNumber;
    }

    public String policeUrl() {
        return policeUrl;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }
}