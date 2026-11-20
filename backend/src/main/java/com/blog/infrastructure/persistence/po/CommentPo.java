package com.blog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_comment")
public class CommentPo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String author;
    private String email;
    private String website;
    private String content;
    private String ip;
    private String status;
    private LocalDateTime createdAt;
}
