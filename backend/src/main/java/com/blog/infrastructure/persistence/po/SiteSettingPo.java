package com.blog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站点运营信息（单行表，id 固定为 1）。
 * <p>
 * 三个可清空字段必须用 ALWAYS：默认 NOT_NULL 策略会把“清空备案号”的更新静默丢弃。
 */
@Data
@TableName("blog_site_setting")
public class SiteSettingPo {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String icpNumber;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String policeNumber;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String policeUrl;

    private LocalDateTime updatedAt;
}