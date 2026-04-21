package com.finance.api.service;

import com.finance.api.dto.WatchlistAddDTO;
import com.finance.api.entity.StockWatchlist;

import java.util.List;

/**
 * 自选股服务接口
 */
public interface WatchlistService {

    /**
     * 添加自选股
     */
    StockWatchlist add(Long userId, WatchlistAddDTO dto);

    /**
     * 删除自选股
     */
    void remove(Long userId, Long watchlistId);

    /**
     * 查询用户自选股列表
     */
    List<StockWatchlist> listByUser(Long userId);

    /**
     * 检查股票是否已在自选
     */
    boolean isInWatchlist(Long userId, String stockCode);
}
