package com.vivaeventos.eventservice.service;

import com.vivaeventos.eventservice.dto.CreateEventRequest;
import com.vivaeventos.eventservice.dto.EventResponse;
import com.vivaeventos.eventservice.dto.EventDTO;
import com.vivaeventos.eventservice.model.Event;
import com.vivaeventos.eventservice.repository.EventRepository;
import com.vivaeventos.eventservice.dto.CancelEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.vivaeventos.eventservice.dto.UpdatePriceRequest;
import com.vivaeventos.eventservice.exception.EventNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @Service → Marca esta clase como componente de lógica de negocio.
 *
 * RESPONSABILIDADES de esta capa:
 *  1. Recibir el DTO del controller
 *  2. Convertirlo a entidad (Event)
 *  3. Guardarlo en la BD con el repository
 *  4. Publicar el evento en Kafka (para que otros microservicios se enteren)
 *  5. Devolver el EventResponse al controller
 */
@Service
public class EventService {

    // Logger para registrar qué pasa (útil para depurar y para auditoría)
    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    // Spring inyecta estas dependencias automáticamente (@Autowired implícito en constructor)
    private final EventRepository eventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Leemos el nombre del topic desde application.yml
    // Si no existe la propiedad, usa "event.created" como valor por defecto
    @Value("${kafka.topics.event-created:event.created}")
    private String eventCreatedTopic;

    @Value("${kafka.topics.event-cancelled:event.cancelled}")
    private String eventCancelledTopic;

    // Inyección por constructor (recomendada sobre @Autowired en campo)
    public EventService(EventRepository eventRepository,
                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.eventRepository = eventRepository;
        this.kafkaTemplate   = kafkaTemplate;

    }

    /**
     * Crea un nuevo evento.
     *
     * @Transactional garantiza que si algo falla a mitad del proceso,
     * la BD hace rollback automático (no quedan datos a medias).
     *
     * @param request  DTO con los datos del evento (ya validados por el controller)
     * @return         EventResponse con los datos del evento creado (incluye el UUID generado)
     */
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        log.info("Creando evento: '{}' para organizador {}", request.getName(), request.getOrganizerId());

        // 1. Convertir el DTO en entidad
        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setVenue(request.getVenue());
        event.setEventDate(request.getEventDate());
        event.setCapacity(request.getCapacity());
        event.setAvailableTickets(request.getCapacity());
        event.setPrice(request.getPrice());
        event.setOrganizerId(request.getOrganizerId());
        event.setStatus(Event.EventStatus.ACTIVE);  // Todo evento nuevo nace como ACTIVE

        // 2. Guardar en la BD — saveAndFlush fuerza el INSERT inmediato para que
        //    @CreationTimestamp y @UpdateTimestamp se pueblen antes de construir la respuesta
        Event savedEvent = eventRepository.saveAndFlush(event);
        log.info("Evento guardado con ID: {}", savedEvent.getId());

        // 3. Publicar mensaje en Kafka para notificar a otros microservicios
        //    (por ejemplo: notification-service puede enviar confirmación al organizador)
        publishEventCreated(savedEvent);

