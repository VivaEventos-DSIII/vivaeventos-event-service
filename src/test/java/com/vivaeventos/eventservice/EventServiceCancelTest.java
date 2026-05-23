package com.vivaeventos.eventservice;

import com.vivaeventos.eventservice.dto.CancelEventRequest;
import com.vivaeventos.eventservice.dto.EventResponse;
import com.vivaeventos.eventservice.exception.EventNotFoundException;
import com.vivaeventos.eventservice.model.Event;
import com.vivaeventos.eventservice.repository.EventRepository;
import com.vivaeventos.eventservice.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para cancelEvent del EventService.
 *
 * Estrategia:
 * - Se mockean repository y Kafka para no necesitar BD ni broker real.
 * - Nombres en formato dado_cuando_entonces (BDD).
 */
@ExtendWith(MockitoExtension.class)
class EventServiceCancelTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EventService eventService;

    private Event eventoActivo;
    private UUID eventoId;

    /**
     * Prepara un evento ACTIVE de prueba antes de cada test.
     */
    @BeforeEach
    void setUp() {
        eventoId = UUID.randomUUID();

        eventoActivo = new Event();
        eventoActivo.setName("Concierto Rock");
        eventoActivo.setCategory("Música");
        eventoActivo.setVenue("Parque Simón Bolívar");
        eventoActivo.setEventDate(LocalDateTime.now().plusDays(30));
        eventoActivo.setCapacity(5000);
        eventoActivo.setAvailableTickets(5000);
        eventoActivo.setPrice(BigDecimal.valueOf(85000));
        eventoActivo.setOrganizerId(UUID.randomUUID());
        eventoActivo.setStatus(Event.EventStatus.ACTIVE);
    }

    /**
     * El organizador cancela el evento →
     * el sistema debe marcarlo como CANCELLED.
     */
    @Test
    void dadoEventoActivo_cuandoSeCancela_entoncesRetornaCancelado() {
        // GIVEN
        when(eventRepository.findById(eventoId))
                .thenReturn(Optional.of(eventoActivo));
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CancelEventRequest request = new CancelEventRequest("Problemas de seguridad");

        // WHEN
        EventResponse response = eventService.cancelEvent(eventoId, request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    /**
     * Cancelación sin motivo también funciona.
     * El motivo es opcional.
     */
    @Test
    void dadoEventoActivo_cuandoSeCancelaSinMotivo_entoncesRetornaCancelado() {
        // GIVEN
        when(eventRepository.findById(eventoId))
                .thenReturn(Optional.of(eventoActivo));
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN — request null simula que el organizador no envió body
        EventResponse response = eventService.cancelEvent(eventoId, null);

        // THEN
        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    /**
     * Caso: el evento no existe → lanza EventNotFoundException (HTTP 404).
     */
    @Test
    void dadoEventoInexistente_cuandoSeCancela_entoncesLanzaEventNotFoundException() {
        // GIVEN
        when(eventRepository.findById(eventoId))
                .thenReturn(Optional.empty());

        CancelEventRequest request = new CancelEventRequest("Motivo cualquiera");

        // WHEN & THEN
        assertThatThrownBy(() -> eventService.cancelEvent(eventoId, request))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("Evento no encontrado");
    }

    /**
     * Caso: el evento ya está cancelado → lanza IllegalStateException (HTTP 409).
     * No tiene sentido cancelar algo que ya está cancelado.
     */
    @Test
    void dadoEventoYaCancelado_cuandoSeCancela_entoncesLanzaExcepcion() {
        // GIVEN — evento ya en estado CANCELLED
        eventoActivo.setStatus(Event.EventStatus.CANCELLED);
        when(eventRepository.findById(eventoId))
                .thenReturn(Optional.of(eventoActivo));

        CancelEventRequest request = new CancelEventRequest("Intento duplicado");

        // WHEN & THEN
        assertThatThrownBy(() -> eventService.cancelEvent(eventoId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya está cancelado");
    }
}

