package com.finance.api.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 资金流向查询 DTO
 */
@Data
public class MoneyFlowQueryDTO {

    @Parameter(description = "股票代码，如 000001.SZ（个股资金流向时使用）")
    private String tsCode;

    @Parameter(description = "交易日期，格式 YYYY-MM-DD（单日查询）")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate tradeDate;

    @Parameter(description = "开始日期，格式 YYYY-MM-DD（区间查询）")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Parameter(description = "结束日期，格式 YYYY-MM-DD（区间查询）")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Parameter(description = "返回天数，默认 5（区间查询时自动忽略）")
    private Integer days = 5;
}
