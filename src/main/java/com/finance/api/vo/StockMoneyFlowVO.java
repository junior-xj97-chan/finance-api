package com.finance.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 个股资金流向 VO
 * 基于 doc_id=170 moneyflow 接口
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "个股资金流向")
public class StockMoneyFlowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** TS股票代码 */
    private String tsCode;

    /** 交易日期 */
    private LocalDate tradeDate;

    // ===== 小单（<5万）=====
    /** 小单买入量（手） */
    private Long buySmVol;

    /** 小单买入金额（万元） */
    private BigDecimal buySmAmount;

    /** 小单卖出量（手） */
    private Long sellSmVol;

    /** 小单卖出金额（万元） */
    private BigDecimal sellSmAmount;

    // ===== 中单（5-20万）=====
    /** 中单买入量（手） */
    private Long buyMdVol;

    /** 中单买入金额（万元） */
    private BigDecimal buyMdAmount;

    /** 中单卖出量（手） */
    private Long sellMdVol;

    /** 中单卖出金额（万元） */
    private BigDecimal sellMdAmount;

    // ===== 大单（20-100万）=====
    /** 大单买入量（手） */
    private Long buyLgVol;

    /** 大单买入金额（万元） */
    private BigDecimal buyLgAmount;

    /** 大单卖出量（手） */
    private Long sellLgVol;

    /** 大单卖出金额（万元） */
    private BigDecimal sellLgAmount;

    // ===== 特大单（>=100万）=====
    /** 特大单买入量（手） */
    private Long buyElgVol;

    /** 特大单买入金额（万元） */
    private BigDecimal buyElgAmount;

    /** 特大单卖出量（手） */
    private Long sellElgVol;

    /** 特大单卖出金额（万元） */
    private BigDecimal sellElgAmount;

    // ===== 汇总 =====
    /** 净流入量（手） */
    private Long netMfVol;

    /** 净流入额（万元） */
    private BigDecimal netMfAmount;

    /** 净流入额（元），方便阅读 */
    private BigDecimal netMfAmountYuan;

    /** 主力净流入额（万元），大单+特大单 */
    private BigDecimal mainNetMfAmount;

    /** 主力净流入占比（%） */
    private BigDecimal mainNetMfRate;
}
