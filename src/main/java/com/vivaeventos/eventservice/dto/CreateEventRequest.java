package com.vivaeventos.eventservice.dto;

import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO = Data Transfer Object
 *
 * Este objeto representa el JSON que llega en el body del POST /events.
 * Ejemplo de JSON que el cliente envía:
 *
 * {
 *   "name": "Concierto Rock en el Parque",
 *   "description": "Festival de rock al aire libre",
 *   "category": "CONCIERTO",
 *   "venue": "Parque Simón Bolívar, Bogotá",
 *   "eventDate": "2025-08-15T18:00:00",
 *   "capacity": 5000,
 *   "price": 85000.00,
 *   "organizerId": "550e8400-e29b-41d4-a716-446655440000"
 * }
 *
 * Las anotaciones @NotBlank, @NotNull, etc. validan el JSON automáticamente
 * antes de que llegue al service. Si algo falla, Spring devuelve 400 Bad Request.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateEventRequest {

    // @NotBlank = no puede ser null, vacío ni solo espacios
    @NotBlank(message = "El nombre del evento es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String name;

    // Sin @NotBlank porque la descripción es opcional
    private String description;

    @Size(max = 100, message = "La categoría no puede superar 100 caracteres")
    private String category;

    @NotBlank(message = "El lugar del evento es obligatorio")
    @Size(max = 255, message = "El lugar no puede superar 255 caracteres")
    private String venue;

    // @NotNull = no puede ser null (pero sí puede tener cualquier valor)
    // @Future  = la fecha debe ser en el futuro (no puedes crear un evento que ya pasó)
    @NotNull(message = "La fecha del evento es obligatoria")
    @Future(message = "La fecha del evento debe ser en el futuro")
    private LocalDateTime eventDate;

    // @Min(1) = el aforo mínimo es 1 persona
    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad debe ser al menos 1")
    private Integer capacity;

    // @DecimalMin = el precio mínimo es 0 (eventos gratuitos son válidos)
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private BigDecimal price;

    // El organizerId viene del token JWT en producción.
    // Por ahora lo recibimos en el request para simplificar el MVP.
    @NotNull(message = "El ID del organizador es obligatorio")
    private UUID organizerId;
}