package com.finance.api.controller;

import com.finance.api.common.Result;
import com.finance.api.dto.MoneyFlowQueryDTO;
import com.finance.api.service.MoneyFlowService;
import com.finance.api.vo.HsgtFlowVO;
import com.finance.api.vo.MarketFlowVO;
import com.finance.api.vo.SectorFlowVO;
import com.finance.api.vo.StockMoneyFlowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资金流向控制器
 * 覆盖：个股资金流向、沪深港通（北向/南向）、大盘资金、行业板块资金流向
 */
@Tag(name = "资金流向", description = "个股/大盘/沪深港通/行业板块资金流向")
@RestController
@RequestMapping("/api/money-flow")
@RequiredArgsConstructor
public class MoneyFlowController {

    private final MoneyFlowService moneyFlowService;

    // ==================== 个股资金流向 ====================

    @GetMapping("/stock/{tsCode}")
    @Operation(summary = "获取个股资金流向",
               description = "返回股票各单级别的买卖量和金额，支持单日或区间查询。" +
                       "单：<5万 | 中单：5-20万 | 大单：20-100万 | 特大单：≥100万")
    public Result<List<StockMoneyFlowVO>> getStockMoneyFlow(
            @Parameter(description = "股票代码，如 000001.SZ") @PathVariable String tsCode,
            @Parameter(description = "交易日期，格式 yyyy-MM-dd") @RequestParam(required = false) String tradeDate,
            @Parameter(description = "开始日期，格式 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @Parameter(description = "天数，默认 5") @RequestParam(defaultValue = "5") int days) {

        MoneyFlowQueryDTO query = buildQuery(tsCode, tradeDate, startDate, endDate, days);
        List<StockMoneyFlowVO> result = moneyFlowService.getStockMoneyFlow(query);
        return Result.ok(result);
    }

    @GetMapping("/stock/{tsCode}/trend")
    @Operation(summary = "获取个股资金流向趋势分析",
               description = "返回近 N 日资金流向的汇总分析，包括净流入总额、主力净流入、天数占比、资金趋势判断")
    public Result<Object> getStockMoneyFlowTrend(
            @Parameter(description = "股票代码") @PathVariable String tsCode,
            @Parameter(description = "天数，默认 10") @RequestParam(defaultValue = "10") int days) {

        Object analysis = moneyFlowService.getStockFlowAnalysis(tsCode, days);
        return Result.<Object>ok(analysis);
    }

    // ==================== 沪深港通资金流向 ====================

    @GetMapping("/hsgt")
    @Operation(summary = "获取沪深港通资金流向",
               description = "返回北向资金（沪股通+深股通）和南向资金（港股通）的每日流向数据")
    public Result<List<HsgtFlowVO>> getHsgtFlow(
            @Parameter(description = "交易日期，格式 yyyy-MM-dd（单日）") @RequestParam(required = false) String tradeDate,
            @Parameter(description = "开始日期，格式 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @Parameter(description = "天数，默认 5") @RequestParam(defaultValue = "5") int days) {

        MoneyFlowQueryDTO query = buildQuery(null, tradeDate, startDate, endDate, days);
        List<HsgtFlowVO> result = moneyFlowService.getHsgtFlow(query);
        return Result.ok(result);
    }

    @GetMapping("/hsgt/north")
    @Operation(summary = "获取北向资金趋势",
               description = "返回近 N 个交易日的北向资金趋势（沪股通+深股通合计）")
    public Result<List<HsgtFlowVO>> getNorthMoneyTrend(
            @Parameter(description = "天数，默认 20") @RequestParam(defaultValue = "20") int days) {

        List<HsgtFlowVO> result = moneyFlowService.getNorthMoneyTrend(days);
        return Result.ok(result);
    }

    @GetMapping("/hsgt/today")
    @Operation(summary = "获取今日北向资金",
               description = "返回最新一个交易日的北向资金实时数据")
    public Result<HsgtFlowVO> getTodayNorthMoney() {
        HsgtFlowVO result = moneyFlowService.getNorthMoneyRealtime();
        return Result.ok(result);
    }

    // ==================== 大盘资金流向 ====================

    @GetMapping("/market")
    @Operation(summary = "获取大盘资金流向",
               description = "返回沪深两市整体资金流向（东方财富口径），包括超大单/大单/中单/小单的净流入和占比")
    public Result<List<MarketFlowVO>> getMarketFlow(
            @Parameter(description = "交易日期，格式 yyyy-MM-dd（单日）") @RequestParam(required = false) String tradeDate,
            @Parameter(description = "开始日期，格式 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @Parameter(description = "天数，默认 5") @RequestParam(defaultValue = "5") int days) {

        MoneyFlowQueryDTO query = buildQuery(null, tradeDate, startDate, endDate, days);
        List<MarketFlowVO> result = moneyFlowService.getMarketFlow(query);
        return Result.ok(result);
    }

    @GetMapping("/market/today")
    @Operation(summary = "获取今日大盘资金流向",
               description = "返回最新一个交易日的大盘资金流向数据")
    public Result<MarketFlowVO> getTodayMarketFlow() {
        MarketFlowVO result = moneyFlowService.getTodayMarketFlow();
        return Result.ok(result);
    }

    // ==================== 行业板块资金流向 ====================

    @GetMapping("/industry/rank")
    @Operation(summary = "获取行业资金流向排名",
               description = "按今日主力净流入额排名，支持设置返回条数，默认返回前 20 名")
    public Result<List<SectorFlowVO>> getIndustryFlowRank(
            @Parameter(description = "交易日期，格式 yyyy-MM-dd（默认今日）") @RequestParam(required = false) String tradeDate,
            @Parameter(description = "返回数量，默认 20") @RequestParam(defaultValue = "20") int limit) {

        MoneyFlowQueryDTO query = new MoneyFlowQueryDTO();
        if (tradeDate != null) {
            query.setTradeDate(java.time.LocalDate.parse(tradeDate));
        }
        List<SectorFlowVO> result = moneyFlowService.getIndustryFlowRank(query, limit);
        return Result.ok(result);
    }

    @GetMapping("/concept/rank")
    @Operation(summary = "获取概念板块资金流向排名",
               description = "按今日主力净流入额排名（概念板块），默认返回前 20 名")
    public Result<List<SectorFlowVO>> getConceptFlowRank(
            @Parameter(description = "交易日期，格式 yyyy-MM-dd（默认今日）") @RequestParam(required = false) String tradeDate,
            @Parameter(description = "返回数量，默认 20") @RequestParam(defaultValue = "20") int limit) {

        MoneyFlowQueryDTO query = new MoneyFlowQueryDTO();
        if (tradeDate != null) {
            query.setTradeDate(java.time.LocalDate.parse(tradeDate));
        }
        List<SectorFlowVO> result = moneyFlowService.getConceptFlowRank(query, limit);
        return Result.ok(result);
    }

    // ==================== 工具方法 ====================

    private MoneyFlowQueryDTO buildQuery(String tsCode, String tradeDate, String startDate,
                                         String endDate, int days) {
        MoneyFlowQueryDTO query = new MoneyFlowQueryDTO();
        query.setTsCode(tsCode);
        if (tradeDate != null && !tradeDate.isEmpty()) {
            query.setTradeDate(java.time.LocalDate.parse(tradeDate));
        }
        if (startDate != null && !startDate.isEmpty()) {
            query.setStartDate(java.time.LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            query.setEndDate(java.time.LocalDate.parse(endDate));
        }
        query.setDays(days);
        return query;
    }
}
