package com.finance.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票行情VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "股票行情")
public class StockQuoteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 股票代码 */
    private String tsCode;

    /** 股票名称 */
    private String name;

    /** 最新价 */
    private BigDecimal close;

    /** 昨收价 */
    private BigDecimal preClose;

    /** 涨跌幅（%） */
    private BigDecimal pctChange;

    /** 涨跌额 */
    private BigDecimal change;

    /** 开盘价 */
    private BigDecimal open;

    /** 最高价 */
    private BigDecimal high;

    /** 最低价 */
    private BigDecimal low;

    /** 成交量（手） */
    private Long volume;

    /** 成交额（元） */
    private BigDecimal amount;

    /** 换手率（%） */
    private BigDecimal turnoverRate;

    /** 动态市盈率 */
    private BigDecimal pe;

    /** 市净率 */
    private BigDecimal pb;

    /** 总市值 */
    private BigDecimal totalMarketCap;

    /** 流通市值 */
    private BigDecimal floatMarketCap;

    /** 数据时间 */
    private LocalDateTime quoteTime;
}
