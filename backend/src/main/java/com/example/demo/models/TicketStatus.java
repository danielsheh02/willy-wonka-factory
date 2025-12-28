package com.example.demo.models;

public enum TicketStatus {
    ACTIVE,      // Активный, не использован
    BOOKED,      // Забронирован на экскурсию
    USED,        // Экскурсия прошла, билет использован
    EXPIRED,     // Истек срок действия
    CANCELLED    // Отменен/аннулирован
}

