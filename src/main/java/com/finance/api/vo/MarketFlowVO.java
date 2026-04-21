package com.finance.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 大盘资金流向 VO
 * 基于 doc_id=345 moneyflow_mkt_dc 接口（东方财富口径）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "大盘资金流向")
public class MarketFlowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 交易日期 */
    private LocalDate tradeDate;

    // ===== 指数表现 =====
    /** 上证收盘价（点） */
    private BigDecimal closeSh;

    /** 上证涨跌幅（%） */
    private BigDecimal pctChangeSh;

    /** 深证收盘价（点） */
    private BigDecimal closeSz;

    /** 深证涨跌幅（%） */
    private BigDecimal pctChangeSz;

    // ===== 主力资金（超大单+大单）=====
    /** 主力净流入净额（元） */
    private BigDecimal netAmount;

    /** 主力净流入净占比（%） */
    private BigDecimal netAmountRate;

    // ===== 超大单（>=100万）=====
    /** 超大单净流入净额（元） */
    private BigDecimal buyElgAmount;

    /** 超大单净流入净占比（%） */
    private BigDecimal buyElgAmountRate;

    // ===== 大单（20-100万）=====
    /** 大单净流入净额（元） */
    private BigDecimal buyLgAmount;

    /** 大单净流入净占比（%） */
    private BigDecimal buyLgAmountRate;

    // ===== 中单（5-20万）=====
    /** 中单净流入净额（元） */
    private BigDecimal buyMdAmount;

    /** 中单净流入净占比（%） */
    private BigDecimal buyMdAmountRate;

    // ===== 小单（<5万）=====
    /** 小单净流入净额（元） */
    private BigDecimal buySmAmount;

    /** 小单净流入净占比（%） */
    private BigDecimal buySmAmountRate;

    /** 资金流向方向：流入/流出 */
    private String direction;
}
