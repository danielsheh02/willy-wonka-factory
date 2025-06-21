package com.example.demo.services;

import com.example.demo.dto.WorkshopRequestDTO;
import com.example.demo.models.Equipment;
import com.example.demo.models.User;
import com.example.demo.models.Workshop;
import com.example.demo.repositories.EquipmentRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WorkshopRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class WorkshopService {

    private final WorkshopRepository workshopRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;

    public WorkshopService(WorkshopRepository workshopRepository, UserRepository userRepository,
            EquipmentRepository equipmentRepository) {
        this.workshopRepository = workshopRepository;
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public Iterable<Workshop> getAllWorkshops() {
        return workshopRepository.findAll();
    }

    public Optional<Workshop> getWorkshopById(Long id) {
        return workshopRepository.findById(id);
    }

    public Optional<Workshop> createWorkshop(WorkshopRequestDTO dto) {
        // We can do this because Workshop own Set<User> foremans
        Set<User> foremans = new HashSet<>();
        if (dto.getForemanIds() != null) {
            for (Long id : dto.getForemanIds()) {
                Optional<User> userOpt = userRepository.findById(id);
                if (userOpt.isEmpty()) {
                    return Optional.empty();
                }
                foremans.add(userOpt.get());
            }
        }

        Set<Equipment> equipments = new HashSet<>();
        if (dto.getEquipmentIds() != null) {
            for (Long id : dto.getEquipmentIds()) {
                Optional<Equipment> eqOpt = equipmentRepository.findById(id);
                if (eqOpt.isEmpty()) {
                    return Optional.empty();
                }
                equipments.add(eqOpt.get());
            }
        }

        Workshop workshop = new Workshop();
        workshop.setName(dto.getName());
        workshop.setDescription(dto.getDescription());
        workshop.setForemans(foremans);
        workshop.setEquipments(equipments);

        // We need this because Workshop do not own Set<Equipment> equipments
        for (Equipment equipment : equipments) {
            equipment.setWorkshop(workshop);
        }

        return Optional.of(workshopRepository.save(workshop));
    }

    public void deleteWorkshop(Long id) {
        workshopRepository.deleteById(id);
    }

    public Optional<Workshop> updateWorkshop(Long id, WorkshopRequestDTO dto) {
        Optional<Workshop> workshopOpt = workshopRepository.findById(id);
        if (workshopOpt.isEmpty())
            return Optional.empty();

        Workshop workshop = workshopOpt.get();

        // We can do this because Workshop own Set<User> foremans
        Set<User> foremans = new HashSet<>();
        if (dto.getForemanIds() != null) {
            for (Long idForeman : dto.getForemanIds()) {
                Optional<User> userOpt = userRepository.findById(idForeman);
                if (userOpt.isEmpty()) {
                    return Optional.empty();
                }
                foremans.add(userOpt.get());
            }
        }

        Set<Equipment> equipments = new HashSet<>();
        if (dto.getEquipmentIds() != null) {
            for (Long idEquipment : dto.getEquipmentIds()) {
                Optional<Equipment> eqOpt = equipmentRepository.findById(idEquipment);
                if (eqOpt.isEmpty()) {
                    return Optional.empty();
                }
                equipments.add(eqOpt.get());
            }
        }

        // We need this because Workshop do not own Set<Equipment> equipments
        for (Equipment oldEquipment : workshop.getEquipments()) {
            oldEquipment.setWorkshop(null);
        }

        for (Equipment equipment : equipments) {
            equipment.setWorkshop(workshop);
        }

        workshop.setName(dto.getName());
        workshop.setDescription(dto.getDescription());
        workshop.setForemans(foremans);
        workshop.setEquipments(equipments);

        return Optional.of(workshopRepository.save(workshop));
    }
}
