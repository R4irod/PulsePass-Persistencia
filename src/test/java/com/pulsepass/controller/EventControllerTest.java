package com.pulsepass.controller;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.EventService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private static final String VALID_CREATE_JSON = """
            {
              "eventCode": "CMF-2026",
              "name": "Caribbean Music Fest 2026",
              "description": "Festival de musica del Caribe",
              "category": "MUSIC",
              "eventDate": "2030-12-15T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    private EventResponse cmf2026(EventStatus status, List<String> artists) {
        return new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026", "Festival de musica del Caribe",
                EventCategory.MUSIC, status, LocalDateTime.of(2030, 12, 15, 20, 0),
                18, "VEN-SMR-01", "Marina Convention Center", artists);
    }

    // TEST-CTRL-EVT-001 — crear válido -> 201
    @Test
    void shouldReturn201WhenCreatingValidEvent() throws Exception {
        // ARRANGE
        when(eventService.create(any(CreateEventRequest.class)))
                .thenReturn(cmf2026(EventStatus.DRAFT, List.of()));

        // ACT + ASSERT
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"));

        // El JSON se deserializó correctamente hacia el request que recibe el Service.
        ArgumentCaptor<CreateEventRequest> captor = ArgumentCaptor.forClass(CreateEventRequest.class);
        verify(eventService).create(captor.capture());
        assertThat(captor.getValue().eventCode()).isEqualTo("CMF-2026");
        assertThat(captor.getValue().category()).isEqualTo(EventCategory.MUSIC);
        assertThat(captor.getValue().minimumAge()).isEqualTo(18);
        assertThat(captor.getValue().venueCode()).isEqualTo("VEN-SMR-01");
    }

    // TEST-CTRL-EVT-002 — request inválido -> 400 + details; el Service NO se invoca
    @Test
    void shouldReturn400AndNotCallServiceWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.name").value("Name is required"))
                .andExpect(jsonPath("$.details.category").value("Category is required"))
                .andExpect(jsonPath("$.details.eventDate").value("Event date is required"))
                .andExpect(jsonPath("$.details.minimumAge").value("Minimum age is required"))
                .andExpect(jsonPath("$.details.venueCode").value("Venue code is required"));

        verify(eventService, never()).create(any());
    }

    @Test
    void shouldReturn400WhenMinimumAgeIsNegative() throws Exception {
        String json = VALID_CREATE_JSON.replace("\"minimumAge\": 18", "\"minimumAge\": -1");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.minimumAge").value("Minimum age must be 0 or greater"));

        verify(eventService, never()).create(any());
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"eventCode\": \"CMF-2026\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));

        verify(eventService, never()).create(any());
    }

    // Errores de negocio al crear (venue inactivo, fecha pasada...) -> 409
    @Test
    void shouldReturn409WhenCreateViolatesBusinessRule() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new BusinessRuleException("Cannot create an event in an inactive venue"));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot create an event in an inactive venue"));
    }

    // TEST-CTRL-EVT-003 — consultar existente -> 200
    @Test
    void shouldReturn200WhenEventExists() throws Exception {
        when(eventService.findByCode("CMF-2026"))
                .thenReturn(cmf2026(EventStatus.PUBLISHED, List.of("Solar Beat")));

        mockMvc.perform(get("/api/events/CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.venueName").value("Marina Convention Center"))
                .andExpect(jsonPath("$.artists[0]").value("Solar Beat"));

        verify(eventService).findByCode("CMF-2026");
    }

    // TEST-CTRL-EVT-004 — inexistente -> 404
    @Test
    void shouldReturn404WhenEventDoesNotExist() throws Exception {
        when(eventService.findByCode("CMF-9999"))
                .thenThrow(new ResourceNotFoundException("Event not found: CMF-9999"));

        mockMvc.perform(get("/api/events/CMF-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event not found: CMF-9999"));
    }

    // TEST-CTRL-EVT-005 — listar publicados -> 200
    @Test
    void shouldReturn200WithPublishedEvents() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of(
                new EventSummaryResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                        EventCategory.MUSIC, EventStatus.PUBLISHED,
                        LocalDateTime.of(2030, 12, 15, 20, 0), "Marina Convention Center"),
                new EventSummaryResponse(2L, "TECH-2030", "Tech Summit",
                        EventCategory.TECHNOLOGY, EventStatus.PUBLISHED,
                        LocalDateTime.of(2031, 3, 1, 9, 0), "Marina Convention Center")));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findPublishedEvents();
    }

    // TEST-CTRL-EVT-006 — publicar válido -> 200
    @Test
    void shouldReturn200WhenPublishingValidEvent() throws Exception {
        when(eventService.publish("CMF-2026")).thenReturn(cmf2026(EventStatus.PUBLISHED, List.of()));

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    // TEST-CTRL-EVT-007 — publicar inválido (regla de negocio) -> 409
    @Test
    void shouldReturn409WhenPublishViolatesBusinessRule() throws Exception {
        when(eventService.publish("CMF-2026"))
                .thenThrow(new BusinessRuleException("Only DRAFT events can be published"));

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only DRAFT events can be published"));
    }

    // TEST-CTRL-EVT-008 — asociar artista -> 200
    @Test
    void shouldReturn200WhenAddingArtistToEvent() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L))
                .thenReturn(cmf2026(EventStatus.DRAFT, List.of("Solar Beat")));

        mockMvc.perform(post("/api/events/CMF-2026/artists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.artists", hasSize(1)))
                .andExpect(jsonPath("$.artists[0]").value("Solar Beat"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    // Asociar artista duplicado (BR-EVENT-010) -> 409
    @Test
    void shouldReturn409WhenArtistIsAlreadyAssociated() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L))
                .thenThrow(new BusinessRuleException("Artist is already associated with this event"));

        mockMvc.perform(post("/api/events/CMF-2026/artists/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Artist is already associated with this event"));
    }

    // artistId no numérico -> 400 y el Service nunca se invoca
    @Test
    void shouldReturn400WhenArtistIdIsNotNumeric() throws Exception {
        mockMvc.perform(post("/api/events/CMF-2026/artists/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(eventService, never()).addArtist(any(), any());
    }

    // TEST-CTRL-EVT-009 — buscar por artista -> 200
    @Test
    void shouldReturn200WhenSearchingEventsByArtist() throws Exception {
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(
                new EventSummaryResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                        EventCategory.MUSIC, EventStatus.PUBLISHED,
                        LocalDateTime.of(2030, 12, 15, 20, 0), "Marina Convention Center")));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }

    @Test
    void shouldReturn400WhenStageNameParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/events/by-artist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.stageName").exists());

        verify(eventService, never()).findByArtist(any());
    }
}
