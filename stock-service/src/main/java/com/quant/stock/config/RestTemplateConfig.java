package com.quant.stock.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 配置类
 * 用于配置HTTP客户端，调用外部Market Data Service
 *
 * @author Quant Trading Platform
 */
@Configuration
@EnableRetry
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Value("${market-data.service.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${market-data.service.read-timeout:30000}")
    private int readTimeout;

    private final ObjectMapper objectMapper;

    public RestTemplateConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 配置RestTemplate Bean
     * 用于调用Market Data Service的API
     */
    @Bean("marketDataRestTemplate")
    public RestTemplate marketDataRestTemplate(RestTemplateBuilder builder) {
        log.info("配置Market Data RestTemplate - connectionTimeout: {}ms, readTimeout: {}ms",
                connectionTimeout, readTimeout);

        // 使用自定义的ObjectMapper创建消息转换器
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        return builder
                .setConnectTimeout(Duration.ofMillis(connectionTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .requestFactory(this::clientHttpRequestFactory)
                .messageConverters(converter)
                .errorHandler(new MarketDataResponseErrorHandler())
                .build();
    }

    /**
     * 配置通用RestTemplate Bean
     * 用于其他HTTP调用
     */
    @Bean("commonRestTemplate")
    public RestTemplate commonRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(15000))
                .requestFactory(this::clientHttpRequestFactory)
                .build();
    }

    /**
     * 配置HTTP请求工厂
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);

        // 设置缓冲请求体
        factory.setBufferRequestBody(true);

        log.debug("配置ClientHttpRequestFactory完成");
        return factory;
    }
}