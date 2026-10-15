package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.impl.EventServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private AuthorizationService authorizationService;
        
    @Mock
    private EventMapper mapper;

    @InjectMocks
    private EventServiceImpl service;

    private Venue activeVenue() {
        return new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1 #2-3", 3000);
    }

    private CreateEventRequest validRequest() {
        return new CreateEventRequest(
                "CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, LocalDateTime.now().plusMonths(1), 18, "VEN-SMR-01");
    }

    // ---------- TEST-EVENT-001: evento existente -> DTO ----------
    @Test
    void shouldFindEventByCode() {
        Venue venue = activeVenue();
        Event event = new Event("CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, EventStatus.DRAFT, LocalDateTime.now().plusMonths(1), 18);
        venue.addEvent(event);

        EventResponse response = new EventResponse(1L, "CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, EventStatus.DRAFT, event.getEventDate(), 18,
                "VEN-SMR-01", "Marina Convention Center", List.of());

        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(mapper.toResponse(event)).thenReturn(response);
        when(authorizationService.isStaffOrAdmin()).thenReturn(true);

        EventResponse result = service.findByCode("CMF-2026");

        assertThat(result).isEqualTo(response);
    }

    // ---------- TEST-EVENT-002: evento inexistente ----------
    @Test
    void shouldThrowWhenEventNotFound() {
        when(eventRepository.findByEventCode("CMF-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("CMF-9999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- TEST-EVENT-003: crear evento válido -> save() ----------
    @Test
    void shouldCreateEventWhenRequestIsValid() {
        Venue venue = activeVenue();
        EventResponse response = new EventResponse(1L, "CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, EventStatus.DRAFT, LocalDateTime.now().plusMonths(1), 18,
                "VEN-SMR-01", "Marina Convention Center", List.of());

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Event.class))).thenReturn(response);

        EventResponse result = service.create(validRequest());

        assertThat(result).isEqualTo(response);
        verify(eventRepository).save(any(Event.class));
    }

    // ---------- TEST-EVENT-004: venue inexistente -> no save() ----------
    @Test
    void shouldThrowWhenVenueNotFound() {
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    // ---------- TEST-EVENT-005: venue inactivo ----------
    @Test
    void shouldThrowWhenVenueIsInactive() {
        Venue inactiveVenue = activeVenue();
        inactiveVenue.setActive(false);

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(inactiveVenue));

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // ---------- TEST-EVENT-006: fecha pasada ----------
    @Test
    void shouldThrowWhenEventDateIsInThePast() {
        Venue venue = activeVenue();
        CreateEventRequest pastRequest = new CreateEventRequest(
                "CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, LocalDateTime.now().minusDays(1), 18, "VEN-SMR-01");

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> service.create(pastRequest))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // ---------- TEST-EVENT-007: publicar DRAFT válido -> PUBLISHED ----------
    @Test
    void shouldPublishValidDraftEvent() {
        Venue venue = activeVenue();
        Event event = new Event("CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, EventStatus.DRAFT, LocalDateTime.now().plusMonths(1), 18);
        venue.addEvent(event);

        EventResponse response = new EventResponse(1L, "CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, EventStatus.PUBLISHED, event.getEventDate(), 18,
                "VEN-SMR-01", "Marina Convention Center", List.of());

        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(mapper.toResponse(event)).thenReturn(response);

        EventResponse result = service.publish("CMF-2026");

        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    // ---------- TEST-EVENT-008: publicar CANCELLED -> error, no persistir ----------
    @Test
    void shouldThrowWhenPublishingCancelledEvent() {
        Venue venue = activeVenue();
        Event event = new Event("CMF-2026", "Caribbean Music Fest", "Festival",
                EventCategory.MUSIC, EventStatus.CANCELLED, LocalDateTime.now().plusMonths(1), 18);
        venue.addEvent(event);

        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }
        // ---------- visibilidad de eventos no publicados ----------

    private Event eventWithStatus(String code, EventStatus status) {
        Venue venue = activeVenue();
        Event event = new Event(code, "Evento " + code, "Descripción",
                EventCategory.MUSIC, status, LocalDateTime.now().plusMonths(1), 18);
        venue.addEvent(event);
        return event;
    }

    private EventSummaryResponse summary(Event event) {
        return new EventSummaryResponse(1L, event.getEventCode(), event.getName(), event.getCategory(),
                event.getStatus(), event.getEventDate(), "Marina Convention Center");
    }

    @Test
    void nonPrivilegedCallerGetsNotFoundForDraftEvent() {
        Event draft = eventWithStatus("CMF-2026", EventStatus.DRAFT);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(draft));
        // authorizationService.isStaffOrAdmin() devuelve false por defecto

        assertThatThrownBy(() -> service.findByCode("CMF-2026"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void anyoneCanReadPublishedEventByCode() {
        Event published = eventWithStatus("CMF-2026", EventStatus.PUBLISHED);
        EventResponse response = new EventResponse(1L, "CMF-2026", "Evento CMF-2026", "Descripción",
                EventCategory.MUSIC, EventStatus.PUBLISHED, published.getEventDate(), 18,
                "VEN-SMR-01", "Marina Convention Center", List.of());
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(published));
        when(mapper.toResponse(published)).thenReturn(response);

        assertThat(service.findByCode("CMF-2026")).isEqualTo(response);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void publicSearchByArtistHidesNonPublishedEvents() {
        Event published = eventWithStatus("PUB-1", EventStatus.PUBLISHED);
        Event draft = eventWithStatus("DRAFT-1", EventStatus.DRAFT);
        when(eventRepository.findByArtistStageName("Solar Beat")).thenReturn(List.of(published, draft));
        when(mapper.toSummary(published)).thenReturn(summary(published));

        List<EventSummaryResponse> result = service.findByArtist("Solar Beat");

        assertThat(result).extracting(EventSummaryResponse::eventCode).containsExactly("PUB-1");
        verify(mapper, never()).toSummary(draft);
    }

    @Test
    void staffSearchByArtistSeesEverything() {
        Event published = eventWithStatus("PUB-1", EventStatus.PUBLISHED);
        Event draft = eventWithStatus("DRAFT-1", EventStatus.DRAFT);
        when(authorizationService.isStaffOrAdmin()).thenReturn(true);
        when(eventRepository.findByArtistStageName("Solar Beat")).thenReturn(List.of(published, draft));
        when(mapper.toSummary(published)).thenReturn(summary(published));
        when(mapper.toSummary(draft)).thenReturn(summary(draft));

        assertThat(service.findByArtist("Solar Beat"))
                .extracting(EventSummaryResponse::eventCode)
                .containsExactly("PUB-1", "DRAFT-1");
    }
}