package com.vivaeventos.eventservice.controller;

import com.vivaeventos.eventservice.dto.CreateEventRequest;
import com.vivaeventos.eventservice.dto.EventFilterRequest;
import com.vivaeventos.eventservice.dto.EventResponse;
import com.vivaeventos.eventservice.dto.EventDTO;
import com.vivaeventos.eventservice.dto.UpdatePriceRequest;
import com.vivaeventos.eventservice.service.EventService;
import com.vivaeventos.eventservice.dto.CancelEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/catalog")
    public ResponseEntity<?> getAllEvents() {
        List<EventDTO> events = eventService.getAllEvents();

        if (events.isEmpty()) {
            return ResponseEntity.ok(
                    Map.of("message", "No hay eventos disponibles en el catálogo")
            );
        }

        return ResponseEntity.ok(events);
    }

    @GetMapping
    public ResponseEntity<?> filterEvents(@ModelAttribute EventFilterRequest filter) {
        List<EventResponse> events = eventService.filterEvents(
                filter.getCategory(),
                filter.getDateFrom(),
                filter.getDateTo()
        );

        if (events.isEmpty()) {
            return ResponseEntity.ok(
                    Map.of("message", "No se encontraron eventos para los filtros aplicados")
            );
        }

        return ResponseEntity.ok(events);
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<EventResponse> updatePrice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePriceRequest request) {
        return ResponseEntity.ok(eventService.updatePrice(id, request));
    }
    /**
     * DELETE /events/{id}
     *
     * Cancela un evento y notifica a los clientes.
     *
     * ¿Por qué DELETE y no PATCH?
     * - Semánticamente DELETE indica que el recurso deja de estar disponible.
     * - No eliminamos el registro de BD (soft delete) — solo cambiamos el status.
     * - Es la convención REST más apropiada para esta operación.
     *
     * @PathVariable id     → UUID del evento a cancelar
     * @RequestBody request → DTO con el motivo (opcional, puede omitirse)
     *
     * Respuestas posibles:
     * - HTTP 200 OK        → evento cancelado exitosamente
     * - HTTP 404 Not Found → evento no existe
     * - HTTP 409 Conflict  → evento ya estaba cancelado
     *
     * Ejemplo de llamada:
     * DELETE http://localhost:8081/events/550e8400-e29b-41d4-a716-446655440000
     * Content-Type: application/json
     * { "reason": "Problemas de seguridad en el venue" }
     *
     * O sin body si no hay motivo:
     * DELETE http://localhost:8081/events/550e8400-e29b-41d4-a716-446655440000
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<EventResponse> cancelEvent(
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid CancelEventRequest request) {
        return ResponseEntity.ok(eventService.cancelEvent(id, request));
    }
}
