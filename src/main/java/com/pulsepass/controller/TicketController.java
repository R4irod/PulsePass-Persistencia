package com.pulsepass.controller;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.service.TicketService;

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
 * FR-CTRL-TKT-001 .. FR-CTRL-TKT-006.
 *
 * Decisión de diseño: el endpoint GET /api/events/{eventCode}/tickets/paid vive aquí y no en
 * EventController porque invoca TicketService. Así EventController depende solo de EventService
 * (NFR-CTRL-004) y su test queda en TicketControllerTest (TEST-CTRL-TKT-007), como pide el PRD.
 * Por eso el prefijo de clase es "/api" y cada método declara su propio segmento.
 */
@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // POST /api/tickets  -> 201 Created
    @PostMapping("/tickets")
    public ResponseEntity<TicketResponse> purchase(@Valid @RequestBody PurchaseTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.purchase(request));
    }

    // GET /api/tickets/{ticketCode}
    @GetMapping("/tickets/{ticketCode}")
    public ResponseEntity<TicketResponse> findByCode(@PathVariable("ticketCode") String ticketCode) {
        return ResponseEntity.ok(ticketService.findByCode(ticketCode));
    }

    // GET /api/tickets/by-user?email=...
    @GetMapping("/tickets/by-user")
    public ResponseEntity<List<TicketResponse>> findByUserEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(ticketService.findByUserEmail(email));
    }

    // GET /api/events/{eventCode}/tickets/paid
    @GetMapping("/events/{eventCode}/tickets/paid")
    public ResponseEntity<List<TicketResponse>> findPaidTicketsByEvent(
            @PathVariable("eventCode") String eventCode) {
        return ResponseEntity.ok(ticketService.findPaidTicketsByEvent(eventCode));
    }

    // PATCH /api/tickets/{ticketCode}/cancel
    @PatchMapping("/tickets/{ticketCode}/cancel")
    public ResponseEntity<TicketResponse> cancel(@PathVariable("ticketCode") String ticketCode) {
        return ResponseEntity.ok(ticketService.cancel(ticketCode));
    }

    // PATCH /api/tickets/{ticketCode}/use
    @PatchMapping("/tickets/{ticketCode}/use")
    public ResponseEntity<TicketResponse> markAsUsed(@PathVariable("ticketCode") String ticketCode) {
        return ResponseEntity.ok(ticketService.markAsUsed(ticketCode));
    }
}
