package com.example.demo.repositories;

import com.example.demo.models.Equipment;
import org.springframework.data.repository.CrudRepository;

public interface EquipmentRepository extends CrudRepository<Equipment, Long> {
}
