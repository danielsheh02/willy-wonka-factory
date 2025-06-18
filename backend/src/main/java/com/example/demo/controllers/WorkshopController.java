package com.example.demo.controllers;

import com.example.demo.dto.WorkshopRequestDTO;
import com.example.demo.models.Workshop;
import com.example.demo.services.WorkshopService;
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
    public Iterable<Workshop> getAll() {
        return workshopService.getAllWorkshops();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workshop> getById(@PathVariable Long id) {
        return workshopService.getWorkshopById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody WorkshopRequestDTO dto) {
        Optional<Workshop> workShowOpt = workshopService.createWorkshop(dto);
        if (workShowOpt.isPresent()) {
            return ResponseEntity.ok(workShowOpt.get());
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
