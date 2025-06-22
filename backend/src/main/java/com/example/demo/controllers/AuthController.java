package com.example.demo.controllers;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthUserDTO;
import com.example.demo.dto.UserRequestDTO;
import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.exceptions.AuthExc;
import com.example.demo.services.AuthService;
import com.example.demo.services.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final PasswordEncoder encoder;

    private final AuthService authService;

    private final UserService userService;

    public AuthController(UserRepository userRepository, AuthenticationManager authenticationManager,
            PasswordEncoder encoder, AuthService authService, UserService userService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.encoder = encoder;
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody AuthUserDTO dto) {
        UserRequestDTO userDto = new UserRequestDTO();
        userDto.setUsername(dto.getUsername());
        userDto.setPassword(dto.getPassword());
        Optional<User> userOpt = userService.createUser(userDto);

        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userOpt.get());
        } else {
            return ResponseEntity.badRequest().body("");
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthUserDTO dto) {
        try {
            String token = authService.loginUser(dto);
            return ResponseEntity.ok(token);
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(new AuthExc("Invalid username or password"), HttpStatus.UNAUTHORIZED);
        }
    }
}
