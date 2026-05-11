package com.vivaeventos.eventservice.dto;

import com.vivaeventos.eventservice.model.Event;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class EventResponse {

    private UUID id;
    private String name;
    private String description;
    private String category;
    private String venue;
    private LocalDateTime eventDate;
    private Integer capacity;
    private Integer availableTickets;
    private BigDecimal price;
    private String status;
    private UUID organizerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Método estático de fábrica: convierte una entidad Event en un EventResponse.
     * Se usa en el Service así: EventResponse.from(event)
     */
    public static EventResponse from(Event event) {
        EventResponse response = new EventResponse();
        response.id          = event.getId();
        response.name        = event.getName();
        response.description = event.getDescription();
        response.category    = event.getCategory();
        response.venue       = event.getVenue();
        response.eventDate   = event.getEventDate();
        response.capacity    = event.getCapacity();
        response.availableTickets = event.getAvailableTickets();
        response.price       = event.getPrice();
        response.status      = event.getStatus().name();  // Enum → String
        response.organizerId = event.getOrganizerId();
        response.createdAt   = event.getCreatedAt();
        response.updatedAt   = event.getUpdatedAt();
        return response;
    }
}