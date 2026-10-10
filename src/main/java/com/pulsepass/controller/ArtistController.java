package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.service.ArtistService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FR-CTRL-ART-001 .. FR-CTRL-ART-003.
 */
@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    // GET /api/artists/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> findById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(artistService.findById(id));
    }

    // GET /api/artists/by-stage-name?stageName=...
    @GetMapping("/by-stage-name")
    public ResponseEntity<ArtistResponse> findByStageName(
            @RequestParam("stageName") String stageName) {
        return ResponseEntity.ok(artistService.findByStageName(stageName));
    }

    // GET /api/artists/active
    @GetMapping("/active")
    public ResponseEntity<List<ArtistResponse>> findActiveArtists() {
        return ResponseEntity.ok(artistService.findActiveArtists());
    }
}
