package com.vivaeventos.eventservice.exception;

/**
 * Excepción de dominio lanzada cuando se busca un evento que no existe en la BD.
 *
 * ¿Por qué una excepción propia en lugar de usar RuntimeException directamente?
 * - Permite capturarla específicamente en el GlobalExceptionHandler
 * - Hace el código más legible: "EventNotFoundException" es más claro que "RuntimeException"
 * - Permite agregar campos adicionales en el futuro (ej: el ID que no se encontró)
 *
 * Al extender RuntimeException es "unchecked" — no obliga a quien la llama
 * a declararla con throws, lo que mantiene el código limpio.
 *
 * El GlobalExceptionHandler la intercepta y devuelve HTTP 404 Not Found.
 */
public class EventNotFoundException extends RuntimeException {

    /**
     * @param message descripción del error, incluye el ID que no se encontró.
     *                Ejemplo: "Evento no encontrado con ID: 550e8400-e29b-41d4-a716-446655440000"
     */
    public EventNotFoundException(String message) {
        super(message);
    }
}