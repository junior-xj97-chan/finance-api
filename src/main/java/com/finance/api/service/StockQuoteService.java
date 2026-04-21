package com.finance.api.service;

import com.finance.api.vo.StockQuoteVO;

import java.util.List;

/**
 * 股票行情服务接口
 */
public interface StockQuoteService {

    /**
     * 获取单只股票实时行情
     */
    StockQuoteVO getQuote(String stockCode);

    /**
     * 批量获取股票行情（支持 Redis 缓存）
     */
    List<StockQuoteVO> getQuotes(List<String> stockCodes);

    /**
     * 获取股票历史 K 线
     */
    List<StockQuoteVO> getHistory(String stockCode, int days);

    /**
     * 搜索股票（模糊查询）
     */
    List<StockQuoteVO> searchStocks(String keyword, int limit);
}
