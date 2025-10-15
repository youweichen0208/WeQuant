package com.quant.stock.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * LocalDate自定义序列化器和反序列化器
 * 支持yyyyMMdd格式
 *
 * @author Quant Trading Platform
 */
public class LocalDateFormatter {

    private static final DateTimeFormatter COMPACT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter STANDARD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 紧凑格式反序列化器（用于接收market-data-service的数据）
     */
    public static class CompactDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String date = p.getText();
            if (date == null || date.trim().isEmpty()) {
                return null;
            }
            // 尝试紧凑格式
            if (date.length() == 8 && date.matches("\\d{8}")) {
                return LocalDate.parse(date, COMPACT_FORMATTER);
            }
            // 尝试标准格式
            return LocalDate.parse(date, STANDARD_FORMATTER);
        }
    }

    /**
     * 标准格式序列化器（用于输出API响应）
     */
    public static class StandardSerializer extends JsonSerializer<LocalDate> {
        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeString(value.format(STANDARD_FORMATTER));
            }
        }
    }

    /**
     * 紧凑格式序列化器
     */
    public static class CompactSerializer extends JsonSerializer<LocalDate> {
        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeString(value.format(COMPACT_FORMATTER));
            }
        }
    }
}