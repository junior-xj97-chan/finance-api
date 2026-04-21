package com.finance.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.api.entity.StockWatchlist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自选股 Mapper
 */
@Mapper
public interface StockWatchlistMapper extends BaseMapper<StockWatchlist> {
}
