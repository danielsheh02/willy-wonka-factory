package com.example.demo.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Конфигурация Jackson для работы с датами в UTC.
 * 
 * Все даты сериализуются в ISO-8601 формате: "2025-01-04T14:00:00"
 * На фронтенде эти даты интерпретируются как UTC и конвертируются в локальное время пользователя.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Устанавливаем UTC как временную зону по умолчанию
            builder.timeZone(TimeZone.getTimeZone("UTC"));
            
            // Отключаем сериализацию дат как timestamps (используем строки ISO-8601)
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            
            System.out.println("[JacksonConfig] Настроена сериализация дат в UTC (ISO-8601)");
        };
    }
}

