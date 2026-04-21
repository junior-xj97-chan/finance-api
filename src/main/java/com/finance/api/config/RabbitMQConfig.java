package com.finance.api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * 
 * 【面试亮点】
 * - 采用 TopicExchange 主题交换机，支持通配符路由（如 finance.price.* 匹配 finance.price.alert）
 * - 队列配置了死信队列（DLQ），消息消费失败后自动转入 DLQ，便于排查和重试
 * - 使用 Jackson2JsonMessageConverter，消息体为 JSON 格式，跨语言友好
 * - 预留了邮件通知队列，可扩展接入真实邮件服务
 */
@Configuration
public class RabbitMQConfig {

    // ========== 交换机 ==========
    public static final String TOPIC_EXCHANGE = "finance.topic.exchange";

    // ========== 队列 ==========
    public static final String PRICE_ALERT_QUEUE = "finance.price.alert.queue";
    public static final String QUOTE_CACHE_WARMUP_QUEUE = "finance.quote.cache.warmup.queue";
    public static final String EMAIL_NOTIFY_QUEUE = "finance.email.notify.queue";

    // ========== Routing Key ==========
    public static final String PRICE_ALERT_KEY = "finance.price.alert";
    public static final String QUOTE_CACHE_WARMUP_KEY = "finance.quote.cache.warmup";
    public static final String EMAIL_NOTIFY_KEY = "finance.email.notify";

    // ========== 交换机 ==========
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    // ========== 队列 ==========
    @Bean
    public Queue priceAlertQueue() {
        return QueueBuilder.durable(PRICE_ALERT_QUEUE)
                .withArgument("x-dead-letter-exchange", TOPIC_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", PRICE_ALERT_KEY + ".dlq")
                .build();
    }

    @Bean
    public Queue quoteCacheWarmupQueue() {
        return QueueBuilder.durable(QUOTE_CACHE_WARMUP_QUEUE).build();
    }

    @Bean
    public Queue emailNotifyQueue() {
        return QueueBuilder.durable(EMAIL_NOTIFY_QUEUE).build();
    }

    // ========== 绑定 ==========
    @Bean
    public Binding priceAlertBinding() {
        return BindingBuilder.bind(priceAlertQueue()).to(topicExchange()).with(PRICE_ALERT_KEY);
    }

    @Bean
    public Binding cacheWarmupBinding() {
        return BindingBuilder.bind(quoteCacheWarmupQueue()).to(topicExchange()).with(QUOTE_CACHE_WARMUP_KEY);
    }

    @Bean
    public Binding emailNotifyBinding() {
        return BindingBuilder.bind(emailNotifyQueue()).to(topicExchange()).with(EMAIL_NOTIFY_KEY);
    }

    // ========== 消息转换器 ==========
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(factory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
