package com.example.demo.services;

import com.example.demo.dto.request.BookTicketRequestDTO;
import com.example.demo.dto.request.GenerateTicketsRequestDTO;
import com.example.demo.dto.response.GoldenTicketResponseDTO;
import com.example.demo.models.Excursion;
import com.example.demo.models.GoldenTicket;
import com.example.demo.models.TicketStatus;
import com.example.demo.repositories.ExcursionRepository;
import com.example.demo.repositories.GoldenTicketRepository;
import com.example.demo.utils.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoldenTicketService {

    @Autowired
    private GoldenTicketRepository ticketRepository;

    @Autowired
    private ExcursionRepository excursionRepository;

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Без O, I, 0, 1 (путаница)
    private static final int TICKET_LENGTH = 8; // Длина номера билета
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Генерация N уникальных золотых билетов
     */
    @Transactional
    public Map<String, Object> generateTickets(GenerateTicketsRequestDTO request) {
        if (request.getCount() == null || request.getCount() <= 0) {
            throw new RuntimeException("Количество билетов должно быть больше 0");
        }

        if (request.getCount() > 1000) {
            throw new RuntimeException("Нельзя сгенерировать больше 1000 билетов за раз");
        }

        List<GoldenTicket> generatedTickets = new ArrayList<>();
        LocalDateTime expiresAt = null;

        if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
            expiresAt = DateTimeUtils.nowUTC().plusDays(request.getExpiresInDays());
        }

        for (int i = 0; i < request.getCount(); i++) {
            String ticketNumber = generateUniqueTicketNumber();
            GoldenTicket ticket = new GoldenTicket(ticketNumber);
            ticket.setExpiresAt(expiresAt);
            generatedTickets.add(ticket);
        }

        List<GoldenTicket> savedTickets = ticketRepository.saveAll(generatedTickets);

        Map<String, Object> result = new HashMap<>();
        result.put("totalGenerated", savedTickets.size());
        result.put("tickets", savedTickets.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
        result.put("message", "Успешно сгенерировано " + savedTickets.size() + " золотых билетов");

        return result;
    }

    /**
     * Генерация уникального номера билета
     */
    private String generateUniqueTicketNumber() {
        String ticketNumber;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            ticketNumber = generateRandomCode();
            attempts++;
            
            if (attempts > maxAttempts) {
                throw new RuntimeException("Не удалось сгенерировать уникальный номер билета после " + maxAttempts + " попыток");
            }
        } while (ticketRepository.existsByTicketNumber(ticketNumber));

        return ticketNumber;
    }

    /**
     * Генерация случайного кода
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(TICKET_LENGTH);
        for (int i = 0; i < TICKET_LENGTH; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    /**
     * Получить все билеты
     */
    public List<GoldenTicketResponseDTO> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Получить билет по номеру
     */
    public GoldenTicketResponseDTO getTicketByNumber(String ticketNumber) {
        GoldenTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Билет с номером " + ticketNumber + " не найден"));
        return toDTO(ticket);
    }

    /**
     * Проверить действительность билета
     */
    public Map<String, Object> validateTicket(String ticketNumber) {
        Map<String, Object> result = new HashMap<>();
        
        GoldenTicket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);
        
        if (ticket == null) {
            result.put("valid", false);
            return result;
        }

        // Возвращаем информацию о билете в любом случае
        GoldenTicketResponseDTO ticketDTO = toDTO(ticket);
        result.put("ticket", ticketDTO);

        // Билет валиден, если он ACTIVE или BOOKED (для перезаписи)
        // USED, EXPIRED, CANCELLED - невалидные статусы
        boolean isValid = ticket.getStatus() == TicketStatus.ACTIVE || 
                         ticket.getStatus() == TicketStatus.BOOKED;
        
        // Проверяем срок действия (если указан)
        if (ticket.getExpiresAt() != null && ticket.getExpiresAt().isBefore(DateTimeUtils.nowUTC())) {
            isValid = false;
        }

        result.put("valid", isValid);
        return result;
    }

    /**
     * Забронировать билет на экскурсию (или перезаписаться)
     */
    @Transactional
    public GoldenTicketResponseDTO bookTicket(BookTicketRequestDTO request) {
        // Проверяем билет
        GoldenTicket ticket = ticketRepository.findByTicketNumber(request.getTicketNumber())
                .orElseThrow(() -> new RuntimeException("Билет с номером " + request.getTicketNumber() + " не найден"));

        // Разрешаем бронирование для ACTIVE и BOOKED (перезапись)
        if (ticket.getStatus() != TicketStatus.ACTIVE && ticket.getStatus() != TicketStatus.BOOKED) {
            throw new RuntimeException("Билет уже использован или неактивен (статус: " + ticket.getStatus() + ")");
        }

        if (ticket.getExpiresAt() != null && ticket.getExpiresAt().isBefore(DateTimeUtils.nowUTC())) {
            throw new RuntimeException("Срок действия билета истек");
        }

        // Если билет уже забронирован, проверяем, что старая экскурсия еще не прошла
        if (ticket.getStatus() == TicketStatus.BOOKED && ticket.getExcursion() != null) {
            if (ticket.getExcursion().getStartTime().isBefore(DateTimeUtils.nowUTC())) {
                throw new RuntimeException("Экскурсия по этому билету уже прошла. Перезапись невозможна.");
            }
        }

        // Проверяем экскурсию
        Excursion excursion = excursionRepository.findById(request.getExcursionId())
                .orElseThrow(() -> new RuntimeException("Экскурсия не найдена"));

        // Проверяем, что экскурсия еще не началась
        if (excursion.getStartTime().isBefore(DateTimeUtils.nowUTC())) {
            throw new RuntimeException("Экскурсия уже началась. Запись невозможна.");
        }

        // Проверяем количество свободных мест
        long bookedCount = ticketRepository.countByExcursionIdAndStatus(excursion.getId(), TicketStatus.BOOKED);
        
        // Если это перезапись на ту же экскурсию, не учитываем текущий билет
        if (ticket.getExcursion() != null && ticket.getExcursion().getId() != null && 
            ticket.getExcursion().getId().equals(excursion.getId())) {
            bookedCount--; // Вычитаем текущий билет
        }
        
        if (excursion.getParticipantsCount() != null && bookedCount >= excursion.getParticipantsCount()) {
            throw new RuntimeException("На экскурсии нет свободных мест");
        }

        // Бронируем билет (или перезаписываем)
        ticket.setExcursion(excursion);
        ticket.setStatus(TicketStatus.BOOKED);
        ticket.setBookedAt(DateTimeUtils.nowUTC());
        ticket.setHolderName(request.getHolderName());
        ticket.setHolderEmail(request.getHolderEmail());
        ticket.setHolderPhone(request.getHolderPhone());

        ticket = ticketRepository.save(ticket);

        return toDTO(ticket);
    }

    /**
     * Отменить бронирование билета
     */
    @Transactional
    public GoldenTicketResponseDTO cancelBooking(String ticketNumber) {
        GoldenTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Билет не найден"));

        if (ticket.getStatus() != TicketStatus.BOOKED) {
            throw new RuntimeException("Билет не забронирован");
        }

        // Проверяем, что экскурсия еще не началась
        if (ticket.getExcursion() != null && ticket.getExcursion().getStartTime().isBefore(DateTimeUtils.nowUTC())) {
            throw new RuntimeException("Экскурсия уже началась. Отмена невозможна.");
        }

        // Возвращаем билет в статус ACTIVE
        ticket.setExcursion(null);
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setBookedAt(null);
        ticket.setHolderName(null);
        ticket.setHolderEmail(null);
        ticket.setHolderPhone(null);

        ticket = ticketRepository.save(ticket);

        return toDTO(ticket);
    }

    /**
     * Удалить билет полностью (только для админов)
     */
    @Transactional
    public void deleteTicket(Long id) {
        GoldenTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Билет не найден"));
        
        ticketRepository.delete(ticket);
    }

    /**
     * Деактивировать билеты для начавшихся экскурсий (фоновая задача)
     */
    @Transactional
    public int deactivateTicketsForStartedExcursions() {
        LocalDateTime now = DateTimeUtils.nowUTC();
        List<GoldenTicket> ticketsToDeactivate = ticketRepository.findTicketsForStartedExcursions(now);

        for (GoldenTicket ticket : ticketsToDeactivate) {
            ticket.setStatus(TicketStatus.USED);
            ticket.setUsedAt(now);
        }

        if (!ticketsToDeactivate.isEmpty()) {
            ticketRepository.saveAll(ticketsToDeactivate);
        }

        return ticketsToDeactivate.size();
    }

    /**
     * Деактивировать истекшие билеты (фоновая задача)
     */
    @Transactional
    public int deactivateExpiredTickets() {
        LocalDateTime now = DateTimeUtils.nowUTC();
        List<GoldenTicket> expiredTickets = ticketRepository.findExpiredTickets(now);

        for (GoldenTicket ticket : expiredTickets) {
            ticket.setStatus(TicketStatus.EXPIRED);
        }

        if (!expiredTickets.isEmpty()) {
            ticketRepository.saveAll(expiredTickets);
        }

        return expiredTickets.size();
    }

    /**
     * Конвертация в DTO
     */
    private GoldenTicketResponseDTO toDTO(GoldenTicket ticket) {
        return new GoldenTicketResponseDTO(
            ticket.getId(),
            ticket.getTicketNumber(),
            ticket.getStatus(),
            ticket.getExcursion() != null ? ticket.getExcursion().getId() : null,
            ticket.getExcursion() != null ? ticket.getExcursion().getName() : null,
            ticket.getExcursion() != null ? ticket.getExcursion().getStartTime() : null,
            ticket.getHolderName(),
            ticket.getHolderEmail(),
            ticket.getHolderPhone(),
            ticket.getGeneratedAt(),
            ticket.getBookedAt(),
            ticket.getUsedAt(),
            ticket.getExpiresAt()
        );
    }
}

