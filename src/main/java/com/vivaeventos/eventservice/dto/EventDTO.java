package com.vivaeventos.eventservice.dto;

import com.vivaeventos.eventservice.model.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {
    private UUID id;
    private String name;
    private LocalDateTime eventDate;
    private String venue;
    private String category;
    private Double price;
    private String status;

    public static EventDTO from(Event event) {
        return new EventDTO(
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getVenue(),
                event.getCategory(),
                event.getPrice() != null ? event.getPrice().doubleValue() : null,
                event.getStatus() != null ? event.getStatus().name() : null
        );
    }
}
