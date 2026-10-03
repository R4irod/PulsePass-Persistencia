package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.impl.TicketServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketMapper mapper;

    @InjectMocks
    private TicketServiceImpl service;

    private User activeUser(LocalDate birthDate) {
        User user = new User("andrea", "andrea@email.com");
        UserProfile profile = new UserProfile("Andrea", "Gomez", "300", "Santa Marta", birthDate);
        user.assignProfile(profile);
        return user;
    }

    private Venue venueWithCapacity(int capacity) {
        return new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1 #2-3", capacity);
    }

    private Event publishedEvent(Venue venue, int minimumAge) {
        Event event = new Event("CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusMonths(1), minimumAge);
        venue.addEvent(event);
        return event;
    }

    private PurchaseTicketRequest request() {
        return new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.GENERAL);
    }

    // ---------- TEST-TICKET-001: compra válida -> PAID ----------
    @Test
    void shouldPurchaseTicketWhenRequestIsValid() {
        User user = activeUser(LocalDate.now().minusYears(25));
        Venue venue = venueWithCapacity(3);
        Event event = publishedEvent(venue, 18);

        TicketResponse response = new TicketResponse(1L, "TCK-ABC", TicketType.GENERAL,
                new BigDecimal("100.00"), TicketStatus.PAID, LocalDateTime.now(),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest");

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(java.util.Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(java.util.Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Ticket.class))).thenReturn(response);

        TicketResponse result = service.purchase(request());

        assertThat(result.status()).isEqualTo(TicketStatus.PAID);
        verify(ticketRepository).save(any(Ticket.class));
    }

    // ---------- TEST-TICKET-002: usuario inexistente ----------
    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.purchase(request()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-003: usuario inactivo ----------
    @Test
    void shouldThrowWhenUserIsInactive() {
        User inactiveUser = activeUser(LocalDate.now().minusYears(25));
        inactiveUser.setActive(false);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(java.util.Optional.of(inactiveUser));

        assertThatThrownBy(() -> service.purchase(request()))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-004: evento DRAFT ----------
    @Test
    void shouldThrowWhenEventIsDraft() {
        User user = activeUser(LocalDate.now().minusYears(25));
        Venue venue = venueWithCapacity(3);
        Event event = new Event("CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, EventStatus.DRAFT, LocalDateTime.now().plusMonths(1), 18);
        venue.addEvent(event);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(java.util.Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(java.util.Optional.of(event));

        assertThatThrownBy(() -> service.purchase(request()))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-005: evento CANCELLED ----------
    @Test
    void shouldThrowWhenEventIsCancelled() {
        User user = activeUser(LocalDate.now().minusYears(25));
        Venue venue = venueWithCapacity(3);
        Event event = new Event("CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, EventStatus.CANCELLED, LocalDateTime.now().plusMonths(1), 18);
        venue.addEvent(event);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(java.util.Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(java.util.Optional.of(event));

        assertThatThrownBy(() -> service.purchase(request()))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-006: usuario menor de edad ----------
    @Test
    void shouldThrowWhenUserDoesNotMeetMinimumAge() {
        User minorUser = activeUser(LocalDate.now().minusYears(17));
        Venue venue = venueWithCapacity(3);
        Event event = publishedEvent(venue, 18);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(java.util.Optional.of(minorUser));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(java.util.Optional.of(event));

        assertThatThrownBy(() -> service.purchase(request()))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-007: sin capacidad ----------
    @Test
    void shouldThrowWhenNoCapacityAvailable() {
        User user = activeUser(LocalDate.now().minusYears(25));
        Venue venue = venueWithCapacity(1);
        Event event = publishedEvent(venue, 18);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(java.util.Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(java.util.Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(1L);

        assertThatThrownBy(() -> service.purchase(request()))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-008: último ticket disponible -> SOLD_OUT ----------
    @Test
    void shouldMarkEventAsSoldOutWhenLastTicketIsPurchased() {
        User user = activeUser(LocalDate.now().minusYears(25));
        Venue venue = venueWithCapacity(1);
        Event event = publishedEvent(venue, 18);

        TicketResponse response = new TicketResponse(1L, "TCK-ABC", TicketType.GENERAL,
                new BigDecimal("100.00"), TicketStatus.PAID, LocalDateTime.now(),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest");

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(java.util.Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(java.util.Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Ticket.class))).thenReturn(response);

        service.purchase(request());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        verify(ticketRepository).save(any(Ticket.class));

        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(EventStatus.SOLD_OUT);
    }

    // ---------- TEST-TICKET-009: cancelar PAID -> CANCELLED ----------
    @Test
    void shouldCancelPaidTicket() {
        Venue venue = venueWithCapacity(3);
        Event event = publishedEvent(venue, 0);
        User user = activeUser(LocalDate.now().minusYears(25));
        Ticket ticket = new Ticket("TCK-ABC", TicketType.GENERAL, new BigDecimal("100.00"),
                TicketStatus.PAID, LocalDateTime.now(), user, event);

        TicketResponse response = new TicketResponse(1L, "TCK-ABC", TicketType.GENERAL,
                new BigDecimal("100.00"), TicketStatus.CANCELLED, ticket.getPurchaseDate(),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest");

        when(ticketRepository.findByTicketCode("TCK-ABC")).thenReturn(java.util.Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = service.cancel("TCK-ABC");

        assertThat(result.status()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
    }

    // ---------- TEST-TICKET-010: cancelar USED -> error ----------
    @Test
    void shouldThrowWhenCancellingUsedTicket() {
        Venue venue = venueWithCapacity(3);
        Event event = publishedEvent(venue, 0);
        User user = activeUser(LocalDate.now().minusYears(25));
        Ticket ticket = new Ticket("TCK-ABC", TicketType.GENERAL, new BigDecimal("100.00"),
                TicketStatus.USED, LocalDateTime.now(), user, event);

        when(ticketRepository.findByTicketCode("TCK-ABC")).thenReturn(java.util.Optional.of(ticket));

        assertThatThrownBy(() -> service.cancel("TCK-ABC"))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-011: marcar PAID como usado -> USED ----------
    @Test
    void shouldMarkPaidTicketAsUsed() {
        Venue venue = venueWithCapacity(3);
        Event event = publishedEvent(venue, 0);
        User user = activeUser(LocalDate.now().minusYears(25));
        Ticket ticket = new Ticket("TCK-ABC", TicketType.GENERAL, new BigDecimal("100.00"),
                TicketStatus.PAID, LocalDateTime.now(), user, event);

        TicketResponse response = new TicketResponse(1L, "TCK-ABC", TicketType.GENERAL,
                new BigDecimal("100.00"), TicketStatus.USED, ticket.getPurchaseDate(),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest");

        when(ticketRepository.findByTicketCode("TCK-ABC")).thenReturn(java.util.Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = service.markAsUsed("TCK-ABC");

        assertThat(result.status()).isEqualTo(TicketStatus.USED);
    }

    // ---------- TEST-TICKET-012: usar ticket CANCELLED -> error ----------
    @Test
    void shouldThrowWhenUsingCancelledTicket() {
        Venue venue = venueWithCapacity(3);
        Event event = publishedEvent(venue, 0);
        User user = activeUser(LocalDate.now().minusYears(25));
        Ticket ticket = new Ticket("TCK-ABC", TicketType.GENERAL, new BigDecimal("100.00"),
                TicketStatus.CANCELLED, LocalDateTime.now(), user, event);

        when(ticketRepository.findByTicketCode("TCK-ABC")).thenReturn(java.util.Optional.of(ticket));

        assertThatThrownBy(() -> service.markAsUsed("TCK-ABC"))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }
}