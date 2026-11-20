package com.blog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_article")
public class ArticlePo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;

    /**
     * 可清空字段必须用 ALWAYS 更新策略。
     * <p>
     * MyBatis-Plus 默认 NOT_NULL：值为 null 时该列不会进入 UPDATE 语句，
     * 于是“清空摘要 / 封面 / 分类”会静默失效，旧值留在库里。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;
    private String content;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String cover;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long categoryId;

    private String status;
    private Boolean pinned;

    /**
     * 阅读量只由 {@code incrementViewCount} 的原子 UPDATE 维护，不参与聚合整体写回，
     * 否则“读取—编辑—保存”会把期间新增的计数覆盖掉（丢失更新）。
     */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long viewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 撤回为草稿时保留原发布时间，重新发布也不再覆盖，因此始终写回当前聚合值。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime publishedAt;
}
