package com.example.demo.models;

import jakarta.persistence.*;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "workshops")
public class Workshop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<WorkshopToUser> foremanLinks;

    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL)
    private Set<Equipment> equipments;

    public Workshop() {
    }

    public Workshop(String name, String description, Set<User> foremans, Set<Equipment> equipments) {
        this.name = name;
        this.description = description;
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

    public Set<Equipment> getEquipments() {
        return equipments;
    }

    public void setEquipments(Set<Equipment> equipments) {
        this.equipments = equipments;
    }

    public Set<WorkshopToUser> getForemanLinks() {
        return foremanLinks;
    }

    public void setForemanLinks(Set<WorkshopToUser> foremanLinks) {
        this.foremanLinks = foremanLinks;
    }
}
