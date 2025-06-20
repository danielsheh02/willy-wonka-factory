package com.example.demo.repositories;

import com.example.demo.models.User;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    Boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

}
