package com.finance.api.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.api.common.BizCode;
import com.finance.api.common.exception.BusinessException;
import com.finance.api.service.StockQuoteService;
import com.finance.api.vo.StockQuoteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 股票行情服务实现
 * 接入 NeoData 金融数据 API + Redis 缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockQuoteServiceImpl implements StockQuoteService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** NeoData API 基础地址 */
    private static final String API_BASE = "https://www.codebuddy.cn/v2/tool/financedata";

    // ==================== 缓存配置 ====================
    private static final String QUOTE_CACHE_PREFIX = "quote:rt:";    // 实时行情缓存
    private static final String HISTORY_CACHE_PREFIX = "quote:his:"; // 历史K线缓存
    private static final String SEARCH_CACHE_PREFIX = "quote:sch:";  // 搜索结果缓存
    private static final long RT_CACHE_TTL = 30L;       // 实时行情 30s
    private static final long HIS_CACHE_TTL = 300L;     // 历史K线 5min
    private static final long SEARCH_CACHE_TTL = 3600L; // 搜索结果 1h

    // ==================== 行情查询 ====================

    @Override
    public StockQuoteVO getQuote(String stockCode) {
        // 1. 查 Redis 缓存
        String cacheKey = QUOTE_CACHE_PREFIX + stockCode;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("实时行情缓存命中: {}", stockCode);
            return parseQuoteFromCache(cached);
        }

        // 2. 缓存未命中，从 NeoData 获取（优先实时日线，降级历史日线）
        StockQuoteVO quote = null;
        
        // 2.1 先尝试实时行情（如果未被限流，缓存 30s）
        try {
            quote = fetchRealtimeQuote(stockCode);
        } catch (Exception e) {
            log.debug("实时行情获取失败 [{}]: {}", stockCode, e.getMessage());
        }
        
        // 2.2 降级方案：通过日线接口获取最近一条数据
        if (quote == null) {
            try {
                quote = fetchLatestDaily(stockCode);
            } catch (Exception e) {
                log.debug("日线数据获取失败 [{}]: {}", stockCode, e.getMessage());
            }
        }
        
        // 2.3 最终兜底：缓存一个占位数据，避免频繁请求
        if (quote == null) {
            log.warn("获取 {} 行情失败，返回空数据", stockCode);
            // 不抛异常，返回 null 让调用方处理
            return null;
        }

        // 3. 写入缓存
        writeQuoteToCache(cacheKey, quote);
        return quote;
    }

    @Override
    public List<StockQuoteVO> getQuotes(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<StockQuoteVO> result = new ArrayList<>();
        List<String> missCodes = new ArrayList<>();

        // 批量查缓存
        List<String> cacheKeys = stockCodes.stream()
                .map(code -> QUOTE_CACHE_PREFIX + code)
                .toList();
        List<String> cachedValues = stringRedisTemplate.opsForValue().multiGet(cacheKeys);

        for (int i = 0; i < stockCodes.size(); i++) {
            String cached = cachedValues != null ? cachedValues.get(i) : null;
            if (cached != null) {
                StockQuoteVO vo = parseQuoteFromCache(cached);
                if (vo != null) {
                    result.add(vo);
                } else {
                    missCodes.add(stockCodes.get(i));
                }
            } else {
                missCodes.add(stockCodes.get(i));
            }
        }

        // 批量获取未命中数据（防止 rt_k 接口被限流）
        if (!missCodes.isEmpty()) {
            log.debug("批量未命中 {} 只股票，开始获取", missCodes.size());
            
            for (String code : missCodes) {
                try {
                    StockQuoteVO quote = null;
                    
                    // 优先尝试实时行情（带缓存保护）
                    try {
                        quote = fetchRealtimeQuote(code);
                    } catch (Exception e) {
                        log.debug("实时行情获取失败 [{}]: {}", code, e.getMessage());
                    }
                    
                    // 降级：使用日线数据
                    if (quote == null) {
                        try {
                            quote = fetchLatestDaily(code);
                        } catch (Exception e) {
                            log.debug("日线数据获取失败 [{}]: {}", code, e.getMessage());
                        }
                    }
                    
                    if (quote != null) {
                        writeQuoteToCache(QUOTE_CACHE_PREFIX + code, quote);
                        result.add(quote);
                    } else {
                        log.warn("获取 {} 行情失败，跳过", code);
                    }
                } catch (Exception e) {
                    log.warn("获取 {} 行情异常: {}", code, e.getMessage());
                }
            }
        }

        return result;
    }

    @Override
    public List<StockQuoteVO> getHistory(String stockCode, int days) {
        if (days <= 0) days = 30;
        if (days > 120) days = 120; // 接口单次最多120条

        // 缓存 key
        String cacheKey = HISTORY_CACHE_PREFIX + stockCode + ":" + days;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return parseHistoryFromCache(cached);
        }

        String endDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String startDate = LocalDate.now().minusDays(days).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<StockQuoteVO> history = callDailyApi(stockCode, startDate, endDate);
        if (history.isEmpty()) {
            throw new BusinessException(BizCode.QUOTE_DATA_EMPTY);
        }

        stringRedisTemplate.opsForValue().set(cacheKey, serializeHistory(history), HIS_CACHE_TTL, java.util.concurrent.TimeUnit.SECONDS);
        return history;
    }

    @Override
    public List<StockQuoteVO> searchStocks(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        if (limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        // 缓存搜索结果
        String cacheKey = SEARCH_CACHE_PREFIX + keyword.hashCode() + ":" + limit;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return parseHistoryFromCache(cached);
        }

        List<StockQuoteVO> searchResult = callStockBasicApi(keyword, limit);
        if (!searchResult.isEmpty()) {
            stringRedisTemplate.opsForValue().set(cacheKey, serializeHistory(searchResult), SEARCH_CACHE_TTL, java.util.concurrent.TimeUnit.SECONDS);
        }
        return searchResult;
    }

    // ==================== API 调用层 ====================

    /**
     * 获取实时行情（rt_k 接口）
     * 注意：该接口每分钟限1次，大批量查询时请使用 fetchLatestDaily 降级
     */
    private StockQuoteVO fetchRealtimeQuote(String stockCode) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("ts_code", stockCode);

            String json = postApi("rt_k", params);
            if (json == null) return null;

            JsonNode root = objectMapper.readTree(json);
            if (root.get("code").asInt() != 0) {
                log.warn("rt_k 接口调用失败 [{}]: {}", stockCode, root.get("msg").asText());
                return null;
            }

            JsonNode data = root.get("data");
            if (data == null || !data.has("items") || data.get("items").isEmpty()) {
                return null;
            }

            JsonNode fields = data.get("fields");
            JsonNode item = data.get("items").get(0); // 取最新一条
            return mapFieldsToQuoteVO(fields, item, stockCode);
        } catch (Exception e) {
            log.error("获取实时行情失败 {}: {}", stockCode, e.getMessage());
            return null;
        }
    }

    /**
     * 降级方案：通过日线接口获取最近一条数据
     */
    private StockQuoteVO fetchLatestDaily(String stockCode) {
        try {
            String endDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String startDate = LocalDate.now().minusDays(5).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            List<StockQuoteVO> list = callDailyApi(stockCode, startDate, endDate);
            if (list.isEmpty()) return null;
            return list.get(0); // 取最近一天
        } catch (Exception e) {
            log.warn("降级获取日线失败 {}: {}", stockCode, e.getMessage());
            return null;
        }
    }

    /**
     * 调用历史日线接口（daily）
     */
    private List<StockQuoteVO> callDailyApi(String stockCode, String startDate, String endDate) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("ts_code", stockCode);
            params.put("start_date", startDate);
            params.put("end_date", endDate);

            String json = postApi("daily", params);
            if (json == null) return Collections.emptyList();

            JsonNode root = objectMapper.readTree(json);
            if (root.get("code").asInt() != 0) {
                log.warn("daily 接口调用失败 [{}]: {}", stockCode, root.get("msg").asText());
                return Collections.emptyList();
            }

            JsonNode data = root.get("data");
            if (data == null || !data.has("items")) return Collections.emptyList();

            JsonNode fields = data.get("fields");
            JsonNode items = data.get("items");

            List<StockQuoteVO> result = new ArrayList<>();
            for (JsonNode item : items) {
                StockQuoteVO vo = mapDailyFieldsToQuoteVO(fields, item);
                if (vo != null) result.add(vo);
            }
            return result;
        } catch (Exception e) {
            log.error("解析日线数据失败 [{}]: {}", stockCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 搜索股票（stock_basic，支持名称/代码模糊匹配）
     */
    private List<StockQuoteVO> callStockBasicApi(String keyword, int limit) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            // 模糊匹配：优先通过 name 字段过滤，接口返回全量后本地过滤
            params.put("ts_code", keyword); // 精确匹配股票代码

            String json = postApi("stock_basic", params);
            if (json == null) return Collections.emptyList();

            JsonNode root = objectMapper.readTree(json);
            if (root.get("code").asInt() != 0) {
                log.warn("stock_basic 接口调用失败: {}", root.get("msg").asText());
                return Collections.emptyList();
            }

            JsonNode data = root.get("data");
            if (data == null || !data.has("items")) return Collections.emptyList();

            JsonNode fields = data.get("fields");
            JsonNode items = data.get("items");

            // 找 name 和 market 的字段索引
            int nameIdx = -1, marketIdx = -1, tsCodeIdx = -1;
            List<String> fieldList = new ArrayList<>();
            for (JsonNode f : fields) fieldList.add(f.asText());
            nameIdx = fieldList.indexOf("name");
            marketIdx = fieldList.indexOf("market");
            tsCodeIdx = fieldList.indexOf("ts_code");

            String kwLower = keyword.toLowerCase();
            List<StockQuoteVO> result = new ArrayList<>();
            for (JsonNode item : items) {
                String tsCode = tsCodeIdx >= 0 ? safeGet(item, tsCodeIdx) : "";
                String name = nameIdx >= 0 ? safeGet(item, nameIdx) : "";
                String market = marketIdx >= 0 ? safeGet(item, marketIdx) : "";

                // 模糊匹配：名称或代码包含关键词
                if (name.toLowerCase().contains(kwLower) || tsCode.toLowerCase().contains(kwLower)) {
                    StockQuoteVO vo = new StockQuoteVO();
                    vo.setTsCode(tsCode);
                    vo.setName(name);
                    // 设置默认市场标签（用于前端展示）
                    vo.setQuoteTime(LocalDateTime.now());
                    result.add(vo);
                    if (result.size() >= limit) break;
                }
            }
            return result;
        } catch (Exception e) {
            log.error("搜索股票失败 [{}]: {}", keyword, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 字段映射 ====================

    /**
     * 映射 rt_k 字段到 StockQuoteVO
     */
    private StockQuoteVO mapFieldsToQuoteVO(JsonNode fields, JsonNode item, String stockCode) {
        List<String> fieldList = new ArrayList<>();
        for (JsonNode f : fields) fieldList.add(f.asText());

        StockQuoteVO vo = new StockQuoteVO();
        vo.setTsCode(stockCode);

        for (int i = 0; i < fieldList.size() && i < item.size(); i++) {
            String field = fieldList.get(i);
            String val = safeGet(item, i);
            switch (field) {
                case "open"    -> vo.setOpen(toDecimal(val));
                case "high"   -> vo.setHigh(toDecimal(val));
                case "low"    -> vo.setLow(toDecimal(val));
                case "close"  -> vo.setClose(toDecimal(val));
                case "pct_chg" -> vo.setPctChange(toDecimal(val));
                case "vol"    -> vo.setVolume(toLong(val));
                case "amount" -> vo.setAmount(toDecimal(val));
                case "trade_date" -> vo.setQuoteTime(parseTradeDate(val));
                default -> {}
            }
        }
        return vo;
    }

    /**
     * 映射 daily 字段到 StockQuoteVO
     */
    private StockQuoteVO mapDailyFieldsToQuoteVO(JsonNode fields, JsonNode item) {
        List<String> fieldList = new ArrayList<>();
        for (JsonNode f : fields) fieldList.add(f.asText());

        StockQuoteVO vo = new StockQuoteVO();

        for (int i = 0; i < fieldList.size() && i < item.size(); i++) {
            String field = fieldList.get(i);
            String val = safeGet(item, i);
            switch (field) {
                case "ts_code"  -> vo.setTsCode(val);
                case "open"    -> vo.setOpen(toDecimal(val));
                case "high"    -> vo.setHigh(toDecimal(val));
                case "low"     -> vo.setLow(toDecimal(val));
                case "close"   -> vo.setClose(toDecimal(val));
                case "pct_chg" -> vo.setPctChange(toDecimal(val));
                case "vol"     -> vo.setVolume(toLong(val));
                case "amount"  -> vo.setAmount(toDecimal(val));
                case "trade_date" -> vo.setQuoteTime(parseTradeDate(val));
                default -> {}
            }
        }
        return vo;
    }

    // ==================== HTTP 请求 ====================

    /**
     * 通用 POST 请求
     */
    private String postApi(String apiName, Map<String, String> params) {
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"api_name\":\"").append(apiName).append("\",\"params\":{");
            int i = 0;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (i > 0) jsonBuilder.append(",");
                jsonBuilder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                i++;
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
                log.warn("HTTP请求失败 [{}]: {}", apiName, respCode);
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

    // ==================== 缓存读写 ====================

    private void writeQuoteToCache(String cacheKey, StockQuoteVO quote) {
        try {
            stringRedisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(quote), RT_CACHE_TTL, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("缓存写入失败: {}", e.getMessage());
        }
    }

    private StockQuoteVO parseQuoteFromCache(String cached) {
        try {
            return objectMapper.readValue(cached, StockQuoteVO.class);
        } catch (Exception e) {
            log.warn("行情缓存解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String serializeHistory(List<StockQuoteVO> history) {
        try {
            return objectMapper.writeValueAsString(history);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<StockQuoteVO> parseHistoryFromCache(String cached) {
        try {
            return objectMapper.readValue(cached,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StockQuoteVO.class));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ==================== 工具方法 ====================

    private BigDecimal toDecimal(String val) {
        if (val == null || val.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Long toLong(String val) {
        if (val == null || val.isBlank()) return 0L;
        try {
            return Long.parseLong(val.split("\\.")[0]);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String safeGet(JsonNode node, int index) {
        if (node == null || index < 0 || index >= node.size()) return "";
        JsonNode n = node.get(index);
        return n == null || n.isNull() ? "" : n.asText();
    }

    private LocalDateTime parseTradeDate(String val) {
        if (val == null || val.length() < 8) return LocalDateTime.now();
        try {
            return LocalDate.parse(val.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"))
                    .atStartOfDay();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
