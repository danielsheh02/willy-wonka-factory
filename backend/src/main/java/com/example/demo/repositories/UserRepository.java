package com.example.demo.repositories;

import com.example.demo.models.Role;
import com.example.demo.models.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    List<User> findByRoleIn(List<Role> roles);

}
