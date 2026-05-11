package com.vivaeventos.eventservice.controller;

import com.vivaeventos.eventservice.dto.CreateEventRequest;
import com.vivaeventos.eventservice.dto.EventFilterRequest;
import com.vivaeventos.eventservice.dto.EventResponse;
import com.vivaeventos.eventservice.dto.EventDTO;
import com.vivaeventos.eventservice.dto.UpdatePriceRequest;
import com.vivaeventos.eventservice.service.EventService;
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
}
