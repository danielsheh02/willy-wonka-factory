package com.example.demo.services;

import com.example.demo.dto.UserRequestDTO;
import com.example.demo.exceptions.UsernameAlreadyExistsException;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.models.Workshop;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WorkshopRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepository, WorkshopRepository workshopRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.workshopRepository = workshopRepository;
        this.encoder = encoder;
    }

    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> createUser(UserRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new UsernameAlreadyExistsException("Username exist");
        }

        Set<Workshop> workshops = new HashSet<>();
        if (dto.getWorkshopsIds() != null) {
            for (Long idWorkshop : dto.getWorkshopsIds()) {
                Optional<Workshop> workshopOpt = workshopRepository.findById(idWorkshop);
                if (workshopOpt.isEmpty()) {
                    return Optional.empty();
                }
                workshops.add(workshopOpt.get());
            }
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        } else {
            user.setRole(Role.UNKNOWN);
        }
        user.setWorkshops(workshops);
        user.setPassword(encoder.encode(dto.getPassword()));

        // We need this because User do not own Set<Workshop> equipments
        for (Workshop workshop : workshops) {
            Set<User> users = workshop.getForemans();
            users.add(user);
            workshop.setForemans(users);
        }

        return Optional.of(userRepository.save(user));
    }

    public Optional<User> updateUser(Long id, UserRequestDTO dto) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();

        String newUsername = dto.getUsername();
        if (!user.getUsername().equals(newUsername)) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new UsernameAlreadyExistsException("Username exist");
            }
            user.setUsername(newUsername);
        }

        Set<Workshop> workshops = new HashSet<>();
        if (dto.getWorkshopsIds() != null) {
            for (Long idWorkshop : dto.getWorkshopsIds()) {
                Optional<Workshop> workshopOpt = workshopRepository.findById(idWorkshop);
                if (workshopOpt.isEmpty()) {
                    return Optional.empty();
                }
                workshops.add(workshopOpt.get());
            }
        }

        // We need this because User do not own Set<Workshop> equipments
        for (Workshop oldWorkshop : user.getWorkshops()) {
            oldWorkshop.getForemans().remove(user);
        }

        for (Workshop workshop : workshops) {
            workshop.getForemans().add(user);
        }

        user.setWorkshops(workshops);
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        } else {
            user.setRole(Role.UNKNOWN);
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(encoder.encode(dto.getPassword()));
        }

        return Optional.of(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}