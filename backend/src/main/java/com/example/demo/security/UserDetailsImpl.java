package com.example.demo.security;

import java.util.Collections;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.models.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class UserDetailsImpl implements UserDetails {
    private Long id;
    private String username;

    @JsonIgnore
    private String password;
    private Boolean isBanned;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String username, String password, Boolean isBanned,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.isBanned = isBanned;
    }

    public static UserDetailsImpl build(User user) {

        List<GrantedAuthority> authorities = Collections
                .singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getIsBanned(),
                authorities);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public long getId() {
        return this.id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Boolean getIsBanned() {
        return isBanned;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

}
