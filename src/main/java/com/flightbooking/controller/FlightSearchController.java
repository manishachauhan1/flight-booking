package com.flightbooking.controller;

import com.flightbooking.dto.FlightSearchRequest;
import com.flightbooking.dto.FlightSearchResponse;
import com.flightbooking.service.FlightSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.flightbooking.Constants.API_FLIGHTS;

@RestController
@RequestMapping(API_FLIGHTS)
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    public FlightSearchController(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<FlightSearchResponse> searchFlights(@Valid FlightSearchRequest request) {
        FlightSearchResponse response = flightSearchService.search(request);
        return ResponseEntity.ok(response);
    }
}
