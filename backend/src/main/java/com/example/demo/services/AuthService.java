package com.example.demo.services;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.example.demo.dto.request.AuthUserRequestDTO;
import com.example.demo.dto.response.JwtResponse;
import com.example.demo.security.UserDetailsImpl;
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

    public JwtResponse loginUser(AuthUserRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        UserDetailsImpl userDetails = userDetailsServiceImpl.loadUserByUsername(dto.getUsername());
        String token = jwtUtils.generateJwtToken(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList();

        String firstRole = roles.isEmpty() ? null : roles.get(0);

        return new JwtResponse(token, userDetails.getId(), userDetails.getUsername(), firstRole);
    }

}
