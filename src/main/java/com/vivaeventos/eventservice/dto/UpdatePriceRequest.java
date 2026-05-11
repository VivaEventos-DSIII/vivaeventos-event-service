package com.vivaeventos.eventservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) para la solicitud de actualización de precio.
 *
 * ¿Por qué un DTO y no usar la entidad directamente?
 * - Evita exponer campos internos de la entidad (id, status, organizerId, etc.)
 * - Permite validar solo los campos relevantes para esta operación
 * - Si la entidad cambia internamente, este contrato con el cliente no se rompe
 *
 * Uso: PATCH /events/{id}/price
 * Body: { "newPrice": 85000.00 }
 */
public class UpdatePriceRequest {

    /**
     * @NotNull → Spring valida que el campo exista en el JSON.
     *            Si falta, devuelve automáticamente HTTP 400 Bad Request.
     *
     * @DecimalMin → El precio mínimo es 0 (eventos gratuitos permitidos).
     *               inclusive = true significa que 0.00 es válido.
     *               Un precio negativo no tiene sentido de negocio.
     */
    @NotNull(message = "El nuevo precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
    private BigDecimal newPrice;

    /**
     * Constructor vacío requerido por Jackson (el deserializador JSON de Spring).
     * Sin este constructor, Spring no puede convertir el JSON del request en este objeto.
     */
    public UpdatePriceRequest() {}

    /**
     * Constructor con parámetro — útil para crear instancias en los tests
     * sin tener que llamar al setter.
     *
     * @param newPrice el nuevo precio a establecer
     */
    public UpdatePriceRequest(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }

    /**
     * Getter requerido por Jackson para serializar/deserializar el objeto.
     * @return el nuevo precio solicitado
     */
    public BigDecimal getNewPrice() { return newPrice; }

    /**
     * Setter requerido por Jackson para asignar el valor desde el JSON.
     * @param newPrice el nuevo precio
     */
    public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }
}