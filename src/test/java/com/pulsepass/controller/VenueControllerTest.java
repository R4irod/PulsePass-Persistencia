package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.VenueService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.Import;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de VenueController. @WebMvcTest solo levanta la capa web
 * (controllers + GlobalExceptionHandler); VenueService es un mock.
 */
@WebMvcTest(VenueController.class)
@Import(ControllerTestSecurityConfig.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    private VenueResponse marinaVenue() {
        return new VenueResponse(
                1L, "VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Av. del Mar", 3, true);
    }

    // TEST-CTRL-VEN-001 — venue existente -> 200
    @Test
    void shouldReturn200WhenVenueExists() throws Exception {
        // ARRANGE
        when(venueService.findByCode("VEN-SMR-01")).thenReturn(marinaVenue());

        // ACT + ASSERT
        mockMvc.perform(get("/api/venues/VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"))
                .andExpect(jsonPath("$.city").value("Santa Marta"))
                .andExpect(jsonPath("$.capacity").value(3))
                .andExpect(jsonPath("$.active").value(true));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    // TEST-CTRL-VEN-002 — venue inexistente -> 404 con ErrorResponse
    @Test
    void shouldReturn404WhenVenueDoesNotExist() throws Exception {
        when(venueService.findByCode("VEN-XXX"))
                .thenThrow(new ResourceNotFoundException("Venue not found: VEN-XXX"));

        mockMvc.perform(get("/api/venues/VEN-XXX"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: VEN-XXX"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").isEmpty());

        verify(venueService).findByCode("VEN-XXX");
    }

    // TEST-CTRL-VEN-003 — venues activos -> 200
    @Test
    void shouldReturn200WithActiveVenues() throws Exception {
        when(venueService.findActiveVenues()).thenReturn(List.of(
                marinaVenue(),
                new VenueResponse(2L, "VEN-BOG-01", "Movistar Arena", "Bogota", "Cra 30", 14000, true)));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$[1].code").value("VEN-BOG-01"));

        verify(venueService).findActiveVenues();
    }

    // QT-CTRL-011 / NFR-CTRL-007 — error inesperado -> 500 sin filtrar detalles internos
    @Test
    void shouldReturn500WithoutLeakingInternalDetailsOnUnexpectedError() throws Exception {
        when(venueService.findByCode("VEN-SMR-01"))
                .thenThrow(new IllegalStateException("connection pool exhausted at host db-internal-01"));

        mockMvc.perform(get("/api/venues/VEN-SMR-01"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }
}
