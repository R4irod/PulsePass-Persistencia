package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    
    List<Venue> findByActiveTrueOrderByNameAsc();
    Optional<Venue> findByCode(String code);
    
}