package com.finance.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 添加自选股请求DTO
 */
@Data
@Schema(description = "添加自选股请求")
public class WatchlistAddDTO {

    @NotBlank(message = "股票代码不能为空")
    @Schema(description = "股票代码，如 000001.SZ")
    private String stockCode;

    @NotBlank(message = "股票名称不能为空")
    @Schema(description = "股票名称")
    private String stockName;

    @NotBlank(message = "市场不能为空")
    @Schema(description = "市场：SH / SZ / HK / US")
    private String market;

    @Schema(description = "标签，逗号分隔")
    private String tags;

    @Schema(description = "备注")
    private String note;
}
