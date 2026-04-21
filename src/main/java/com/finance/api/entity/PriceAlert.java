package com.finance.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格提醒实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("price_alert")
public class PriceAlert extends BaseEntity {

    /** 用户 ID */
    private Long userId;

    /** 股票代码 */
    private String stockCode;

    /** 股票名称 */
    private String stockName;

    /** 目标价格 */
    private BigDecimal targetPrice;

    /** 提醒类型：gt-大于 lt-小于 eq-等于 */
    private String alertType;

    /** 条件描述 */
    private String conditionDesc;

    /** 是否已触发（0-未触发 1-已触发） */
    private Integer isTriggered;

    /** 触发时间 */
    private LocalDateTime triggeredAt;

    /** 是否启用（0-禁用 1-启用） */
    private Integer isEnabled;
}
