package com.finance.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.api.entity.PriceAlert;
import com.finance.api.mapper.PriceAlertMapper;
import com.finance.api.service.impl.StockQuoteServiceImpl;
import com.finance.api.vo.StockQuoteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 价格监控定时任务
 * 每5分钟扫描所有启用中的价格提醒，判断是否触发
 *
 * 支持市场：
 * - A股（.SH/.SZ）：rt_k 实时行情 + daily 日线降级
 * - 港股（.HK）：hk_daily 日线
 * - 美股（.US）：us_daily 日线
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceMonitorTask {

    private final PriceAlertMapper priceAlertMapper;
    private final RabbitTemplate rabbitTemplate;
    private final StockQuoteServiceImpl stockQuoteService;

    /** 每5分钟执行一次 */
    @Scheduled(fixedRate = 300_000)
    public void monitorPriceAlerts() {
        log.debug("开始执行价格监控...");

        // 查询所有启用且未触发的提醒
        List<PriceAlert> alerts = priceAlertMapper.selectList(
                new LambdaQueryWrapper<PriceAlert>()
                        .eq(PriceAlert::getIsEnabled, 1)
                        .eq(PriceAlert::getIsTriggered, 0)
        );

        if (alerts.isEmpty()) {
            return;
        }

        log.info("待监控提醒数量: {}", alerts.size());

        // 按市场分组统计
        Map<String, List<PriceAlert>> byMarket = alerts.stream()
                .collect(Collectors.groupingBy(this::getMarket));

        for (Map.Entry<String, List<PriceAlert>> entry : byMarket.entrySet()) {
            String market = entry.getKey();
            List<PriceAlert> marketAlerts = entry.getValue();

            // 批量获取该市场的行情（利用缓存，减少 API 调用）
            List<String> codes = marketAlerts.stream()
                    .map(PriceAlert::getStockCode)
                    .distinct()
                    .toList();

            try {
                Map<String, StockQuoteVO> quoteMap = fetchQuotesBatch(codes);
                
                // 逐个检查是否触发
                for (PriceAlert alert : marketAlerts) {
                    try {
                        checkAndTrigger(alert, quoteMap);
                    } catch (Exception e) {
                        log.error("检查提醒 {} 时出错: {}", alert.getStockCode(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("批量获取行情失败 [{}]: {}", market, e.getMessage());
            }
        }
    }

    /**
     * 批量获取行情（利用缓存，减少 API 调用）
     */
    private Map<String, StockQuoteVO> fetchQuotesBatch(List<String> codes) {
        Map<String, StockQuoteVO> result = new HashMap<>();
        
        // 使用批量查询，内部会利用缓存
        List<StockQuoteVO> quotes = stockQuoteService.getQuotes(codes);
        
        for (StockQuoteVO quote : quotes) {
            if (quote != null && quote.getTsCode() != null) {
                result.put(quote.getTsCode(), quote);
            }
        }
        
        return result;
    }

    /**
     * 检查并触发提醒
     */
    private void checkAndTrigger(PriceAlert alert, Map<String, StockQuoteVO> quoteMap) {
        StockQuoteVO quote = quoteMap.get(alert.getStockCode());
        
        if (quote == null || quote.getClose() == null) {
            log.debug("获取 {} 行情数据失败，跳过", alert.getStockCode());
            return;
        }

        BigDecimal currentPrice = quote.getClose();
        BigDecimal targetPrice = alert.getTargetPrice();
        boolean triggered = false;

        switch (alert.getAlertType()) {
            case "gt" -> triggered = currentPrice.compareTo(targetPrice) > 0;
            case "lt" -> triggered = currentPrice.compareTo(targetPrice) < 0;
            case "eq" -> triggered = currentPrice.compareTo(targetPrice) == 0;
        }

        if (triggered) {
            log.info("【触发】股票 {} 当前价格 {} 触发了 {} {} 的提醒",
                    alert.getStockName(), currentPrice,
                    alert.getAlertType(), targetPrice);

            // 更新为已触发
            alert.setIsTriggered(1);
            alert.setTriggeredAt(java.time.LocalDateTime.now());
            priceAlertMapper.updateById(alert);

            // 发送消息通知
            Map<String, Object> msg = new HashMap<>();
            msg.put("userId", alert.getUserId());
            msg.put("stockCode", alert.getStockCode());
            msg.put("stockName", alert.getStockName());
            msg.put("targetPrice", alert.getTargetPrice().toString());
            msg.put("alertType", alert.getAlertType());
            msg.put("currentPrice", currentPrice.toString());
            msg.put("triggeredAt", alert.getTriggeredAt().toString());

            rabbitTemplate.convertAndSend(
                    com.finance.api.config.RabbitMQConfig.TOPIC_EXCHANGE,
                    com.finance.api.config.RabbitMQConfig.PRICE_ALERT_KEY,
                    msg
            );
        }
    }

    /**
     * 根据股票代码判断市场
     */
    private String getMarket(PriceAlert alert) {
        String code = alert.getStockCode();
        if (code == null) return "UNKNOWN";
        
        if (code.endsWith(".US")) return "US";
        if (code.endsWith(".HK")) return "HK";
        if (code.endsWith(".SH")) return "SH";
        if (code.endsWith(".SZ")) return "SZ";
        
        return "UNKNOWN";
    }
}
