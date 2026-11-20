package com.blog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("blog_category")
public class CategoryPo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String slug;

    /** 可清空字段用 ALWAYS：默认 NOT_NULL 会让“清空描述”静默失效。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    private Integer sortOrder;
}
