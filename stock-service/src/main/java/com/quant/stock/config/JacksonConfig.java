package com.quant.stock.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;

/**
 * Jackson配置类
 * 配置JSON序列化和反序列化
 *
 * @author Quant Trading Platform
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册JavaTimeModule以支持Java 8日期时间类型
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        mapper.registerModule(javaTimeModule);

        // 注册自定义的LocalDate序列化器和反序列化器
        SimpleModule customModule = new SimpleModule();
        customModule.addSerializer(LocalDate.class, new LocalDateFormatter.StandardSerializer());
        customModule.addDeserializer(LocalDate.class, new LocalDateFormatter.CompactDeserializer());
        mapper.registerModule(customModule);

        // 禁用将日期写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}