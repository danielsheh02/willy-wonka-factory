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

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "workshop_foremans", joinColumns = @JoinColumn(name = "workshop_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> foremans;

    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL)
    private Set<Equipment> equipments;

    public Workshop() {
    }

    public Workshop(String name, String description, Set<User> foremans, Set<Equipment> equipments) {
        this.name = name;
        this.description = description;
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

    public Set<User> getForemans() {
        return foremans;
    }

    public void setForemans(Set<User> foremans) {
        this.foremans = foremans;
    }

    public Set<Equipment> getEquipments() {
        return equipments;
    }

    public void setEquipments(Set<Equipment> equipments) {
        this.equipments = equipments;
    }
}
