package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.EventService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper mapper;

    public EventServiceImpl(EventRepository eventRepository,
                             VenueRepository venueRepository,
                             ArtistRepository artistRepository,
                             EventMapper mapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        // BR-EVENT-001: eventCode único
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException(
                    "Event code already exists: " + request.eventCode());
        }

        // BR-EVENT-002: venue debe existir
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venue not found: " + request.venueCode()));

        // BR-EVENT-003: venue debe estar activo
        if (!venue.isActive()) {
            throw new BusinessRuleException(
                    "Cannot create event in inactive venue: " + request.venueCode());
        }

        // BR-EVENT-004: fecha futura
        if (!request.eventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Event date must be in the future: " + request.eventDate());
        }

        // BR-EVENT-006: minimumAge >= 0
        if (request.minimumAge() == null || request.minimumAge() < 0) {
            throw new BusinessRuleException(
                    "Minimum age cannot be negative: " + request.minimumAge());
        }

        // BR-EVENT-005: todo evento nuevo inicia en DRAFT
        Event event = new Event(
                request.eventCode(),
                request.name(),
                request.description(),
                request.category(),
                EventStatus.DRAFT,
                request.eventDate(),
                request.minimumAge()
        );

        venue.addEvent(event);

        Event saved = eventRepository.save(event);

        return mapper.toResponse(saved);
    }

    @Override
    public EventResponse findByCode(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + eventCode));
    }

    @Override
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + eventCode));

        // BR-EVENT-007: solo se puede publicar un evento DRAFT
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Current status: " + event.getStatus());
        }

        // BR-EVENT-008: la fecha debe seguir siendo futura
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Cannot publish an event with a past date: " + event.getEventDate());
        }

        // BR-EVENT-009: el venue debe seguir activo
        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException(
                    "Cannot publish event because venue is no longer active: "
                    + event.getVenue().getCode());
        }

        event.setStatus(EventStatus.PUBLISHED);

        Event saved = eventRepository.save(event);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + eventCode));

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Artist not found: " + artistId));

        // BR-EVENT-011: no se pueden agregar artistas a eventos CANCELLED o FINISHED
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "Cannot add artists to an event with status " + event.getStatus());
        }

        // BR-EVENT-010: no se puede asociar dos veces el mismo artista
        if (event.getArtists().contains(artist)) {
            throw new BusinessRuleException(
                    "Artist " + artist.getStageName() + " is already associated with this event");
        }

        event.addArtist(artist);

        Event saved = eventRepository.save(event);

        return mapper.toResponse(saved);
    }

    @Override
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findByArtistStageName(stageName)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }
}