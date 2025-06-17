package com.example.demo.dto;
import com.example.demo.models.Role;

public class UserRequestDTO {
    private String username;
    private Role role;

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}