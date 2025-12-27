package com.example.demo.controllers;

import com.example.demo.dto.request.ExcursionRequestDTO;
import com.example.demo.dto.response.ExcursionResponseDTO;
import com.example.demo.models.ExcursionStatus;
import com.example.demo.services.ExcursionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/excursions")
@CrossOrigin(origins = "*")
public class ExcursionController {

    @Autowired
    private ExcursionService excursionService;

    @GetMapping
    public ResponseEntity<List<ExcursionResponseDTO>> getAllExcursions() {
        return ResponseEntity.ok(excursionService.getAllExcursions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExcursionResponseDTO> getExcursionById(@PathVariable Long id) {
        return excursionService.getExcursionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/guide/{guideId}")
    public ResponseEntity<List<ExcursionResponseDTO>> getExcursionsByGuide(@PathVariable Long guideId) {
        return ResponseEntity.ok(excursionService.getExcursionsByGuideId(guideId));
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<ExcursionStatus>> getAllStatuses() {
        return ResponseEntity.ok(excursionService.getAllStatuses());
    }

    @PostMapping
    public ResponseEntity<?> createExcursion(@RequestBody ExcursionRequestDTO dto) {
        try {
            ExcursionResponseDTO excursion = excursionService.createExcursion(dto);
            return ResponseEntity.ok(excursion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExcursion(@PathVariable Long id, @RequestBody ExcursionRequestDTO dto) {
        try {
            ExcursionResponseDTO excursion = excursionService.updateExcursion(id, dto);
            return ResponseEntity.ok(excursion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExcursion(@PathVariable Long id) {
        try {
            excursionService.deleteExcursion(id);
            return ResponseEntity.ok(Map.of("message", "Экскурсия удалена"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/check-availability")
    public ResponseEntity<Map<String, Object>> checkRouteAvailability(@RequestBody ExcursionRequestDTO dto) {
        Map<String, Object> result = excursionService.checkRouteAvailability(dto);
        return ResponseEntity.ok(result);
    }
}

