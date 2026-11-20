package com.blog.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_user")
public class UserPo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;

    /** 可清空字段用 ALWAYS：默认 NOT_NULL 会让“清空头像 / 邮箱”静默失效。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String avatar;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String email;

    private LocalDateTime createdAt;
}
