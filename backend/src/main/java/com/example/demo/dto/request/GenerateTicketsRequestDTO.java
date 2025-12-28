package com.example.demo.dto.request;

public class GenerateTicketsRequestDTO {
    private Integer count; // Количество билетов для генерации
    private Integer expiresInDays; // Через сколько дней истекут (опционально)

    public GenerateTicketsRequestDTO() {
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getExpiresInDays() {
        return expiresInDays;
    }

    public void setExpiresInDays(Integer expiresInDays) {
        this.expiresInDays = expiresInDays;
    }
}

