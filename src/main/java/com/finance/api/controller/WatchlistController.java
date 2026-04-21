package com.finance.api.controller;

import com.finance.api.common.Result;
import com.finance.api.dto.WatchlistAddDTO;
import com.finance.api.entity.StockWatchlist;
import com.finance.api.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自选股控制器
 */
@Tag(name = "自选股管理", description = "自选股 CRUD 操作")
@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @PostMapping
    @Operation(summary = "添加自选股")
    public Result<StockWatchlist> add(
            @Parameter(description = "用户 ID", hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody WatchlistAddDTO dto) {
        return Result.ok("添加成功", watchlistService.add(userId, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除自选股")
    public Result<Void> remove(
            @Parameter(description = "用户 ID", hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "自选股记录 ID") @PathVariable Long id) {
        watchlistService.remove(userId, id);
        return Result.ok("删除成功", null);
    }

    @GetMapping
    @Operation(summary = "查询我的自选股列表")
    public Result<List<StockWatchlist>> list(
            @Parameter(description = "用户 ID", hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(watchlistService.listByUser(userId));
    }

    @GetMapping("/check/{stockCode}")
    @Operation(summary = "检查股票是否已在自选")
    public Result<Boolean> check(
            @Parameter(description = "用户 ID", hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "股票代码") @PathVariable String stockCode) {
        return Result.ok(watchlistService.isInWatchlist(userId, stockCode));
    }
}
