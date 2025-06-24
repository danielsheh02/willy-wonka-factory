package com.example.demo.services;

import com.example.demo.dto.request.WorkshopRequestDTO;
import com.example.demo.dto.response.WorkshopResponseDTO;
import com.example.demo.dto.short_db.EquipmentShortDTO;
import com.example.demo.dto.short_db.UserShortDTO;
import com.example.demo.models.Equipment;
import com.example.demo.models.User;
import com.example.demo.models.Workshop;
import com.example.demo.models.WorkshopToUser;
import com.example.demo.repositories.EquipmentRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WorkshopRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    public EquipmentShortDTO toEquipmentDTO(Equipment equipment) {
        return new EquipmentShortDTO(
                equipment.getId(),
                equipment.getName(),
                equipment.getDescription(),
                equipment.getModel(),
                equipment.getStatus(),
                equipment.getHealth(),
                equipment.getTemperature(),
                equipment.getLastServicedAt());
    }

    private WorkshopResponseDTO toWorkshopDTO(Workshop workshop) {
        Set<UserShortDTO> foremans = workshop.getForemanLinks()
                .stream()
                .map(WorkshopToUser::getUser)
                .map(user -> new UserShortDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getRole(),
                        user.getIsBanned()))
                .collect(Collectors.toSet());

        Set<EquipmentShortDTO> equipmentDTOs = workshop.getEquipments()
                .stream()
                .map(this::toEquipmentDTO)
                .collect(Collectors.toSet());

        return new WorkshopResponseDTO(
                workshop.getId(),
                workshop.getName(),
                workshop.getDescription(),
                foremans, equipmentDTOs);
    }

    public Iterable<WorkshopResponseDTO> getAllWorkshops() {
        return workshopRepository.findAll()
                .stream()
                .map(this::toWorkshopDTO)
                .collect(Collectors.toList());
    }

    public Page<WorkshopResponseDTO> getAllWorkshopsPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return workshopRepository.findAll(pageable)
                .map(this::toWorkshopDTO);
    }

    public Optional<WorkshopResponseDTO> getWorkshopById(Long id) {
        return workshopRepository.findById(id)
                .map(this::toWorkshopDTO);
    }

    public Optional<WorkshopResponseDTO> createWorkshop(WorkshopRequestDTO dto) {
        Workshop workshop = new Workshop();
        workshop.setName(dto.getName());
        workshop.setDescription(dto.getDescription());
        Set<WorkshopToUser> foremanLinks = new HashSet<>();
        if (dto.getForemanIds() != null) {
            for (Long userId : dto.getForemanIds()) {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isEmpty()) {
                    return Optional.empty();
                }
                WorkshopToUser link = new WorkshopToUser();
                link.setWorkshop(workshop);
                link.setUser(userOpt.get());
                foremanLinks.add(link);
            }
        }
        workshop.setForemanLinks(foremanLinks);

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

        workshop.setEquipments(equipments);

        // We need this because Workshop do not own Set<Equipment> equipments
        for (Equipment equipment : equipments) {
            equipment.setWorkshop(workshop);
        }

        Workshop saved = workshopRepository.save(workshop);
        return Optional.of(toWorkshopDTO(saved));
    }

    public Optional<WorkshopResponseDTO> updateWorkshop(Long id, WorkshopRequestDTO dto) {
        Optional<Workshop> workshopOpt = workshopRepository.findById(id);
        if (workshopOpt.isEmpty())
            return Optional.empty();

        Workshop workshop = workshopOpt.get();

        workshop.setName(dto.getName());
        workshop.setDescription(dto.getDescription());
        Set<WorkshopToUser> foremanLinks = new HashSet<>();
        if (dto.getForemanIds() != null) {
            for (Long userId : dto.getForemanIds()) {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isEmpty()) {
                    return Optional.empty();
                }
                WorkshopToUser link = new WorkshopToUser();
                link.setWorkshop(workshop);
                link.setUser(userOpt.get());
                foremanLinks.add(link);
            }
        }
        workshop.getForemanLinks().clear();
        workshop.getForemanLinks().addAll(foremanLinks);

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

        for (Equipment oldEquipment : workshop.getEquipments()) {
            oldEquipment.setWorkshop(null);
        }

        for (Equipment equipment : equipments) {
            equipment.setWorkshop(workshop);
        }

        workshop.setEquipments(equipments);

        Workshop saved = workshopRepository.save(workshop);
        return Optional.of(toWorkshopDTO(saved));
    }

    public void deleteWorkshop(Long id) {
        workshopRepository.deleteById(id);
    }
}
