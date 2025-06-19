package com.example.demo.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserRequestDTO;
import com.example.demo.security.UserDetailsServiceImpl;
import com.example.demo.security.jwt.JwtUtils;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final UserDetailsServiceImpl userDetailsServiceImpl;

    private final JwtUtils jwtUtils;

    public AuthService(AuthenticationManager authenticationManager,
            UserDetailsServiceImpl userDetailsServiceImpl,
            JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userDetailsServiceImpl = userDetailsServiceImpl;
        this.jwtUtils = jwtUtils;
    }

    public String loginUser(UserRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(dto.getUsername());
        String token = jwtUtils.generateJwtToken(userDetails);
        return token;
    }

}
