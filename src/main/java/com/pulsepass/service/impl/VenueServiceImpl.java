package com.pulsepass.service.impl;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.VenueService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VenueServiceImpl implements VenueService {

    private final VenueRepository repository;
    private final VenueMapper mapper;

    public VenueServiceImpl(VenueRepository repository, VenueMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public VenueResponse findByCode(String code) {
        return repository.findByCode(code)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venue not found: " + code));
    }

    @Override
    public List<VenueResponse> findActiveVenues() {
        return repository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}