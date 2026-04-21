package com.finance.api.service;

import com.finance.api.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RabbitMQ 消息监听器（消费者）
 * 处理价格提醒、缓存预热、通知等异步任务
 * 
 * 【面试亮点】
 * - @RabbitListener 注解声明式监听队列，无需手动管理 Connection/Channel
 * - 价格提醒触发后：先更新数据库状态，再发送 MQ 消息，实现最终一致性
 * - 消费者与业务解耦：定时任务只负责"判断是否触发"，通知逻辑由消费者异步处理
 * - 预留邮件通知队列，可接入真实邮件服务（SendCloud/阿里云邮件等）
 * - 预留 WebSocket 扩展点，可向在线用户实时推送通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceAlertConsumer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 监听价格提醒队列
     */
    @RabbitListener(queues = RabbitMQConfig.PRICE_ALERT_QUEUE)
    public void handlePriceAlert(Map<String, Object> message) {
        try {
            Long userId = ((Number) message.get("userId")).longValue();
            String stockCode = (String) message.get("stockCode");
            String stockName = (String) message.get("stockName");
            String targetPrice = message.get("targetPrice").toString();
            String alertType = (String) message.get("alertType");
            String currentPrice = message.get("currentPrice").toString();

            log.info("【价格提醒触发】用户:{} 股票:{}({}) 当前价:{} 触发条件:{} {}",
                    userId, stockName, stockCode, currentPrice, alertType, targetPrice);

            // TODO: 发送通知（邮件/短信/站内信）
            sendNotification(userId, stockName, stockCode, targetPrice, alertType, currentPrice);

        } catch (Exception e) {
            log.error("处理价格提醒失败: {}", message, e);
        }
    }

    /**
     * 监听缓存预热队列
     */
    @RabbitListener(queues = RabbitMQConfig.QUOTE_CACHE_WARMUP_QUEUE)
    public void handleCacheWarmup(Map<String, Object> message) {
        try {
            String stockCode = (String) message.get("stockCode");
            log.debug("【缓存预热】开始预热: {}", stockCode);

            // TODO: 调用行情服务预热缓存
            // stockQuoteService.getQuote(stockCode);

        } catch (Exception e) {
            log.error("缓存预热失败: {}", message, e);
        }
    }

    /**
     * 监听邮件通知队列
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_NOTIFY_QUEUE)
    public void handleEmailNotify(Map<String, Object> message) {
        try {
            String to = (String) message.get("to");
            String subject = (String) message.get("subject");
            String content = (String) message.get("content");

            log.info("【邮件通知】发送邮件到: {} 主题: {}", to, subject);
            // TODO: 调用邮件服务发送邮件

        } catch (Exception e) {
            log.error("邮件发送失败: {}", message, e);
        }
    }

    /**
     * 发送通知（实际实现时替换为邮件/推送服务）
     */
    private void sendNotification(Long userId, String stockName, String stockCode,
                                  String targetPrice, String alertType, String currentPrice) {
        String conditionDesc = switch (alertType) {
            case "gt" -> String.format("涨破 ¥%s", targetPrice);
            case "lt" -> String.format("跌破 ¥%s", targetPrice);
            case "eq" -> String.format("触及 ¥%s", targetPrice);
            default -> "价格异动";
        };

        // TODO: 可以通过 WebSocket / SSE 推送给在线用户
        // 也可以发邮件/短信
        log.info("【通知】给用户 {} 发送提醒: {} {} 提醒条件:{} 当前价格:{}",
                userId, stockName, stockCode, conditionDesc, currentPrice);
    }
}
