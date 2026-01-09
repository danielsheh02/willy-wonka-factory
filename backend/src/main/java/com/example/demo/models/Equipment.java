package com.example.demo.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus status;

    @Column(nullable = false)
    @Min(0)
    @Max(100)
    private int health;

    private Double temperature;
    private LocalDate lastServicedAt;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "workshop_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Workshop workshop;

    public Equipment() {
    }

    public Equipment(String name, String description, String model, EquipmentStatus status, int health,
            Double temperature, LocalDate lastServicedAt) {
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

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

}
