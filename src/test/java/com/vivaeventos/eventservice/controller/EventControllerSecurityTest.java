package com.vivaeventos.eventservice.controller;

import com.vivaeventos.eventservice.security.SecurityConfig;
import com.vivaeventos.eventservice.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
class EventControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    private static final UUID EVENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private static final String VALID_CREATE_BODY = """
            {
              "name": "Concierto Rock",
              "description": "Festival de rock al aire libre",
              "category": "CONCIERTO",
              "venue": "Parque Simón Bolívar",
              "eventDate": "2030-08-15T18:00:00",
              "capacity": 5000,
              "price": 85000.00,
              "organizerId": "550e8400-e29b-41d4-a716-446655440000"
            }
            """;

    private static final String VALID_PRICE_BODY = """
            { "newPrice": 90000.00 }
            """;

    // ─────────────────────────────────────────────
    // POST /events — crear evento
    // ─────────────────────────────────────────────

    @Test
    void dadoSinAutenticacion_cuandoCrearEvento_entoncesRetorna401() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dadoRoleUser_cuandoCrearEvento_entoncesRetorna403() throws Exception {
        mockMvc.perform(post("/events")
                        .header("X-User-Role", "ROLE_USER")
                        .header("X-User-Email", "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void dadoRoleAdmin_cuandoCrearEvento_entoncesRetorna201() throws Exception {
        when(eventService.createEvent(any())).thenReturn(null);

        mockMvc.perform(post("/events")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .header("X-User-Email", "admin@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isCreated());
    }

    // ─────────────────────────────────────────────
    // PATCH /events/{id}/price — modificar precio
    // ─────────────────────────────────────────────

    @Test
    void dadoSinAutenticacion_cuandoActualizarPrecio_entoncesRetorna401() throws Exception {
        mockMvc.perform(patch("/events/{id}/price", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRICE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dadoRoleUser_cuandoActualizarPrecio_entoncesRetorna403() throws Exception {
        mockMvc.perform(patch("/events/{id}/price", EVENT_ID)
                        .header("X-User-Role", "ROLE_USER")
                        .header("X-User-Email", "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRICE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void dadoRoleAdmin_cuandoActualizarPrecio_entoncesRetorna200() throws Exception {
        when(eventService.updatePrice(any(), any())).thenReturn(null);

        mockMvc.perform(patch("/events/{id}/price", EVENT_ID)
                        .header("X-User-Role", "ROLE_ADMIN")
                        .header("X-User-Email", "admin@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRICE_BODY))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────
    // DELETE /events/{id} — cancelar evento
    // ─────────────────────────────────────────────

    @Test
    void dadoSinAutenticacion_cuandoCancelarEvento_entoncesRetorna401() throws Exception {
        mockMvc.perform(delete("/events/{id}", EVENT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dadoRoleUser_cuandoCancelarEvento_entoncesRetorna403() throws Exception {
        mockMvc.perform(delete("/events/{id}", EVENT_ID)
                        .header("X-User-Role", "ROLE_USER")
                        .header("X-User-Email", "user@test.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void dadoRoleAdmin_cuandoCancelarEvento_entoncesRetorna200() throws Exception {
        when(eventService.cancelEvent(any(), any())).thenReturn(null);

        mockMvc.perform(delete("/events/{id}", EVENT_ID)
                        .header("X-User-Role", "ROLE_ADMIN")
                        .header("X-User-Email", "admin@test.com"))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────
    // GET /events/catalog — cualquier usuario autenticado puede leer
    // ─────────────────────────────────────────────

    @Test
    void dadoSinAutenticacion_cuandoLeerCatalogo_entoncesRetorna401() throws Exception {
        mockMvc.perform(get("/events/catalog"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dadoRoleUser_cuandoLeerCatalogo_entoncesRetorna200() throws Exception {
        when(eventService.getAllEvents()).thenReturn(List.of());

        mockMvc.perform(get("/events/catalog")
                        .header("X-User-Role", "ROLE_USER")
                        .header("X-User-Email", "user@test.com"))
                .andExpect(status().isOk());
    }
}
