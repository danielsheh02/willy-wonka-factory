package com.example.demo.dto.request;

import java.util.Set;

public class WorkshopRequestDTO {

    private String name;
    private String description;
    private Set<Long> foremanIds;
    private Set<Long> equipmentIds;

    public Set<Long> getForemanIds() {
        return foremanIds;
    }

    public void setForemanIds(Set<Long> foremanIds) {
        this.foremanIds = foremanIds;
    }

    public Set<Long> getEquipmentIds() {
        return equipmentIds;
    }

    public void setEquipmentIds(Set<Long> equipmentIds) {
        this.equipmentIds = equipmentIds;
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
}
