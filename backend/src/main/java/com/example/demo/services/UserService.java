package com.example.demo.services;

import com.example.demo.dto.request.UserRequestDTO;
import com.example.demo.dto.response.UserResponseDTO;
import com.example.demo.dto.short_db.WorkshopShortDTO;
import com.example.demo.exceptions.UsernameAlreadyExistsException;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.models.Workshop;
import com.example.demo.models.WorkshopToUser;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WorkshopRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    private UserResponseDTO toUserDTO(User user) {
        Set<WorkshopShortDTO> workshops = user.getWorkshopLinks()
                .stream()
                .map(WorkshopToUser::getWorkshop)
                .map(workshop -> new WorkshopShortDTO(
                        workshop.getId(),
                        workshop.getName(),
                        workshop.getDescription()))
                .collect(Collectors.toSet());

        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getIsBanned(),
                workshops,
                user.getCreatedAt());
    }

    public Iterable<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserDTO)
                .toList();
    }

    public Optional<UserResponseDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toUserDTO);
    }

    public Optional<UserResponseDTO> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toUserDTO);
    }

    public Page<UserResponseDTO> getAllUsersPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable)
                .map(this::toUserDTO);
    }

    public Optional<UserResponseDTO> createUser(UserRequestDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().isEmpty()) {
            return Optional.empty();
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new UsernameAlreadyExistsException("Username exist");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        } else {
            user.setRole(Role.UNKNOWN);
        }
        user.setPassword(encoder.encode(dto.getPassword()));

        Set<WorkshopToUser> links = new HashSet<>();
        if (dto.getWorkshopsIds() != null) {
            for (Long idWorkshop : dto.getWorkshopsIds()) {
                Optional<Workshop> workshopOpt = workshopRepository.findById(idWorkshop);
                if (workshopOpt.isEmpty()) {
                    return Optional.empty();
                }

                WorkshopToUser link = new WorkshopToUser();
                link.setUser(user);
                link.setWorkshop(workshopOpt.get());

                links.add(link);
            }
        }

        user.setWorkshopLinks(links);

        User saved = userRepository.save(user);
        return Optional.of(toUserDTO(saved));
    }

    public Optional<UserResponseDTO> updateUser(Long id, UserRequestDTO dto) {
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

        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        } else {
            user.setRole(Role.UNKNOWN);
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(encoder.encode(dto.getPassword()));
        }

        Set<WorkshopToUser> links = new HashSet<>();
        if (dto.getWorkshopsIds() != null) {
            for (Long idWorkshop : dto.getWorkshopsIds()) {
                Optional<Workshop> workshopOpt = workshopRepository.findById(idWorkshop);
                if (workshopOpt.isEmpty()) {
                    return Optional.empty();
                }

                WorkshopToUser link = new WorkshopToUser();
                link.setUser(user);
                link.setWorkshop(workshopOpt.get());

                links.add(link);
            }
        }

        user.getWorkshopLinks().clear();
        user.getWorkshopLinks().addAll(links);

        User saved = userRepository.save(user);
        return Optional.of(toUserDTO(saved));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}