package com.example.demo.controllers;

import com.example.demo.dto.request.EquipmentRequestDTO;
import com.example.demo.dto.response.EquipmentResponseDTO;
import com.example.demo.models.EquipmentStatus;
import com.example.demo.services.EquipmentService;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public Iterable<?> getAllEquipment() {
        return equipmentService.getAllEquipment();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEquipmentById(@PathVariable Long id) {
        return equipmentService.getEquipmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/paged")
    public ResponseEntity<?> getAllUsersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EquipmentResponseDTO> usersPage = equipmentService.getAllEquipmentsPaged(page, size);
        return ResponseEntity.ok(usersPage);
    }

    @PostMapping
    public ResponseEntity<?> createEquipment(@RequestBody EquipmentRequestDTO dto) {
        Optional<EquipmentResponseDTO> equipmentOpt = equipmentService.createEquipment(dto);
        if (equipmentOpt.isPresent()) {
            return ResponseEntity.ok(equipmentOpt.get());
        } else {
            return ResponseEntity.badRequest().body("Invalid workshopId");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEquipment(@PathVariable Long id, @RequestBody EquipmentRequestDTO dto) {
        return equipmentService.updateEquipment(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statuses")
    public ResponseEntity<String[]> getEquipmentStatuses() {
        EquipmentStatus[] statuses = EquipmentStatus.values();
        String[] statusNames = new String[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            statusNames[i] = statuses[i].name();
        }
        return ResponseEntity.ok(statusNames);
    }
}
