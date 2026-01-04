package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * Конфигурация JPA для работы с датами в UTC
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.example.demo.repositories")
public class JpaConfig {
    
    /**
     * Устанавливаем UTC как временную зону по умолчанию для всего приложения
     * Это гарантирует, что все даты в БД будут в UTC
     */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.out.println("[JpaConfig] Установлена временная зона UTC для JPA");
    }
}

