package com.finance.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类 - 所有 Entity 公共字段
 *
 * 使用方式：Entity 继承本类，自身只需声明业务字段，无需重复定义 id / createTime / updateTime / deleted
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0=正常，1=已删除） */
    @TableLogic
    private Integer deleted;
}
