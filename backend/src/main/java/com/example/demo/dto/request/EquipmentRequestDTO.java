package com.example.demo.dto.request;

import com.example.demo.models.EquipmentStatus;
import java.time.LocalDate;

public class EquipmentRequestDTO {
    private String name;
    private String description;
    private String model;
    private EquipmentStatus status;
    private int health;
    private Double temperature;
    private LocalDate lastServicedAt;
    private Long workshopId;

    public EquipmentRequestDTO() {
    }

    public EquipmentRequestDTO(String name, String description, String model, EquipmentStatus status, int health,
            Double temperature, LocalDate lastServicedAt, Long workshopId) {
        this.name = name;
        this.description = description;
        this.model = model;
        this.status = status;
        this.health = health;
        this.temperature = temperature;
        this.lastServicedAt = lastServicedAt;
        this.workshopId = workshopId;
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

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshoId) {
        this.workshopId = workshoId;
    }

}
