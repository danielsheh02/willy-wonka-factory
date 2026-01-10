package com.example.demo.controllers;

import com.example.demo.dto.request.BookTicketRequestDTO;
import com.example.demo.dto.request.GenerateTicketsRequestDTO;
import com.example.demo.dto.response.GoldenTicketResponseDTO;
import com.example.demo.services.GoldenTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
public class GoldenTicketController {

    @Autowired
    private GoldenTicketService ticketService;

    /**
     * Генерация золотых билетов (только для админов)
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTickets(@RequestBody GenerateTicketsRequestDTO request) {
        try {
            Map<String, Object> result = ticketService.generateTickets(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Получить все билеты
     */
    @GetMapping
    public ResponseEntity<List<GoldenTicketResponseDTO>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    /**
     * Получить билет по номеру
     */
    @GetMapping("/{ticketNumber}")
    public ResponseEntity<?> getTicketByNumber(@PathVariable String ticketNumber) {
        try {
            GoldenTicketResponseDTO ticket = ticketService.getTicketByNumber(ticketNumber);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Проверить действительность билета
     */
    @GetMapping("/validate/{ticketNumber}")
    public ResponseEntity<Map<String, Object>> validateTicket(@PathVariable String ticketNumber) {
        Map<String, Object> result = ticketService.validateTicket(ticketNumber);
        return ResponseEntity.ok(result);
    }

    /**
     * Забронировать билет на экскурсию
     */
    @PostMapping("/book")
    public ResponseEntity<?> bookTicket(@RequestBody BookTicketRequestDTO request) {
        try {
            GoldenTicketResponseDTO ticket = ticketService.bookTicket(request);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Отменить бронирование
     */
    @DeleteMapping("/{ticketNumber}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String ticketNumber) {
        try {
            GoldenTicketResponseDTO ticket = ticketService.cancelBooking(ticketNumber);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Удалить билет полностью из базы данных (только для админов)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTicket(@PathVariable Long id) {
        try {
            ticketService.deleteTicket(id);
            return ResponseEntity.ok(Map.of("message", "Билет удален"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

