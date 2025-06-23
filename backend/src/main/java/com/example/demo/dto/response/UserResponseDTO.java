package com.example.demo.dto.response;

import java.util.Set;

import com.example.demo.dto.short_db.WorkshopShortDTO;
import com.example.demo.models.Role;

public class UserResponseDTO {
    private Long id;
    private String username;
    private Role role;
    private Boolean isBanned;
    private Set<WorkshopShortDTO> workshops;

    public UserResponseDTO(Long id, String username, Role role, Boolean isBanned, Set<WorkshopShortDTO> workshops) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.isBanned = isBanned;
        this.workshops = workshops;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getIsBanned() {
        return isBanned;
    }

    public void setIsBanned(Boolean isBanned) {
        this.isBanned = isBanned;
    }

    public Set<WorkshopShortDTO> getWorkshops() {
        return workshops;
    }

    public void setWorkshops(Set<WorkshopShortDTO> foremans) {
        this.workshops = foremans;
    }

}
