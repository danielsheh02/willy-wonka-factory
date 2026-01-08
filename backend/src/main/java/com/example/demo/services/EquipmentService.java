package com.example.demo.services;

import com.example.demo.dto.request.EquipmentRequestDTO;
import com.example.demo.dto.response.EquipmentResponseDTO;
import com.example.demo.dto.short_db.WorkshopShortDTO;
import com.example.demo.models.Equipment;
import com.example.demo.models.Workshop;
import com.example.demo.repositories.EquipmentRepository;
import com.example.demo.repositories.WorkshopRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private EquipmentResponseDTO toEquipmentDTO(Equipment equipment) {
        WorkshopShortDTO workshopDTO = null;
        if (equipment.getWorkshop() != null) {
            workshopDTO = new WorkshopShortDTO(
                    equipment.getWorkshop().getId(),
                    equipment.getWorkshop().getName(),
                    equipment.getWorkshop().getDescription());
        }

        return new EquipmentResponseDTO(
                equipment.getId(),
                equipment.getName(),
                equipment.getDescription(),
                equipment.getModel(),
                equipment.getStatus(),
                equipment.getHealth(),
                equipment.getTemperature(),
                equipment.getLastServicedAt(),
                workshopDTO);
    }

    public Iterable<EquipmentResponseDTO> getAllEquipment() {
        return equipmentRepository.findAll().stream()
                .map(this::toEquipmentDTO)
                .toList();
    }

    public Page<EquipmentResponseDTO> getAllEquipmentsPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return equipmentRepository.findAll(pageable)
                .map(this::toEquipmentDTO);
    }

    public Optional<EquipmentResponseDTO> getEquipmentById(Long id) {
        return equipmentRepository.findById(id).map(this::toEquipmentDTO);
    }

    public Optional<EquipmentResponseDTO> createEquipment(EquipmentRequestDTO dto) {
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

        Equipment saved = equipmentRepository.save(equipment);
        return Optional.of(toEquipmentDTO(saved));
    }

    public Optional<EquipmentResponseDTO> updateEquipment(Long id, EquipmentRequestDTO dto) {
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
        Equipment saved = equipmentRepository.save(equipment);
        return Optional.of(toEquipmentDTO(saved));
    }

    public void deleteEquipment(Long id) {
        equipmentRepository.deleteById(id);
    }

    public long countEquipment() {
        return equipmentRepository.count();
    }
}
