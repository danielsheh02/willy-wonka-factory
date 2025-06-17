package com.example.demo.services;

import com.example.demo.dto.UserRequestDTO;
import com.example.demo.exceptions.UsernameAlreadyExistsException;
import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(UserRequestDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setRole(dto.getRole());

        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, UserRequestDTO dto) {
        return userRepository.findById(id).map(existingUser -> {
            String newUsername = dto.getUsername();
            if (!existingUser.getUsername().equals(newUsername)) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new UsernameAlreadyExistsException("Username exist");
                }
                existingUser.setUsername(newUsername);
            }
    
            existingUser.setRole(dto.getRole());
    
            return userRepository.save(existingUser);
        });
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Boolean existsByUsername(String username){
        return userRepository.existsByUsername(username);
    }
}