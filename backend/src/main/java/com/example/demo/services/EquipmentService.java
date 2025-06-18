package com.example.demo.services;

import com.example.demo.dto.EquipmentRequestDTO;
import com.example.demo.models.Equipment;
import com.example.demo.models.Workshop;
import com.example.demo.repositories.EquipmentRepository;
import com.example.demo.repositories.WorkshopRepository;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    private final WorkshopRepository workshopRepository;

    public EquipmentService(EquipmentRepository equipmentRepository, WorkshopRepository workshopRepository) {
        this.equipmentRepository = equipmentRepository;
        this.workshopRepository = workshopRepository;
    }

    public Iterable<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    public Optional<Equipment> getEquipmentById(Long id) {
        return equipmentRepository.findById(id);
    }

    public Optional<Equipment> createEquipment(EquipmentRequestDTO dto) {
        Workshop workshop = null;
        if (dto.getWorkshopId() != null) {
            Optional<Workshop> workshopOpt = workshopRepository.findById(dto.getWorkshopId());
            if (workshopOpt.isEmpty())
                return Optional.empty();
            workshop = workshopOpt.get();
        }

        Equipment equipment = new Equipment();
        equipment.setName(dto.getName());
        equipment.setDescription(dto.getDescription());
        equipment.setModel(dto.getModel());
        equipment.setStatus(dto.getStatus());
        equipment.setHealth(dto.getHealth());
        equipment.setTemperature(dto.getTemperature());
        equipment.setLastServicedAt(dto.getLastServicedAt());
        equipment.setWorkshop(workshop);
        return Optional.of(equipmentRepository.save(equipment));
    }

    public Optional<Equipment> updateEquipment(Long id, EquipmentRequestDTO dto) {
        Optional<Equipment> equipmentOpt = equipmentRepository.findById(id);
        if (equipmentOpt.isEmpty())
            return Optional.empty();

        Equipment equipment = equipmentOpt.get();

        Workshop workshop = null;
        if (dto.getWorkshopId() != null) {
            Optional<Workshop> workshopOpt = workshopRepository.findById(dto.getWorkshopId());
            if (workshopOpt.isEmpty())
                return Optional.empty();
            workshop = workshopOpt.get();
        }

        equipment.setName(dto.getName());
        equipment.setDescription(dto.getDescription());
        equipment.setModel(dto.getModel());
        equipment.setStatus(dto.getStatus());
        equipment.setHealth(dto.getHealth());
        equipment.setTemperature(dto.getTemperature());
        equipment.setLastServicedAt(dto.getLastServicedAt());
        equipment.setWorkshop(workshop);
        return Optional.of(equipmentRepository.save(equipment));
    }

    public void deleteEquipment(Long id) {
        equipmentRepository.deleteById(id);
    }
}
