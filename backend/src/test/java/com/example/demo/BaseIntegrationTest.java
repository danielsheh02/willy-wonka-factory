package com.example.demo;

import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.security.jwt.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected JwtUtils jwtUtils;

    @Autowired
    protected BCryptPasswordEncoder passwordEncoder;

    protected Map<Role, String> roleTokens = new HashMap<>();
    protected Map<Role, User> roleUsers = new HashMap<>();

    @BeforeEach
    public void setupBaseTest() {
        
        if (userRepository.count() == 0) {
            createTestUsers();
            generateTokensForRoles();
        } else {
            roleUsers.clear();
            roleTokens.clear();
            
            Role[] roles = {Role.ADMIN, Role.FOREMAN, Role.WORKER, Role.MASTER, Role.GUIDE};
            for (Role role : roles) {
                User user = userRepository.findAll().stream()
                        .filter(u -> u.getRole() == role)
                        .findFirst()
                        .orElse(null);
                
                if (user != null) {
                    roleUsers.put(role, user);
                    
                    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        Collections.singletonList(new SimpleGrantedAuthority(role.name()))
                    );
                    
                    String token = jwtUtils.generateJwtToken(userDetails);
                    roleTokens.put(role, token);
                }
            }

            if (roleUsers.size() < 5) {
                createTestUsers();
                generateTokensForRoles();
            }
        }
    }

    private void createTestUsers() {
        Role[] roles = {Role.ADMIN, Role.FOREMAN, Role.WORKER, Role.MASTER, Role.GUIDE};
        
        for (Role role : roles) {
            User user = new User();
            user.setUsername(role.name().toLowerCase() + "_user");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setRole(role);
            user.setIsBanned(false);
            
            User savedUser = userRepository.save(user);
            roleUsers.put(role, savedUser);
        }
    }

    private void generateTokensForRoles() {
        for (Map.Entry<Role, User> entry : roleUsers.entrySet()) {
            Role role = entry.getKey();
            User user = entry.getValue();
            
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(role.name()))
            );
            
            String token = jwtUtils.generateJwtToken(userDetails);
            roleTokens.put(role, token);
        }
    }

    protected String getTokenForRole(Role role) {
        return roleTokens.get(role);
    }

    protected User getUserForRole(Role role) {
        return roleUsers.get(role);
    }

    protected String asJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}

