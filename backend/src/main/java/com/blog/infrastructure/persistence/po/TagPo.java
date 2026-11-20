package com.blog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("blog_tag")
public class TagPo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String color;
}
