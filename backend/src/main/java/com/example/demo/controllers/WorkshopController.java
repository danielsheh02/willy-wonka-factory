package com.example.demo.controllers;

import com.example.demo.dto.request.WorkshopRequestDTO;
import com.example.demo.dto.response.WorkshopResponseDTO;
import com.example.demo.services.WorkshopService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/workshops")
public class WorkshopController {

    private final WorkshopService workshopService;

    public WorkshopController(WorkshopService workshopService) {
        this.workshopService = workshopService;
    }

    @GetMapping
    public Iterable<?> getAll() {
        return workshopService.getAllWorkshops();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return workshopService.getWorkshopById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/paged")
    public ResponseEntity<?> getAllWorkshopsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<WorkshopResponseDTO> workshopsPage = workshopService.getAllWorkshopsPaged(page, size);
        return ResponseEntity.ok(workshopsPage);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody WorkshopRequestDTO dto) {
        Optional<WorkshopResponseDTO> workshopOpt = workshopService.createWorkshop(dto);
        if (workshopOpt.isPresent()) {
            return ResponseEntity.ok(workshopOpt.get());
        } else {
            return ResponseEntity.badRequest().body("Invalid foreman IDs");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkshop(@PathVariable Long id, @RequestBody WorkshopRequestDTO dto) {
        return workshopService.updateWorkshop(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workshopService.deleteWorkshop(id);
        return ResponseEntity.noContent().build();
    }
}
