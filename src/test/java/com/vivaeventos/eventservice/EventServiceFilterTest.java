package com.vivaeventos.eventservice;

import com.vivaeventos.eventservice.dto.EventResponse;
import com.vivaeventos.eventservice.model.Event;
import com.vivaeventos.eventservice.repository.EventRepository;
import com.vivaeventos.eventservice.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para filterEvents del EventService.
 *
 * Dado que tus compañeros migraron el filtrado a JPA Specifications,
 * los mocks ahora interceptan eventRepository.findAll(spec, sort)
 * en lugar del método findByFilters anterior.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceFilterTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EventService eventService;

    private Event eventoActivo;

    @BeforeEach
    void setUp() {
        eventoActivo = new Event();
        eventoActivo.setName("Concierto Rock");
        eventoActivo.setCategory("Música");
        eventoActivo.setVenue("Parque Simón Bolívar");
        eventoActivo.setEventDate(LocalDateTime.of(2025, 8, 15, 18, 0));
        eventoActivo.setCapacity(5000);
        eventoActivo.setAvailableTickets(5000);
        eventoActivo.setPrice(BigDecimal.valueOf(85000));
        eventoActivo.setOrganizerId(UUID.randomUUID());
        eventoActivo.setStatus(Event.EventStatus.ACTIVE);
    }

    /**
     * Criterio 1: existen eventos → devuelve lista con eventos correspondientes.
     */
    @Test
    void dadoQueExistenEventos_cuandoSeFiltraPorCategoria_entoncesDevuelveLista() {
        when(eventRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(eventoActivo));

        List<EventResponse> resultado = eventService.filterEvents("Música", null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getName()).isEqualTo("Concierto Rock");
        assertThat(resultado.get(0).getCategory()).isEqualTo("Música");
    }

    /**
     * Criterio 1: filtrar por rango de fechas devuelve eventos correctos.
     */
    @Test
    void dadoQueExistenEventos_cuandoSeFiltraPorFecha_entoncesDevuelveLista() {
        when(eventRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(eventoActivo));

        LocalDateTime desde = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2025, 8, 31, 23, 59);

        List<EventResponse> resultado = eventService.filterEvents(null, desde, hasta);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getName()).isEqualTo("Concierto Rock");
    }

    /**
     * Criterio 2: no existen eventos para el filtro → devuelve lista vacía.
     */
    @Test
    void dadoQueNoExistenEventos_cuandoSeAplicaFiltro_entoncesDevuelveListaVacia() {
        when(eventRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        List<EventResponse> resultado = eventService.filterEvents("Teatro", null, null);

        assertThat(resultado).isEmpty();
    }

    /**
     * Criterio 1: filtrar por categoría Y fecha combina ambos filtros.
     */
    @Test
    void dadoQueExistenEventos_cuandoSeFiltraPorCategoriaYFecha_entoncesDevuelveEventosCorrectos() {
        when(eventRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(eventoActivo));

        LocalDateTime desde = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2025, 8, 31, 23, 59);

        List<EventResponse> resultado = eventService.filterEvents("Música", desde, hasta);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCategory()).isEqualTo("Música");
    }
}