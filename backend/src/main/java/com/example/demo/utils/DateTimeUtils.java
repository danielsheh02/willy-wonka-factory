package com.example.demo.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Утилита для работы с датами и временем в UTC.
 * 
 * Важно: Весь backend работает ТОЛЬКО в UTC.
 * Конвертация в локальное время пользователя происходит на фронтенде (JavaScript).
 */
public class DateTimeUtils {
    
    /**
     * Получить текущее время в UTC.
     * Используется для всех операций с датами на backend.
     */
    public static LocalDateTime nowUTC() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}

