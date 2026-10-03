package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByStageNameIgnoreCase(String stageName);
    List<Artist> findByActiveTrueOrderByStageNameAsc();
    Optional<Artist> findByStageName(String stageName);
}