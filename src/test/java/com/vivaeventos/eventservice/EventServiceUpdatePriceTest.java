package com.vivaeventos.eventservice;

import com.vivaeventos.eventservice.dto.UpdatePriceRequest;
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
 * Pruebas unitarias para el método updatePrice del EventService.
 *
 * @ExtendWith(MockitoExtension.class) → Activa Mockito para crear mocks automáticamente.
 *
 * Estrategia de pruebas:
 * - Se simulan (mockean) el repository y Kafka para no necesitar BD ni broker real.
 * - Cada test verifica un caso específico del criterio de aceptación.
 * - Los nombres de los tests siguen el patrón: dado_cuando_entonces (BDD).
 */
@ExtendWith(MockitoExtension.class)
class EventServiceUpdatePriceTest {

    /**
     * @Mock → Crea un objeto simulado del repository.
     * No ejecuta SQL real — solo devuelve lo que le digamos con when().
     */
    @Mock
    private EventRepository eventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * @InjectMocks → Crea una instancia real de EventService
     * e inyecta los @Mock anteriores en su constructor.
     */
    @InjectMocks
    private EventService eventService;

    // Datos de prueba reutilizables entre tests
    private Event eventoActivo;
    private UUID eventoId;

    /**
     * @BeforeEach → Se ejecuta antes de CADA test.
     * Prepara un evento de prueba con fecha futura (no ha iniciado).
     */
    @BeforeEach
    void setUp() {
        eventoId = UUID.randomUUID();

        eventoActivo = new Event();
        eventoActivo.setName("Concierto Rock");
        eventoActivo.setCategory("Música");
        eventoActivo.setVenue("Parque Simón Bolívar");
        // Fecha futura → el evento NO ha iniciado → precio modificable
        eventoActivo.setEventDate(LocalDateTime.now().plusDays(30));
        eventoActivo.setCapacity(5000);
        eventoActivo.setAvailableTickets(5000);
        eventoActivo.setPrice(BigDecimal.valueOf(85000));
        eventoActivo.setOrganizerId(UUID.randomUUID());
        eventoActivo.setStatus(Event.EventStatus.ACTIVE);
    }

    /**
     * El evento no ha iniciado → precio actualizado exitosamente.
     *
     * Escenario: evento futuro ACTIVE, precio nuevo válido.
     * Resultado esperado: HTTP 200, precio actualizado en la respuesta.
     */
    @Test
    void dadoEventoFuturoActivo_cuandoSeActualizaPrecio_entoncesRetornaPrecioNuevo() {
        // GIVEN — configurar qué devuelve el mock
        // findById devuelve el evento activo
        when(eventRepository.findById(eventoId))
                .thenReturn(Optional.of(eventoActivo));
        // save devuelve el evento con el precio ya actualizado
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePriceRequest request = new UpdatePriceRequest(BigDecimal.valueOf(95000));

        // WHEN — ejecutar el método bajo prueba
        EventResponse response = eventService.updatePrice(eventoId, request);

        // THEN — verificar el resultado
        assertThat(response).isNotNull();
        assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(95000));
    }

    /**
     * El evento ya inició → no se puede modificar el precio.
     *
     * Escenario: evento con fecha en el pasado.
     * Resultado esperado: IllegalStateException con mensaje descriptivo.
     */
    @Test
    void dadoEventoPasado_cuandoSeActualizaPrecio_entoncesLanzaExcepcion() {
        // GIVEN — evento que ya pasó
        eventoActivo.setEventDate(LocalDateTime.now().minusDays(1));
        when(eventRepository.findById(eventoId))
                .thenReturn(Optional.of(eventoActivo));

        UpdatePriceRequest request = new UpdatePriceRequest(BigDecimal.valueOf(95000));

        // WHEN & THEN — verificar que lanza la excepción correcta
        assertThatThrownBy(() -> eventService.updatePrice(eventoId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya ha iniciado");
    }

    /**
     * Caso: el evento no existe en la BD.
     *
     * Resultado esperado: EventNotFoundException con mensaje descriptivo.
     */
    @Test
    void dadoEventoInexistente_cuandoSeActualizaPrecio_entoncesLanzaEventNotFoundException() {
        // GIVEN — el repositorio no encuentra el evento
        when(eventRepository.findById(eventoId))
                .thenReturn(Optional.empty());

        UpdatePriceRequest request = new UpdatePriceRequest(BigDecimal.valueOf(95000));

        // WHEN & THEN
        assertThatThrownBy(() -> eventService.updatePrice(eventoId, request))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("Evento no encontrado");
    }

    /**
     * Caso: el evento está CANCELADO → no se puede modificar el precio.
     *
     * Resultado esperado: IllegalStateException.
     */
    @Test
    void dadoEventoCancelado_cuandoSeActualizaPrecio_entoncesLanzaExcepcion() {
        // GIVEN — evento cancelado con fecha futura
        eventoActivo.setStatus(Event.EventStatus.CANCELLED);
        when(eventRepository.findById(eventoId))
                .thenReturn(Optional.of(eventoActivo));

        UpdatePriceRequest request = new UpdatePriceRequest(BigDecimal.valueOf(95000));

        // WHEN & THEN
        assertThatThrownBy(() -> eventService.updatePrice(eventoId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelado");
    }
}