package com.example.demo.models;

import jakarta.persistence.*;
import java.util.Set;

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

    @ManyToMany
    @JoinTable(name = "workshop_foremans", joinColumns = @JoinColumn(name = "workshop_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> foremans;

    @OneToMany(mappedBy = "workshop")
    private Set<Equipment> equipment;

    public Workshop() {
    }

    public Workshop(String name, String description, Set<User> foremans, Set<Equipment> equipment) {
        this.name = name;
        this.description = description;
        this.foremans = foremans;
        this.equipment = equipment;
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

    public Set<User> getForemans() {
        return foremans;
    }

    public void setForemans(Set<User> foremans) {
        this.foremans = foremans;
    }

    public Set<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(Set<Equipment> equipment) {
        this.equipment = equipment;
    }
}
