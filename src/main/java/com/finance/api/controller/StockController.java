package com.finance.api.controller;

import com.finance.api.common.Result;
import com.finance.api.service.StockQuoteService;
import com.finance.api.vo.StockQuoteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 股票行情控制器
 */
@Tag(name = "股票行情", description = "实时行情、历史 K 线、股票搜索")
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockQuoteService stockQuoteService;

    @GetMapping("/quote/{stockCode}")
    @Operation(summary = "获取单只股票实时行情",
               description = "优先从 Redis 缓存获取，缓存未命中时从 NeoData 拉取，缓存 30 秒")
    public Result<StockQuoteVO> getQuote(
            @Parameter(description = "股票代码，如 000001.SZ") @PathVariable String stockCode) {
        return Result.ok(stockQuoteService.getQuote(stockCode));
    }

    @PostMapping("/quotes")
    @Operation(summary = "批量获取股票行情",
               description = "支持批量查询，自动处理缓存未命中，热点数据走 Redis")
    public Result<List<StockQuoteVO>> getQuotes(
            @RequestBody List<String> stockCodes) {
        return Result.ok(stockQuoteService.getQuotes(stockCodes));
    }

    @GetMapping("/history/{stockCode}")
    @Operation(summary = "获取股票历史 K 线",
               description = "返回近 N 天的日 K 数据，缓存有效期 5 分钟")
    public Result<List<StockQuoteVO>> getHistory(
            @Parameter(description = "股票代码") @PathVariable String stockCode,
            @Parameter(description = "天数，默认 30") @RequestParam(defaultValue = "30") int days) {
        return Result.ok(stockQuoteService.getHistory(stockCode, days));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索股票",
               description = "按代码或名称模糊搜索，返回基础行情数据")
    public Result<List<StockQuoteVO>> search(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "返回数量，默认 10") @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(stockQuoteService.searchStocks(keyword, limit));
    }
}
