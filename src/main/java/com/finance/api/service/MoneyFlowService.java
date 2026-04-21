package com.finance.api.service;

import com.finance.api.dto.MoneyFlowQueryDTO;
import com.finance.api.vo.HsgtFlowVO;
import com.finance.api.vo.MarketFlowVO;
import com.finance.api.vo.SectorFlowVO;
import com.finance.api.vo.StockMoneyFlowVO;

import java.util.List;

/**
 * 资金流向服务接口
 */
public interface MoneyFlowService {

    // ==================== 个股资金流向 ====================

    /**
     * 获取单只股票资金流向
     * 支持按单日或区间查询
     */
    List<StockMoneyFlowVO> getStockMoneyFlow(MoneyFlowQueryDTO query);

    /**
     * 获取单只股票最新 N 日资金流向
     */
    List<StockMoneyFlowVO> getStockMoneyFlowLatest(String tsCode, int days);

    // ==================== 沪深港通资金流向 ====================

    /**
     * 获取沪深港通资金流向
     * 北向资金（沪股通+深股通）、南向资金（港股通）
     */
    List<HsgtFlowVO> getHsgtFlow(MoneyFlowQueryDTO query);

    /**
     * 获取北向资金实时数据（最近一个交易日）
     */
    HsgtFlowVO getNorthMoneyRealtime();

    /**
     * 获取近 N 日北向资金趋势
     */
    List<HsgtFlowVO> getNorthMoneyTrend(int days);

    // ==================== 大盘资金流向 ====================

    /**
     * 获取大盘资金流向
     * 沪深两市整体资金流向（东方财富口径）
     */
    List<MarketFlowVO> getMarketFlow(MoneyFlowQueryDTO query);

    /**
     * 获取今日大盘资金流向
     */
    MarketFlowVO getTodayMarketFlow();

    // ==================== 行业板块资金流向 ====================

    /**
     * 获取行业资金流向排名（东方财富口径）
     */
    List<SectorFlowVO> getIndustryFlowRank(MoneyFlowQueryDTO query, int limit);

    /**
     * 获取概念板块资金流向排名
     */
    List<SectorFlowVO> getConceptFlowRank(MoneyFlowQueryDTO query, int limit);

    // ==================== 汇总分析 ====================

    /**
     * 获取股票资金流向综合分析
     * 包含近 N 日资金净流入、超大单占比、资金趋势
     */
    Object getStockFlowAnalysis(String tsCode, int days);
}
