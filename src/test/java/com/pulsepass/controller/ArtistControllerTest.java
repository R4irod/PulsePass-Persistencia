package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.Import;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
@Import(ControllerTestSecurityConfig.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    private ArtistResponse solarBeat() {
        return new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);
    }

    // TEST-CTRL-ART-001 — buscar por ID -> 200
    @Test
    void shouldReturn200WhenArtistExistsById() throws Exception {
        when(artistService.findById(1L)).thenReturn(solarBeat());

        mockMvc.perform(get("/api/artists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.active").value(true));

        verify(artistService).findById(1L);
    }

    // TEST-CTRL-ART-002 — ID inexistente -> 404
    @Test
    void shouldReturn404WhenArtistIdDoesNotExist() throws Exception {
        when(artistService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Artist not found: 999"));

        mockMvc.perform(get("/api/artists/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Artist not found: 999"));
    }

    // ID no numérico -> 400 y el Service nunca se invoca
    @Test
    void shouldReturn400WhenArtistIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/artists/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(artistService);
    }

    // TEST-CTRL-ART-003 — buscar por stageName -> 200
    @Test
    void shouldReturn200WhenSearchingByStageName() throws Exception {
        when(artistService.findByStageName("Solar Beat")).thenReturn(solarBeat());

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    void shouldReturn404WhenStageNameDoesNotExist() throws Exception {
        when(artistService.findByStageName("Nobody"))
                .thenThrow(new ResourceNotFoundException("Artist not found: Nobody"));

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Nobody"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Artist not found: Nobody"));
    }

    // TEST-CTRL-ART-004 — listar activos -> 200
    @Test
    void shouldReturn200WithActiveArtists() throws Exception {
        when(artistService.findActiveArtists()).thenReturn(List.of(
                solarBeat(),
                new ArtistResponse(2L, "Neon Waves", "Mexico", "Synth Pop", true),
                new ArtistResponse(3L, "Caribbean Sound", "Colombia", "Tropical", true)));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[1].stageName").value("Neon Waves"));

        verify(artistService).findActiveArtists();
    }
}
