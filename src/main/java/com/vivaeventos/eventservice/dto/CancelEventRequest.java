package com.vivaeventos.eventservice.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de cancelación de un evento.
 *
 * ¿Por qué un DTO y no solo el ID en la URL?
 * - Permite enviar el motivo de cancelación en el body.
 * - El motivo es opcional — si no se envía, se usa uno genérico.
 * - En el futuro puede ampliarse con política de devolución, etc.
 *
 * Uso: DELETE /events/{id}
 * Body (opcional): { "reason": "Problemas de seguridad en el venue" }
 */
public class CancelEventRequest {

    /**
     * Motivo de cancelación — opcional.
     * Se incluirá en la notificación que reciben los clientes.
     * Máximo 500 caracteres para evitar textos excesivamente largos.
     */
    @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
    private String reason;

    /**
     * Constructor vacío requerido por Jackson.
     */
    public CancelEventRequest() {}

    /**
     * Constructor con parámetro — útil para los tests.
     * @param reason motivo de cancelación
     */
    public CancelEventRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}