package com.pulsepass.service;

import com.pulsepass.dto.response.VenueResponse;

import java.util.List;

public interface VenueService {

    VenueResponse findByCode(String code);

    List<VenueResponse> findActiveVenues();
}