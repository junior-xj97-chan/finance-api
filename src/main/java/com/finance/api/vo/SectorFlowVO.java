package com.finance.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 行业/板块资金流向排名 VO
 * 基于 doc_id=343/344 moneyflow_ind_ths / moneyflow_ind_dc 接口
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "行业/板块资金流向排名")
public class SectorFlowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 行业/板块代码 */
    private String code;

    /** 行业/板块名称 */
    private String name;

    /** 今日主力净流入额（元） */
    private BigDecimal netAmount;

    /** 今日主力净流入占比（%） */
    private BigDecimal netAmountRate;

    /** 今日主力净流入额（万元），方便阅读 */
    private BigDecimal netAmountWan;

    /** 涨跌幅（%） */
    private BigDecimal pctChange;

    /** 成交额（元） */
    private BigDecimal turnover;

    /** 资金流向：流入/流出 */
    private String direction;

    /** 排名序号 */
    private Integer rank;
}
