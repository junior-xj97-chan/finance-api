package com.finance.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 沪深港通资金流向 VO
 * 基于 doc_id=47 moneyflow_hsgt 接口
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "沪深港通资金流向（北向/南向）")
public class HsgtFlowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 交易日期 */
    private LocalDate tradeDate;

    /** 沪股通（百万元） */
    private BigDecimal hgt;

    /** 深股通（百万元） */
    private BigDecimal sgt;

    /** 北向资金（百万元），即沪股通+深股通 */
    private BigDecimal northMoney;

    /** 北向资金净流入（万元） */
    private BigDecimal northMoneyWan;

    /** 南向资金（百万元），港股通 */
    private BigDecimal southMoney;

    /** 南向资金净流入（万元） */
    private BigDecimal southMoneyWan;

    /** 港股通（上海） */
    private BigDecimal ggtSs;

    /** 港股通（深圳） */
    private BigDecimal ggtSz;

    /** 北向资金流向状态：净买入/净卖出 */
    private String direction;
}
