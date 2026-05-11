package com.vivaeventos.eventservice.dto;

import com.vivaeventos.eventservice.model.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor

/**
 * DTO simplificado para el catálogo de eventos
 *
 * Solo expone los campos necesarios:
 *   - Nombre del evento
 *   - Fecha del evento
 *   - Lugar del evento
 */
public class EventDTO {

    private String name;
    private LocalDateTime eventDate;
    private String venue;

    /**
     * Método estático de fábrica: convierte una entidad Event en un EventDTO.
     * <p>
     * Se usa así: EventDTO.from(event)
     */
    public static EventDTO from(Event event) {
        return new EventDTO(
                event.getName(),
                event.getEventDate(),
                event.getVenue()
        );
    }
}

