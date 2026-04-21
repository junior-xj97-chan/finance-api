package com.finance.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 自选股实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("stock_watchlist")
public class StockWatchlist extends BaseEntity {

    /** 用户 ID */
    private Long userId;

    /** 股票代码 */
    private String stockCode;

    /** 股票名称 */
    private String stockName;

    /** 市场（SH/SZ/HK/US） */
    private String market;

    /** 标签 */
    private String tags;

    /** 备注 */
    private String note;
}
