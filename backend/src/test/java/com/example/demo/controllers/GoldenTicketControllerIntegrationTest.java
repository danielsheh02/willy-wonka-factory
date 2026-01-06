package com.example.demo.controllers;

import com.example.demo.BaseIntegrationTest;
import com.example.demo.dto.request.BookTicketRequestDTO;
import com.example.demo.dto.request.GenerateTicketsRequestDTO;
import com.example.demo.models.*;
import com.example.demo.repositories.ExcursionRepository;
import com.example.demo.repositories.GoldenTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@DisplayName("Интеграционные тесты для GoldenTicketController")
public class GoldenTicketControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GoldenTicketRepository goldenTicketRepository;

    @Autowired
    private ExcursionRepository excursionRepository;

    private Excursion testExcursion;

    @Override
    @BeforeEach
    public void setupBaseTest() {
        super.setupBaseTest();
        // Важно: сначала удаляем билеты (child), потом экскурсии (parent)
        goldenTicketRepository.deleteAll();
        excursionRepository.deleteAll();
        
        // Создаем тестовую экскурсию
        User guide = getUserForRole(Role.GUIDE);
        if (guide != null) {
            testExcursion = new Excursion();
            testExcursion.setName("Тестовая экскурсия");
            testExcursion.setStartTime(LocalDateTime.now().plusDays(5));
            testExcursion.setParticipantsCount(20);
            testExcursion.setGuide(guide);
            testExcursion.setStatus(ExcursionStatus.CONFIRMED);
            testExcursion = excursionRepository.save(testExcursion);
        }
    }

    @Test
    @DisplayName("Генерация билетов ADMIN'ом")
    public void testGenerateTicketsAsAdmin() throws Exception {
        GenerateTicketsRequestDTO dto = new GenerateTicketsRequestDTO();
        dto.setCount(5);
        dto.setExpiresInDays(30);

        mockMvc.perform(post("/api/tickets/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGenerated", is(5))) // Исправлено: API возвращает totalGenerated
                .andExpect(jsonPath("$.tickets", hasSize(5)));
    }

    @Test
    @DisplayName("Генерация билетов WORKER'ом запрещена")
    public void testGenerateTicketsAsWorkerForbidden() throws Exception {
        GenerateTicketsRequestDTO dto = new GenerateTicketsRequestDTO();
        dto.setCount(3);

        mockMvc.perform(post("/api/tickets/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.WORKER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение всех билетов ADMIN'ом")
    public void testGetAllTicketsAsAdmin() throws Exception {
        GoldenTicket ticket = new GoldenTicket("TICKET01");
        goldenTicketRepository.save(ticket);

        mockMvc.perform(get("/api/tickets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Получение билета по номеру")
    public void testGetTicketByNumber() throws Exception {
        GoldenTicket ticket = new GoldenTicket("TICKET02");
        goldenTicketRepository.save(ticket);

        mockMvc.perform(get("/api/tickets/TICKET02")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber", is("TICKET02")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("Получение несуществующего билета")
    public void testGetNonExistentTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/NONEXIST")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("Валидация билета (публичный доступ)")
    public void testValidateTicket() throws Exception {
        GoldenTicket ticket = new GoldenTicket("VALID001");
        ticket.setStatus(TicketStatus.ACTIVE);
        goldenTicketRepository.save(ticket);

        mockMvc.perform(get("/api/tickets/validate/VALID001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.ticket.ticketNumber", is("VALID001"))); // Исправлено: ticketNumber внутри объекта ticket
    }

    @Test
    @DisplayName("Валидация недействительного билета")
    public void testValidateInvalidTicket() throws Exception {
        GoldenTicket ticket = new GoldenTicket("USED001");
        ticket.setStatus(TicketStatus.USED);
        goldenTicketRepository.save(ticket);

        mockMvc.perform(get("/api/tickets/validate/USED001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)));
    }

    @Test
    @DisplayName("Бронирование билета на экскурсию (публичный доступ)")
    public void testBookTicket() throws Exception {
        GoldenTicket ticket = new GoldenTicket("BOOK001");
        ticket.setStatus(TicketStatus.ACTIVE);
        goldenTicketRepository.save(ticket);

        BookTicketRequestDTO dto = new BookTicketRequestDTO();
        dto.setTicketNumber("BOOK001");
        dto.setExcursionId(testExcursion.getId());
        dto.setHolderName("Чарли Бакет");
        dto.setHolderEmail("charlie@example.com");
        dto.setHolderPhone("+1234567890");

        mockMvc.perform(post("/api/tickets/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber", is("BOOK001")))
                .andExpect(jsonPath("$.status", is("BOOKED")))
                .andExpect(jsonPath("$.holderName", is("Чарли Бакет")));
    }

    @Test
    @DisplayName("Бронирование уже забронированного билета")
    public void testBookAlreadyBookedTicket() throws Exception {
        GoldenTicket ticket = new GoldenTicket("BOOKED01");
        ticket.setStatus(TicketStatus.BOOKED);
        ticket.setExcursion(testExcursion);
        goldenTicketRepository.save(ticket);

        BookTicketRequestDTO dto = new BookTicketRequestDTO();
        dto.setTicketNumber("BOOKED01");
        dto.setExcursionId(testExcursion.getId());
        dto.setHolderName("Кто-то другой");

        mockMvc.perform(post("/api/tickets/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Отмена бронирования (публичный доступ)")
    public void testCancelBooking() throws Exception {
        GoldenTicket ticket = new GoldenTicket("CANCEL01");
        ticket.setStatus(TicketStatus.BOOKED);
        ticket.setExcursion(testExcursion);
        ticket.setHolderName("Тестовый пользователь");
        goldenTicketRepository.save(ticket);

        mockMvc.perform(delete("/api/tickets/CANCEL01/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber", is("CANCEL01")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("Отмена бронирования незабронированного билета")
    public void testCancelNonBookedTicket() throws Exception {
        GoldenTicket ticket = new GoldenTicket("ACTIVE01");
        ticket.setStatus(TicketStatus.ACTIVE);
        goldenTicketRepository.save(ticket);

        mockMvc.perform(delete("/api/tickets/ACTIVE01/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("Бронирование билета на несуществующую экскурсию")
    public void testBookTicketForNonExistentExcursion() throws Exception {
        GoldenTicket ticket = new GoldenTicket("BOOK002");
        ticket.setStatus(TicketStatus.ACTIVE);
        goldenTicketRepository.save(ticket);

        BookTicketRequestDTO dto = new BookTicketRequestDTO();
        dto.setTicketNumber("BOOK002");
        dto.setExcursionId(99999L);
        dto.setHolderName("Тест");

        mockMvc.perform(post("/api/tickets/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("Генерация большого количества билетов")
    public void testGenerateManyTickets() throws Exception {
        GenerateTicketsRequestDTO dto = new GenerateTicketsRequestDTO();
        dto.setCount(20);

        mockMvc.perform(post("/api/tickets/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGenerated", is(20)))
                .andExpect(jsonPath("$.tickets", hasSize(20)));
    }

    @Test
    @DisplayName("Валидация несуществующего билета")
    public void testValidateNonExistentTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/validate/NOTEXIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)));
    }

    @Test
    @DisplayName("Получение билетов GUIDE'ом")
    public void testGetAllTicketsAsGuide() throws Exception {
        GoldenTicket ticket = new GoldenTicket("GUIDE01");
        goldenTicketRepository.save(ticket);

        mockMvc.perform(get("/api/tickets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForRole(Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}

