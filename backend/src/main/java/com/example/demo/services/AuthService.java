package com.example.demo.services;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AuthUserDTO;
import com.example.demo.dto.JwtResponse;
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

    public JwtResponse loginUser(AuthUserDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(dto.getUsername());
        String token = jwtUtils.generateJwtToken(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replaceFirst("^ROLE_", ""))
                .toList();

        String firstRole = roles.isEmpty() ? null : roles.get(0);

        return new JwtResponse(token, userDetails.getUsername(), firstRole);
    }

}
