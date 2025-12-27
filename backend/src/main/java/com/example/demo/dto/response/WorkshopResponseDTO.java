package com.example.demo.dto.response;

import java.util.Set;

import com.example.demo.dto.short_db.EquipmentShortDTO;
import com.example.demo.dto.short_db.UserShortDTO;

public class WorkshopResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Integer capacity;
    private Integer visitDurationMinutes;
    private Set<UserShortDTO> foremans;
    private Set<EquipmentShortDTO> equipments;

    public WorkshopResponseDTO(Long id, String name, String description, Integer capacity, 
            Integer visitDurationMinutes, Set<UserShortDTO> foremans,
            Set<EquipmentShortDTO> equipments) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.visitDurationMinutes = visitDurationMinutes;
        this.foremans = foremans;
        this.equipments = equipments;
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

    public Set<UserShortDTO> getForemans() {
        return foremans;
    }

    public void setForemans(Set<UserShortDTO> foremans) {
        this.foremans = foremans;
    }

    public Set<EquipmentShortDTO> getEquipments() {
        return equipments;
    }

    public void setEquipments(Set<EquipmentShortDTO> equipments) {
        this.equipments = equipments;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getVisitDurationMinutes() {
        return visitDurationMinutes;
    }

    public void setVisitDurationMinutes(Integer visitDurationMinutes) {
        this.visitDurationMinutes = visitDurationMinutes;
    }

}
