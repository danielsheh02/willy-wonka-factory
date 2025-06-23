package com.example.demo.repositories;

import com.example.demo.models.Equipment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
}
