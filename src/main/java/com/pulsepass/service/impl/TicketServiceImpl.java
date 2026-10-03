package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.User;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final TicketMapper mapper;

    public TicketServiceImpl(UserRepository userRepository,
                              EventRepository eventRepository,
                              TicketRepository ticketRepository,
                              TicketMapper mapper) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        // BR-TICKET-001: usuario debe existir
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + request.userEmail()));

        // BR-TICKET-002: usuario debe estar activo
        if (!user.isActive()) {
            throw new BusinessRuleException(
                    "User is not active: " + request.userEmail());
        }

        // BR-TICKET-003: evento debe existir
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + request.eventCode()));

        // BR-TICKET-004: evento debe estar PUBLISHED
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Cannot purchase ticket for event with status " + event.getStatus());
        }

        // BR-TICKET-005: el evento no puede haber pasado ya
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Cannot purchase ticket for an event that already occurred");
        }

        // BR-TICKET-006: edad mínima, si aplica
        if (event.getMinimumAge() != null && event.getMinimumAge() > 0) {
            if (user.getProfile() == null || user.getProfile().getBirthDate() == null) {
                throw new BusinessRuleException(
                        "User does not meet minimum age: birth date unknown");
            }
            int ageAtEvent = Period.between(
                    user.getProfile().getBirthDate(),
                    event.getEventDate().toLocalDate()
            ).getYears();

            if (ageAtEvent < event.getMinimumAge()) {
                throw new BusinessRuleException(
                        "User does not meet minimum age of " + event.getMinimumAge());
            }
        }

        // BR-TICKET-007: capacidad disponible
        long paidTickets = ticketRepository.countByEventEventCodeAndStatus(
                event.getEventCode(), TicketStatus.PAID);

        if (paidTickets >= event.getVenue().getCapacity()) {
            throw new BusinessRuleException(
                    "No capacity available for event: " + event.getEventCode());
        }

        // Calcular precio (encapsulado en TicketPricingPolicy)
        BigDecimal price = TicketPricingPolicy.calculatePrice(request.type());

        // Crear Ticket (BR-TICKET-009 cumplida por diseño: price nunca es negativo)
        String ticketCode = "TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Ticket ticket = new Ticket(
                ticketCode,
                request.type(),
                price,
                TicketStatus.PAID,
                LocalDateTime.now(),
                user,
                event
        );

        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: si se completó la capacidad, pasar a SOLD_OUT
        if (paidTickets + 1 == event.getVenue().getCapacity()) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return mapper.toResponse(saved);
    }

    @Override
    public TicketResponse findByCode(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + ticketCode));
    }

    @Override
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findByEventEventCodeAndStatus(eventCode, TicketStatus.PAID)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + ticketCode));

        // BR-TICKET-010 y 011: solo PAID puede cancelarse
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Cannot cancel ticket with status " + ticket.getStatus());
        }

        // BR-TICKET-012: no se puede cancelar después de la fecha del evento
        if (!ticket.getEvent().getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Cannot cancel ticket after the event has occurred");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket saved = ticketRepository.save(ticket);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + ticketCode));

        // BR-TICKET-013 y 014: solo PAID puede marcarse como usado
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Cannot mark ticket as used with status " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.USED);
        Ticket saved = ticketRepository.save(ticket);

        return mapper.toResponse(saved);
    }
}