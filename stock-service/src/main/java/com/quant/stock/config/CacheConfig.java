package com.quant.stock.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置类
 * 配置Redis缓存策略和不同数据类型的缓存时间
 *
 * @author Quant Trading Platform
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    private final ObjectMapper objectMapper;

    @Value("${cache.stock.history-ttl:300}")
    private int historyTtl;

    @Value("${cache.stock.realtime-ttl:30}")
    private int realtimeTtl;

    @Value("${cache.stock.info-ttl:86400}")
    private int infoTtl;

    @Value("${cache.enabled:false}")
    private boolean cacheEnabled;

    public CacheConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 配置Redis缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 暂时禁用缓存，避免序列化问题
        if (!cacheEnabled) {
            log.info("缓存已禁用，使用NoOpCacheManager");
            return new NoOpCacheManager();
        }

        log.info("配置Redis缓存管理器 - historyTtl: {}s, realtimeTtl: {}s, infoTtl: {}s",
                historyTtl, realtimeTtl, infoTtl);

        // 使用自定义的ObjectMapper创建序列化器
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .entryTtl(Duration.ofSeconds(300)); // 默认5分钟

        // 不同缓存区域的配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 股票历史数据缓存 - 5分钟
        cacheConfigurations.put("stockHistory",
                defaultConfig.entryTtl(Duration.ofSeconds(historyTtl)));

        // 股票实时数据缓存 - 30秒
        cacheConfigurations.put("stockLatest",
                defaultConfig.entryTtl(Duration.ofSeconds(realtimeTtl)));

        // 股票基础信息缓存 - 1天
        cacheConfigurations.put("stockInfo",
                defaultConfig.entryTtl(Duration.ofSeconds(infoTtl)));

        // 批量查询缓存 - 1分钟
        cacheConfigurations.put("batchQuery",
                defaultConfig.entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * 配置RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用自定义的ObjectMapper创建序列化器
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 设置序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        log.info("配置RedisTemplate完成");
        return template;
    }
}