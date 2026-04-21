package com.finance.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格提醒请求DTO
 */
@Data
@Schema(description = "价格提醒请求")
public class PriceAlertDTO {

    @NotBlank(message = "股票代码不能为空")
    @Schema(description = "股票代码")
    private String stockCode;

    @NotBlank(message = "股票名称不能为空")
    @Schema(description = "股票名称")
    private String stockName;

    @NotNull(message = "目标价格不能为空")
    @DecimalMin(value = "0.01", message = "目标价格必须大于0")
    @Schema(description = "目标价格")
    private BigDecimal targetPrice;

    @NotBlank(message = "提醒类型不能为空")
    @Schema(description = "提醒类型：gt-大于 lt-小于 eq-等于")
    private String alertType;
}
