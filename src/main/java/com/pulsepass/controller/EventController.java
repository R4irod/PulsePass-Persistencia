package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.service.EventService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FR-CTRL-EVT-001 .. FR-CTRL-EVT-006.
 * La validación estructural la hace @Valid; las reglas de negocio
 * (venue activo, fecha futura, transición DRAFT -> PUBLISHED...) las aplica EventService.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // POST /api/events  -> 201 Created
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    // GET /api/events/{eventCode}
    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(@PathVariable("eventCode") String eventCode) {
        return ResponseEntity.ok(eventService.findByCode(eventCode));
    }

    // GET /api/events/published
    @GetMapping("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublishedEvents() {
        return ResponseEntity.ok(eventService.findPublishedEvents());
    }

    // PATCH /api/events/{eventCode}/publish
    @PatchMapping("/{eventCode}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable("eventCode") String eventCode) {
        return ResponseEntity.ok(eventService.publish(eventCode));
    }

    // POST /api/events/{eventCode}/artists/{artistId}  -> 200 OK (según el contrato del PRD)
    @PostMapping("/{eventCode}/artists/{artistId}")
    public ResponseEntity<EventResponse> addArtist(
            @PathVariable("eventCode") String eventCode,
            @PathVariable("artistId") Long artistId) {
        return ResponseEntity.ok(eventService.addArtist(eventCode, artistId));
    }

    // GET /api/events/by-artist?stageName=...
    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(
            @RequestParam("stageName") String stageName) {
        return ResponseEntity.ok(eventService.findByArtist(stageName));
    }
}