        // 4. Convertir la entidad guardada en DTO de respuesta y devolverlo
        return EventResponse.from(savedEvent);
    }

    /**
     * Filtra eventos por categoría y/o rango de fechas.
     * Si no hay resultados, devuelve lista vacía (el controller maneja el mensaje).
     */
    @Transactional(readOnly = true)
    public List<EventResponse> filterEvents(String category, LocalDateTime dateFrom, LocalDateTime dateTo) {
        log.info("Filtrando eventos - categoría: {}, desde: {}, hasta: {}", category, dateFrom, dateTo);

        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), Event.EventStatus.ACTIVE));
            predicates.add(cb.greaterThan(root.get("availableTickets"), 0));

            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };  // <-- termina el Specification, el método continúa abajo

        List<Event> events = eventRepository.findAll(spec,
                Sort.by(Sort.Direction.ASC, "eventDate")
        );

        log.info("Se encontraron {} eventos con los filtros aplicados", events.size());

        return events.stream()
                .map(EventResponse::from)
                .toList();
    }

    /**
     * Obtiene todos los eventos activos disponibles (US-02).
     * Solo devuelve los campos: nombre, fecha y lugar.
     *
     * @return Lista de EventDTO con eventos disponibles ordenados por fecha
     */
    @Transactional(readOnly = true)
    public List<EventDTO> getAllEvents() {
        log.info("Obteniendo catálogo completo de eventos disponibles");

        List<Event> events = eventRepository
                .findByStatusAndAvailableTicketsGreaterThanOrderByEventDateAsc(
                        Event.EventStatus.ACTIVE, 0
                );

        return events.stream()
                .map(EventDTO::from)
                .toList();
    }

    /**
     * Publica un mensaje en el topic de Kafka "event.created".
     * Los microservicios suscritos (notification-service, etc.) lo recibirán.
     *
     * Si Kafka no está disponible, logueamos el error, pero NO lanzamos excepción
     * para no romper el flujo de creación del evento (el evento ya se guardó en BD).
     */

    /**
     * Modifica el precio de las boletas de un evento.
     *
     * Reglas de negocio implementadas:
     *  1. El evento debe existir → si no, lanza EventNotFoundException (HTTP 404)
     *  2. El evento no debe haber iniciado → si ya inició, lanza IllegalStateException (HTTP 409)
     *  3. El evento debe estar ACTIVE → cancelados o agotados no se modifican (HTTP 409)
     *  4. Si todo es válido → actualiza el precio y guarda en BD
     *
     * Criterio de aceptación 1
     *  "Dado que el evento no ha iniciado cuando el organizador cambia el precio
     *   entonces el sistema debe actualizar el valor."
     *
     * Criterio de aceptación 2
     *  "Dado que existen nuevas compras cuando se realiza el cambio
     *   entonces deben usar el nuevo precio."
     *  → Esto se cumple automáticamente porque el order-service consulta el precio
     *    vigente en el momento de crear la orden, no el precio histórico.
     *
     * @Transactional garantiza que si algo falla (ej: error de BD al guardar),
     * se hace rollback y el precio no queda a medias actualizado.
     *
     * @param eventId  UUID del evento cuyo precio se va a modificar
     * @param request  DTO con el nuevo precio validado
     * @return         EventResponse con todos los datos del evento ya actualizados
     * @throws EventNotFoundException  si el evento no existe
     * @throws IllegalStateException   si el evento ya inició o no está activo
     */
    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Evento no encontrado con ID: " + id));
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse updatePrice(UUID eventId, UpdatePriceRequest request) {
        log.info("Actualizando precio del evento {} a {}", eventId, request.getNewPrice());

        // 1. Buscar el evento en la BD.
        //    findById devuelve Optional<Event>.
        //    orElseThrow lanza EventNotFoundException si el Optional está vacío.
        //    El GlobalExceptionHandler convierte esa excepción en HTTP 404.
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(
                        "Evento no encontrado con ID: " + eventId));

        // 2. Verificar que el evento no ha iniciado.
        //    isBefore(now) → la fecha del evento es anterior al momento actual
        //    → el evento ya pasó o está en curso → no se puede modificar el precio
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "No se puede modificar el precio de un evento que ya ha iniciado");
        }

        // 3. Verificar que el evento está en estado ACTIVE.
        //    Un evento CANCELLED o SOLD_OUT no debería tener cambios de precio
        //    porque ya no acepta nuevas compras.
        if (event.getStatus() != Event.EventStatus.ACTIVE) {
            throw new IllegalStateException(
                    "No se puede modificar el precio de un evento cancelado o agotado");
        }

        // 4. Guardar el precio anterior solo para el log (auditoría básica).
        //    En producción esto iría a una tabla de historial de precios.
        BigDecimal precioAnterior = event.getPrice();

        // 5. Actualizar el precio en la entidad.
        //    Como la entidad está dentro de una transacción activa (@Transactional),
        //    JPA detecta el cambio automáticamente (dirty checking) y genera el UPDATE.
        event.setPrice(request.getNewPrice());

        // 6. Guardar explícitamente y obtener la entidad actualizada con updatedAt nuevo.
        Event updatedEvent = eventRepository.save(event);
        log.info("Precio del evento {} actualizado de {} a {}",
                eventId, precioAnterior, request.getNewPrice());

        // 7. Convertir la entidad a DTO de respuesta y devolver.
        //    El order-service consultará este precio actualizado en nuevas compras (criterio 2).
        return EventResponse.from(updatedEvent);
    }
    /**
     * Cancela un evento y notifica a los clientes vía Kafka.
     *
     * Reglas de negocio:
     *  1. El evento debe existir → si no, lanza EventNotFoundException (HTTP 404)
     *  2. El evento no debe estar ya cancelado → si lo está, lanza IllegalStateException (HTTP 409)
     *  3. Si todo es válido → marca como CANCELLED, guarda y publica en Kafka
     *
     * @Transactional garantiza que si falla el save, no se publica en Kafka
     * con un estado inconsistente.
     *
     * @param eventId UUID del evento a cancelar
     * @param request DTO con el motivo de cancelación (opcional)
     * @return        EventResponse con el evento ya marcado como CANCELLED
     * @throws EventNotFoundException  si el evento no existe
     * @throws IllegalStateException   si el evento ya está cancelado
     */
    @Transactional
    public EventResponse cancelEvent(UUID eventId, CancelEventRequest request) {
        log.info("Cancelando evento {} - motivo: {}", eventId,
                request != null ? request.getReason() : "sin motivo");

        // 1. Buscar el evento — lanza EventNotFoundException si no existe (HTTP 404)
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(
                        "Evento no encontrado con ID: " + eventId));

        // 2. Verificar que el evento no está ya cancelado
        //    No tiene sentido cancelar algo que ya está cancelado
        if (event.getStatus() == Event.EventStatus.CANCELLED) {
            throw new IllegalStateException(
                    "El evento ya está cancelado");
        }

        // 3. Marcar el evento como CANCELLED en la BD
        //    A partir de este momento no aparecerá en el catálogo
        //    ni permitirá nuevas compras
        event.setStatus(Event.EventStatus.CANCELLED);
        Event cancelledEvent = eventRepository.save(event);
        log.info("Evento {} marcado como CANCELLED en BD", eventId);

        // 4. Publicar en Kafka para que notification-service notifique a los clientes
        //    Si Kafka falla, logueamos el error, pero NO hacemos rollback del save
        //    porque el evento SÍ quedó cancelado en BD (lo importante)
        publishEventCancelled(cancelledEvent, request);

        return EventResponse.from(cancelledEvent);
    }

    /**
     * Publica un mensaje en el topic "event.cancelled" de Kafka.
     *
     * El notification-service escucha este topic y envía un correo
     * a cada cliente que compró boleta para este evento (criterio 2 US-12).
     *
     * Si Kafka no está disponible, logueamos el error, pero NO lanzamos excepción
     * para no deshacer la cancelación que ya quedó guardada en BD.
     *
     * @param event   entidad del evento ya cancelado
     * @param request DTO con el motivo, puede ser null
     */
    private void publishEventCancelled(Event event, CancelEventRequest request) {
        try {
            java.util.Map<String, Object> message = new java.util.HashMap<>();
            message.put("eventId", event.getId());
            message.put("eventName", event.getName());
            message.put("eventDate", event.getEventDate());
            message.put("venue", event.getVenue());
            message.put("organizerId", event.getOrganizerId());
            message.put("reason", request != null && request.getReason() != null
                    ? request.getReason()
                    : "El organizador ha cancelado el evento");

            kafkaTemplate.send(eventCancelledTopic, event.getId().toString(), message);
            log.info("Publicado en Kafka topic '{}' cancelación del evento {}",
                    eventCancelledTopic, event.getId());

        } catch (Exception e) {
            log.error("Error al publicar cancelación en Kafka para evento {}: {}",
                    event.getId(), e.getMessage());
        }
    }
    private void publishEventCreated(Event event) {
        try {
            // El mensaje que enviamos a Kafka es el EventResponse (serializado a JSON)
            EventResponse message = EventResponse.from(event);

            // send(topic, key, value)
            // key = ID del evento → permite particionar mensajes del mismo evento juntos
            kafkaTemplate.send(eventCreatedTopic, event.getId().toString(), message);

            log.info("Mensaje publicado en Kafka topic '{}' para evento {}", eventCreatedTopic, event.getId());
        } catch (Exception e) {
            log.error("Error al publicar en Kafka el evento {}: {}", event.getId(), e.getMessage());
        }
    }
}