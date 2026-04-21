package com.finance.api.service.impl;

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.api.common.BizCode;
import com.finance.api.common.exception.BusinessException;
import com.finance.api.dto.MoneyFlowQueryDTO;
import com.finance.api.service.MoneyFlowService;
import com.finance.api.vo.HsgtFlowVO;
import com.finance.api.vo.MarketFlowVO;
import com.finance.api.vo.SectorFlowVO;
import com.finance.api.vo.StockMoneyFlowVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 资金流向服务实现
 * 整合 NeoData 金融数据 API + Redis 缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyFlowServiceImpl implements MoneyFlowService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String API_BASE = "https://www.codebuddy.cn/v2/tool/financedata";

    // ==================== 缓存配置 ====================
    private static final String STOCK_FLOW_CACHE = "mflow:stock:";
    private static final String HSGT_CACHE = "mflow:hsgt:";
    private static final String MARKET_FLOW_CACHE = "mflow:market:";
    private static final String IND_FLOW_CACHE = "mflow:ind:";
    private static final long SHORT_TTL = 60L;         // 1 分钟（当日数据）
    private static final long MEDIUM_TTL = 300L;      // 5 分钟
    private static final long LONG_TTL = 900L;        // 15 分钟（历史数据）

    // ==================== 个股资金流向 ====================

    @Override
    public List<StockMoneyFlowVO> getStockMoneyFlow(MoneyFlowQueryDTO query) {
        if (query.getTsCode() == null && query.getTradeDate() == null
                && query.getStartDate() == null && query.getEndDate() == null) {
            throw new BusinessException(BizCode.PARAM_ERROR, "请传入股票代码或日期参数");
        }

        String tsCode = query.getTsCode();
        LocalDate endDate = query.getEndDate() != null ? query.getEndDate()
                : (query.getTradeDate() != null ? query.getTradeDate() : LocalDate.now());
        LocalDate startDate = query.getStartDate() != null ? query.getStartDate()
                : endDate.minusDays(query.getDays() != null ? query.getDays() - 1 : 4);

        // 缓存 key
        String cacheKey = STOCK_FLOW_CACHE + (tsCode != null ? tsCode + ":" : "")
                + startDate + "_" + endDate;

        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return parseListFromCache(cached, StockMoneyFlowVO.class);
        }

        try {
            Map<String, String> params = new LinkedHashMap<>();
            if (tsCode != null) params.put("ts_code", tsCode);
            if (query.getStartDate() != null || query.getTradeDate() != null) {
                LocalDate sd = query.getStartDate() != null ? query.getStartDate() : query.getTradeDate();
                params.put("start_date", sd.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            }
            if (query.getEndDate() != null) {
                params.put("end_date", endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            } else if (query.getTradeDate() == null) {
                params.put("end_date", endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            }

            List<StockMoneyFlowVO> result = callMoneyFlowApi("moneyflow", params);
            if (result != null && !result.isEmpty()) {
                stringRedisTemplate.opsForValue().set(cacheKey, serializeToJson(result), SHORT_TTL, TimeUnit.SECONDS);
            }
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取个股资金流向失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<StockMoneyFlowVO> getStockMoneyFlowLatest(String tsCode, int days) {
        MoneyFlowQueryDTO query = new MoneyFlowQueryDTO();
        query.setTsCode(tsCode);
        query.setEndDate(LocalDate.now());
        query.setDays(days);
        return getStockMoneyFlow(query);
    }

    // ==================== 沪深港通资金流向 ====================

    @Override
    public List<HsgtFlowVO> getHsgtFlow(MoneyFlowQueryDTO query) {
        if (query.getTradeDate() == null && query.getStartDate() == null) {
            // 默认查最近 5 个交易日
            query.setEndDate(LocalDate.now());
            query.setDays(5);
        }

        LocalDate endDate = query.getEndDate() != null ? query.getEndDate()
                : (query.getTradeDate() != null ? query.getTradeDate() : LocalDate.now());
        LocalDate startDate = query.getStartDate() != null ? query.getStartDate()
                : endDate.minusDays(query.getDays() != null ? query.getDays() - 1 : 4);

        String cacheKey = HSGT_CACHE + startDate + "_" + endDate;

        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return parseListFromCache(cached, HsgtFlowVO.class);
        }

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("start_date", startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            params.put("end_date", endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            List<HsgtFlowVO> result = callHsgtApi(params);
            if (result != null && !result.isEmpty()) {
                stringRedisTemplate.opsForValue().set(cacheKey, serializeToJson(result), SHORT_TTL, TimeUnit.SECONDS);
            }
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取沪深港通资金流向失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public HsgtFlowVO getNorthMoneyRealtime() {
        List<HsgtFlowVO> flows = getHsgtFlow(new MoneyFlowQueryDTO());
        return flows.isEmpty() ? null : flows.get(flows.size() - 1);
    }

    @Override
    public List<HsgtFlowVO> getNorthMoneyTrend(int days) {
        MoneyFlowQueryDTO query = new MoneyFlowQueryDTO();
        query.setEndDate(LocalDate.now());
        query.setDays(days);
        return getHsgtFlow(query);
    }

    // ==================== 大盘资金流向 ====================

    @Override
    public List<MarketFlowVO> getMarketFlow(MoneyFlowQueryDTO query) {
        LocalDate endDate = query.getEndDate() != null ? query.getEndDate()
                : (query.getTradeDate() != null ? query.getTradeDate() : LocalDate.now());
        LocalDate startDate = query.getStartDate() != null ? query.getStartDate()
                : endDate.minusDays(query.getDays() != null ? query.getDays() - 1 : 4);

        String cacheKey = MARKET_FLOW_CACHE + startDate + "_" + endDate;

        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return parseListFromCache(cached, MarketFlowVO.class);
        }

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("start_date", startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            params.put("end_date", endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            List<MarketFlowVO> result = callMarketFlowApi(params);
            if (result != null && !result.isEmpty()) {
                stringRedisTemplate.opsForValue().set(cacheKey, serializeToJson(result), SHORT_TTL, TimeUnit.SECONDS);
            }
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取大盘资金流向失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public MarketFlowVO getTodayMarketFlow() {
        List<MarketFlowVO> flows = getMarketFlow(new MoneyFlowQueryDTO());
        return flows.isEmpty() ? null : flows.get(flows.size() - 1);
    }

    // ==================== 行业板块资金流向 ====================

    @Override
    public List<SectorFlowVO> getIndustryFlowRank(MoneyFlowQueryDTO query, int limit) {
        LocalDate tradeDate = query.getTradeDate() != null ? query.getTradeDate()
                : (query.getStartDate() != null ? query.getStartDate() : LocalDate.now());

        String cacheKey = IND_FLOW_CACHE + "ind:" + tradeDate;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            List<SectorFlowVO> result = parseListFromCache(cached, SectorFlowVO.class);
            return result.stream().limit(limit).toList();
        }

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("trade_date", tradeDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            List<SectorFlowVO> result = callSectorFlowApi("moneyflow_ind_dc", params);
            if (result != null && !result.isEmpty()) {
                // 按净流入排序
                result.sort((a, b) -> b.getNetAmount().compareTo(a.getNetAmount()));
                // 添加排名
                for (int i = 0; i < result.size(); i++) {
                    result.get(i).setRank(i + 1);
                }
                stringRedisTemplate.opsForValue().set(cacheKey, serializeToJson(result), MEDIUM_TTL, TimeUnit.SECONDS);
            }
            return result != null ? result.stream().limit(limit).toList() : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取行业资金流向排名失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<SectorFlowVO> getConceptFlowRank(MoneyFlowQueryDTO query, int limit) {
        LocalDate tradeDate = query.getTradeDate() != null ? query.getTradeDate()
                : (query.getStartDate() != null ? query.getStartDate() : LocalDate.now());

        String cacheKey = IND_FLOW_CACHE + "concept:" + tradeDate;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            List<SectorFlowVO> result = parseListFromCache(cached, SectorFlowVO.class);
            return result.stream().limit(limit).toList();
        }

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("trade_date", tradeDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            List<SectorFlowVO> result = callSectorFlowApi("moneyflow_ind_dc", params);
            if (result != null && !result.isEmpty()) {
                result.sort((a, b) -> b.getNetAmount().compareTo(a.getNetAmount()));
                for (int i = 0; i < result.size(); i++) {
                    result.get(i).setRank(i + 1);
                }
                stringRedisTemplate.opsForValue().set(cacheKey, serializeToJson(result), MEDIUM_TTL, TimeUnit.SECONDS);
            }
            return result != null ? result.stream().limit(limit).toList() : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取概念板块资金流向排名失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 综合分析 ====================

    @Override
    public Object getStockFlowAnalysis(String tsCode, int days) {
        if (tsCode == null) {
            throw new BusinessException(BizCode.PARAM_ERROR, "股票代码不能为空");
        }

        // 缓存 5 分钟
        String cacheKey = STOCK_FLOW_CACHE + "analysis:" + tsCode + ":" + days;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, Map.class);
            } catch (Exception e) {
                log.warn("分析缓存解析失败: {}", e.getMessage());
            }
        }

        // 1. 获取近 N 日资金流向
        List<StockMoneyFlowVO> flows = getStockMoneyFlowLatest(tsCode, days);
        if (flows.isEmpty()) {
            throw new BusinessException(BizCode.QUOTE_DATA_EMPTY, "暂无资金流向数据");
        }

        // 2. 计算汇总指标
        BigDecimal totalNetMf = BigDecimal.ZERO;
        BigDecimal totalMainNetMf = BigDecimal.ZERO;
        int inflowDays = 0;

        for (StockMoneyFlowVO flow : flows) {
            if (flow.getNetMfAmount() != null) {
                totalNetMf = totalNetMf.add(flow.getNetMfAmount());
            }
            if (flow.getMainNetMfAmount() != null) {
                totalMainNetMf = totalMainNetMf.add(flow.getMainNetMfAmount());
            }
            if (flow.getNetMfAmount() != null && flow.getNetMfAmount().compareTo(BigDecimal.ZERO) > 0) {
                inflowDays++;
            }
        }

        // 3. 趋势判断
        String trend;
        if (flows.size() >= 3) {
            BigDecimal recent = flows.stream()
                    .filter(f -> f.getNetMfAmount() != null)
                    .limit(3)
                    .map(StockMoneyFlowVO::getNetMfAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal earlier = flows.stream()
                    .filter(f -> f.getNetMfAmount() != null)
                    .skip(3)
                    .limit(3)
                    .map(StockMoneyFlowVO::getNetMfAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            trend = recent.compareTo(earlier) > 0 ? "加速流入" : (recent.compareTo(earlier) < 0 ? "资金缩减" : "平稳");
        } else {
            trend = totalNetMf.compareTo(BigDecimal.ZERO) > 0 ? "净流入" : "净流出";
        }

        // 4. 组装分析结果
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("tsCode", tsCode);
        analysis.put("days", days);
        analysis.put("dataDays", flows.size());
        analysis.put("totalNetMfAmount", totalNetMf.setScale(2, RoundingMode.HALF_UP));
        analysis.put("totalMainNetMfAmount", totalMainNetMf.setScale(2, RoundingMode.HALF_UP));
        analysis.put("avgNetMfAmount", totalNetMf.divide(BigDecimal.valueOf(flows.size()), 2, RoundingMode.HALF_UP));
        analysis.put("inflowDays", inflowDays);
        analysis.put("inflowDaysRate", BigDecimal.valueOf(inflowDays)
                .divide(BigDecimal.valueOf(flows.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP));
        analysis.put("trend", trend);
        analysis.put("direction", totalNetMf.compareTo(BigDecimal.ZERO) > 0 ? "流入" : "流出");
        analysis.put("dailyFlows", flows);

        // 缓存
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(analysis), MEDIUM_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("分析结果缓存失败: {}", e.getMessage());
        }

        return analysis;
    }

    // ==================== 内部方法：API 调用 ====================

    /**
     * 调用个股资金流向 API (moneyflow)
     */
    private List<StockMoneyFlowVO> callMoneyFlowApi(String apiName, Map<String, String> params) {
        String json = postApi(apiName, params);
        if (json == null) return Collections.emptyList();

        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.get("code").asInt() != 0) {
                log.warn("API调用失败 [{}]: {}", apiName, root.get("msg").asText());
                return Collections.emptyList();
            }

            JsonNode data = root.get("data");
            if (data == null || !data.has("items")) return Collections.emptyList();

            JsonNode fieldsNode = data.get("fields");
            JsonNode items = data.get("items");

            List<StockMoneyFlowVO> result = new ArrayList<>();
            for (JsonNode item : items) {
                StockMoneyFlowVO vo = mapToStockMoneyFlow(fieldsNode, item);
                result.add(vo);
            }
            return result;
        } catch (Exception e) {
            log.error("解析资金流向数据失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 调用沪深港通资金流向 API (moneyflow_hsgt)
     */
    private List<HsgtFlowVO> callHsgtApi(Map<String, String> params) {
        String json = postApi("moneyflow_hsgt", params);
        if (json == null) return Collections.emptyList();

        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.get("code").asInt() != 0) {
                log.warn("API调用失败: {}", root.get("msg").asText());
                return Collections.emptyList();
            }

            JsonNode data = root.get("data");
            if (data == null || !data.has("items")) return Collections.emptyList();

            JsonNode fieldsNode = data.get("fields");
            JsonNode items = data.get("items");

            List<HsgtFlowVO> result = new ArrayList<>();
            for (JsonNode item : items) {
                HsgtFlowVO vo = mapToHsgtFlow(fieldsNode, item);
                result.add(vo);
            }
            return result;
        } catch (Exception e) {
            log.error("解析沪深港通数据失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 调用大盘资金流向 API (moneyflow_mkt_dc)
     */
    private List<MarketFlowVO> callMarketFlowApi(Map<String, String> params) {
        String json = postApi("moneyflow_mkt_dc", params);
        if (json == null) return Collections.emptyList();

        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.get("code").asInt() != 0) {
                log.warn("API调用失败: {}", root.get("msg").asText());
                return Collections.emptyList();
            }

            JsonNode data = root.get("data");
            if (data == null || !data.has("items")) return Collections.emptyList();

            JsonNode fieldsNode = data.get("fields");
            JsonNode items = data.get("items");

            List<MarketFlowVO> result = new ArrayList<>();
            for (JsonNode item : items) {
                MarketFlowVO vo = mapToMarketFlow(fieldsNode, item);
                result.add(vo);
            }
            return result;
        } catch (Exception e) {
            log.error("解析大盘资金流向数据失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 调用行业/板块资金流向 API
     */
    private List<SectorFlowVO> callSectorFlowApi(String apiName, Map<String, String> params) {
        String json = postApi(apiName, params);
        if (json == null) return Collections.emptyList();

        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.get("code").asInt() != 0) {
                log.warn("API调用失败 [{}]: {}", apiName, root.get("msg").asText());
                return Collections.emptyList();
            }

            JsonNode data = root.get("data");
            if (data == null || !data.has("items")) return Collections.emptyList();

            JsonNode fieldsNode = data.get("fields");
            JsonNode items = data.get("items");

            List<SectorFlowVO> result = new ArrayList<>();
            for (JsonNode item : items) {
                SectorFlowVO vo = mapToSectorFlow(fieldsNode, item);
                result.add(vo);
            }
            return result;
        } catch (Exception e) {
            log.error("解析板块资金流向数据失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 映射方法 ====================

    private StockMoneyFlowVO mapToStockMoneyFlow(JsonNode fieldsNode, JsonNode item) {
        StockMoneyFlowVO vo = new StockMoneyFlowVO();
        Map<String, String> fieldMap = new LinkedHashMap<>();
        for (int i = 0; i < fieldsNode.size(); i++) {
            fieldMap.put(fieldsNode.get(i).asText(), item.get(i) != null && !item.get(i).isNull() ? item.get(i).asText() : null);
        }

        vo.setTsCode(fieldMap.get("ts_code"));
        if (fieldMap.get("trade_date") != null) {
            vo.setTradeDate(LocalDate.parse(fieldMap.get("trade_date"), DateTimeFormatter.ofPattern("yyyyMMdd")));
        }

        vo.setBuySmVol(toLong(fieldMap.get("buy_sm_vol")));
        vo.setBuySmAmount(toDecimal(fieldMap.get("buy_sm_amount")));
        vo.setSellSmVol(toLong(fieldMap.get("sell_sm_vol")));
        vo.setSellSmAmount(toDecimal(fieldMap.get("sell_sm_amount")));
        vo.setBuyMdVol(toLong(fieldMap.get("buy_md_vol")));
        vo.setBuyMdAmount(toDecimal(fieldMap.get("buy_md_amount")));
        vo.setSellMdVol(toLong(fieldMap.get("sell_md_vol")));
        vo.setSellMdAmount(toDecimal(fieldMap.get("sell_md_amount")));
        vo.setBuyLgVol(toLong(fieldMap.get("buy_lg_vol")));
        vo.setBuyLgAmount(toDecimal(fieldMap.get("buy_lg_amount")));
        vo.setSellLgVol(toLong(fieldMap.get("sell_lg_vol")));
        vo.setSellLgAmount(toDecimal(fieldMap.get("sell_lg_amount")));
        vo.setBuyElgVol(toLong(fieldMap.get("buy_elg_vol")));
        vo.setBuyElgAmount(toDecimal(fieldMap.get("buy_elg_amount")));
        vo.setSellElgVol(toLong(fieldMap.get("sell_elg_vol")));
        vo.setSellElgAmount(toDecimal(fieldMap.get("sell_elg_amount")));
        vo.setNetMfVol(toLong(fieldMap.get("net_mf_vol")));
        vo.setNetMfAmount(toDecimal(fieldMap.get("net_mf_amount")));
        vo.setNetMfAmountYuan(toDecimal(fieldMap.get("net_mf_amount"))
                .multiply(BigDecimal.valueOf(10000)));

        // 计算主力净流入（大单+特大单）
        BigDecimal mainNet = BigDecimal.ZERO;
        if (vo.getBuyLgAmount() != null && vo.getSellLgAmount() != null) {
            mainNet = mainNet.add(vo.getBuyLgAmount().subtract(vo.getSellLgAmount()));
        }
        if (vo.getBuyElgAmount() != null && vo.getSellElgAmount() != null) {
            mainNet = mainNet.add(vo.getBuyElgAmount().subtract(vo.getSellElgAmount()));
        }
        vo.setMainNetMfAmount(mainNet.setScale(2, RoundingMode.HALF_UP));

        return vo;
    }

    private HsgtFlowVO mapToHsgtFlow(JsonNode fieldsNode, JsonNode item) {
        HsgtFlowVO vo = new HsgtFlowVO();
        Map<String, String> fieldMap = new LinkedHashMap<>();
        for (int i = 0; i < fieldsNode.size(); i++) {
            fieldMap.put(fieldsNode.get(i).asText(), item.get(i) != null && !item.get(i).isNull() ? item.get(i).asText() : null);
        }

        if (fieldMap.get("trade_date") != null) {
            vo.setTradeDate(LocalDate.parse(fieldMap.get("trade_date"), DateTimeFormatter.ofPattern("yyyyMMdd")));
        }
        vo.setHgt(toDecimal(fieldMap.get("hgt")));
        vo.setSgt(toDecimal(fieldMap.get("sgt")));
        vo.setNorthMoney(toDecimal(fieldMap.get("north_money")));
        vo.setSouthMoney(toDecimal(fieldMap.get("south_money")));
        vo.setGgtSs(toDecimal(fieldMap.get("ggt_ss")));
        vo.setGgtSz(toDecimal(fieldMap.get("ggt_sz")));

        // 转为万元
        if (vo.getNorthMoney() != null) {
            vo.setNorthMoneyWan(vo.getNorthMoney().multiply(BigDecimal.valueOf(100)));
        }
        if (vo.getSouthMoney() != null) {
            vo.setSouthMoneyWan(vo.getSouthMoney().multiply(BigDecimal.valueOf(100)));
        }

        // 判断方向
        if (vo.getNorthMoney() != null) {
            vo.setDirection(vo.getNorthMoney().compareTo(BigDecimal.ZERO) >= 0 ? "净买入" : "净卖出");
        }

        return vo;
    }

    private MarketFlowVO mapToMarketFlow(JsonNode fieldsNode, JsonNode item) {
        MarketFlowVO vo = new MarketFlowVO();
        Map<String, String> fieldMap = new LinkedHashMap<>();
        for (int i = 0; i < fieldsNode.size(); i++) {
            fieldMap.put(fieldsNode.get(i).asText(), item.get(i) != null && !item.get(i).isNull() ? item.get(i).asText() : null);
        }

        if (fieldMap.get("trade_date") != null) {
            vo.setTradeDate(LocalDate.parse(fieldMap.get("trade_date"), DateTimeFormatter.ofPattern("yyyyMMdd")));
        }
        vo.setCloseSh(toDecimal(fieldMap.get("close_sh")));
        vo.setPctChangeSh(toDecimal(fieldMap.get("pct_change_sh")));
        vo.setCloseSz(toDecimal(fieldMap.get("close_sz")));
        vo.setPctChangeSz(toDecimal(fieldMap.get("pct_change_sz")));
        vo.setNetAmount(toDecimal(fieldMap.get("net_amount")));
        vo.setNetAmountRate(toDecimal(fieldMap.get("net_amount_rate")));
        vo.setBuyElgAmount(toDecimal(fieldMap.get("buy_elg_amount")));
        vo.setBuyElgAmountRate(toDecimal(fieldMap.get("buy_elg_amount_rate")));
        vo.setBuyLgAmount(toDecimal(fieldMap.get("buy_lg_amount")));
        vo.setBuyLgAmountRate(toDecimal(fieldMap.get("buy_lg_amount_rate")));
        vo.setBuyMdAmount(toDecimal(fieldMap.get("buy_md_amount")));
        vo.setBuyMdAmountRate(toDecimal(fieldMap.get("buy_md_amount_rate")));
        vo.setBuySmAmount(toDecimal(fieldMap.get("buy_sm_amount")));
        vo.setBuySmAmountRate(toDecimal(fieldMap.get("buy_sm_amount_rate")));

        if (vo.getNetAmount() != null) {
            vo.setDirection(vo.getNetAmount().compareTo(BigDecimal.ZERO) >= 0 ? "流入" : "流出");
        }

        return vo;
    }

    private SectorFlowVO mapToSectorFlow(JsonNode fieldsNode, JsonNode item) {
        SectorFlowVO vo = new SectorFlowVO();
        Map<String, String> fieldMap = new LinkedHashMap<>();
        for (int i = 0; i < fieldsNode.size(); i++) {
            fieldMap.put(fieldsNode.get(i).asText(), item.get(i) != null && !item.get(i).isNull() ? item.get(i).asText() : null);
        }

        vo.setCode(fieldMap.get("code"));
        vo.setName(fieldMap.get("name"));
        vo.setNetAmount(toDecimal(fieldMap.get("net_amount")));
        vo.setNetAmountRate(toDecimal(fieldMap.get("net_amount_rate")));
        vo.setPctChange(toDecimal(fieldMap.get("pct_change")));
        vo.setTurnover(toDecimal(fieldMap.get("turnover")));

        if (vo.getNetAmount() != null) {
            vo.setNetAmountWan(vo.getNetAmount().divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP));
            vo.setDirection(vo.getNetAmount().compareTo(BigDecimal.ZERO) >= 0 ? "流入" : "流出");
        }

        return vo;
    }

    // ==================== 工具方法 ====================

    private String postApi(String apiName, Map<String, String> params) {
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"api_name\":\"").append(apiName).append("\",\"params\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) jsonBuilder.append(",");
                jsonBuilder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                first = false;
            }
            jsonBuilder.append("}}");

            URL url = new URL(API_BASE);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            conn.getOutputStream().write(jsonBuilder.toString().getBytes(StandardCharsets.UTF_8));

            int respCode = conn.getResponseCode();
            if (respCode != 200) {
                log.warn("HTTP请求失败: {}", respCode);
                return null;
            }

            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } catch (Exception e) {
            log.error("API调用异常 [{}]: {}", apiName, e.getMessage());
            return null;
        }
    }

    private <T> List<T> parseListFromCache(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.warn("缓存解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private BigDecimal toDecimal(String val) {
        if (val == null || val.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val).setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Long toLong(String val) {
        if (val == null || val.isEmpty()) return 0L;
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            return 0L;
        }
    }
}
