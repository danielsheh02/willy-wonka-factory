package com.example.demo.controllers;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.request.AuthUserRequestDTO;
import com.example.demo.dto.response.JwtResponse;
import com.example.demo.dto.request.UserRequestDTO;
import com.example.demo.dto.response.UserResponseDTO;
import com.example.demo.exceptions.AuthExc;
import com.example.demo.services.AuthService;
import com.example.demo.services.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody AuthUserRequestDTO dto) {
        UserRequestDTO userDto = new UserRequestDTO();
        userDto.setUsername(dto.getUsername());
        userDto.setPassword(dto.getPassword());
        Optional<UserResponseDTO> userOpt = userService.createUser(userDto);

        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userOpt.get());
        } else {
            return ResponseEntity.badRequest().body("");
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthUserRequestDTO dto) {
        try {
            JwtResponse jwtResponse = authService.loginUser(dto);
            return ResponseEntity.ok(jwtResponse);
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(new AuthExc("Invalid username or password"), HttpStatus.UNAUTHORIZED);
        }
    }
}
