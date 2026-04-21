package com.finance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.api.common.BizCode;
import com.finance.api.common.exception.BusinessException;
import com.finance.api.dto.WatchlistAddDTO;
import com.finance.api.entity.StockWatchlist;
import com.finance.api.mapper.StockWatchlistMapper;
import com.finance.api.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 自选股服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl implements WatchlistService {

    private final StockWatchlistMapper watchlistMapper;

    /** 自选股最大数量 */
    private static final int MAX_WATCHLIST_SIZE = 100;

    @Override
    public StockWatchlist add(Long userId, WatchlistAddDTO dto) {
        // 检查是否已存在
        if (isInWatchlist(userId, dto.getStockCode())) {
            throw new BusinessException(BizCode.WATCHLIST_ALREADY_EXISTS);
        }

        // 检查数量上限
        Long count = watchlistMapper.selectCount(
                new LambdaQueryWrapper<StockWatchlist>()
                        .eq(StockWatchlist::getUserId, userId)
        );
        if (count >= MAX_WATCHLIST_SIZE) {
            throw new BusinessException(BizCode.WATCHLIST_FULL);
        }

        // 插入
        StockWatchlist watchlist = new StockWatchlist();
        watchlist.setUserId(userId);
        watchlist.setStockCode(dto.getStockCode());
        watchlist.setStockName(dto.getStockName());
        watchlist.setMarket(dto.getMarket());
        watchlist.setTags(dto.getTags());
        watchlist.setNote(dto.getNote());

        watchlistMapper.insert(watchlist);
        log.info("用户 {} 添加自选股: {}", userId, dto.getStockCode());

        return watchlist;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long watchlistId) {
        int rows = watchlistMapper.delete(
                new LambdaQueryWrapper<StockWatchlist>()
                        .eq(StockWatchlist::getId, watchlistId)
                        .eq(StockWatchlist::getUserId, userId)
        );
        if (rows == 0) {
            throw new BusinessException(BizCode.WATCHLIST_NOT_FOUND);
        }
        log.info("用户 {} 删除自选股 ID: {}", userId, watchlistId);
    }

    @Override
    public List<StockWatchlist> listByUser(Long userId) {
        return watchlistMapper.selectList(
                new LambdaQueryWrapper<StockWatchlist>()
                        .eq(StockWatchlist::getUserId, userId)
                        .orderByDesc(StockWatchlist::getCreateTime)
        );
    }

    @Override
    public boolean isInWatchlist(Long userId, String stockCode) {
        Long count = watchlistMapper.selectCount(
                new LambdaQueryWrapper<StockWatchlist>()
                        .eq(StockWatchlist::getUserId, userId)
                        .eq(StockWatchlist::getStockCode, stockCode)
        );
        return count > 0;
    }
}
