package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByEventCode(String eventCode);
    // FR-EVT-... : buscar por eventCode
    Optional<Event> findByEventCode(String eventCode);

    // FR-EVT-005: eventos publicados ordenados por fecha
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // FR-VEN-004: eventos de un venue por venue.code
    List<Event> findByVenueCode(String code);

    // FR-ART-004 / FR-SRC-001: eventos por artista (N:M, sin duplicados)
    @Query("""
        select distinct e
        from Event e
        join e.artists a
        where a.stageName = :stageName
        """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    // FR-SRC-002: eventos de una ciudad en los que participa un artista
    @Query("""
        select distinct e
        from Event e
        join e.artists a
        where e.venue.city = :city
        and a.stageName = :stageName
        """)
    List<Event> findByVenueCityAndArtistStageName(@Param("city") String city,
                                                    @Param("stageName") String stageName);

    // FR-SRC-003: eventos recomendados
    @Query("""
        select distinct e
        from Event e
        join e.artists a
        where e.status = 'PUBLISHED'
        and e.eventDate > :afterDate
        and e.venue.city = :city
        and lower(a.stageName) like lower(concat('%', :artistText, '%'))
        order by e.eventDate asc
        """)
    List<Event> findRecommended(@Param("afterDate") LocalDateTime afterDate,
                                 @Param("city") String city,
                                 @Param("artistText") String artistText);
}