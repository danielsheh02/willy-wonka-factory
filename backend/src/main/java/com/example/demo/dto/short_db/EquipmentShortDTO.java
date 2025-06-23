package com.example.demo.dto.short_db;

import java.time.LocalDate;
import com.example.demo.models.EquipmentStatus;

public class EquipmentShortDTO {
    private Long id;
    private String name;
    private String description;
    private String model;
    private EquipmentStatus status;
    private int health;
    private Double temperature;
    private LocalDate lastServicedAt;

    public EquipmentShortDTO() {
    }

    public EquipmentShortDTO(Long id, String name, String description, String model, EquipmentStatus status,
            int health, Double temperature, LocalDate lastServicedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.model = model;
        this.status = status;
        this.health = health;
        this.temperature = temperature;
        this.lastServicedAt = lastServicedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public LocalDate getLastServicedAt() {
        return lastServicedAt;
    }

    public void setLastServicedAt(LocalDate lastServicedAt) {
        this.lastServicedAt = lastServicedAt;
    }
}
