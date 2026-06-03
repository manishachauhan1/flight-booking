package com.flightbooking.service;

import com.flightbooking.dto.FlightResultDTO;
import com.flightbooking.dto.FlightSearchRequest;
import com.flightbooking.dto.FlightSearchResponse;
import com.flightbooking.entity.Flight;
import com.flightbooking.repository.FlightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.flightbooking.Constants.CONNECTING_FLIGHT_KEY_SEPARATOR;

@Service
public class FlightSearchService {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchService.class);

    private final FlightRepository flightRepository;
    private final PricingService pricingService;
    private final long minLayoverMinutes;
    private final long maxLayoverMinutes;

    public FlightSearchService(FlightRepository flightRepository, PricingService pricingService,
                               @Value("${app.layover.min-minutes}") long minLayoverMinutes,
                               @Value("${app.layover.max-minutes}") long maxLayoverMinutes) {
        this.flightRepository = flightRepository;
        this.pricingService = pricingService;
        this.minLayoverMinutes = minLayoverMinutes;
        this.maxLayoverMinutes = maxLayoverMinutes;
    }

    public FlightSearchResponse search(FlightSearchRequest request) {
        List<FlightResultDTO> results = new ArrayList<>();
        results.addAll(findDirectFlights(request));
        if (!request.isDirectOnly()) {
            results.addAll(findConnectingFlights(request));
        }
        results.sort(Comparator.comparingDouble(FlightResultDTO::getTotalPrice));

        FlightSearchResponse response = new FlightSearchResponse();
        response.setResults(results);

        log.info("Search {} -> {} on {}: found {} results",
                request.getSource(), request.getDestination(), request.getDate(), results.size());
        return response;
    }

    private List<FlightResultDTO> findDirectFlights(FlightSearchRequest request) {
        return flightRepository
                .findBySourceAndDestinationAndDepartureDate(request.getSource(), request.getDestination(), request.getDate())
                .stream()
                .filter(f -> f.hasAvailableSeats(request.getPassengers()))
                .map(f -> new FlightResultDTO(f, pricingService.generatePriceToken(List.of(f), f.getPrice())))
                .collect(Collectors.toList());
    }

    private List<FlightResultDTO> findConnectingFlights(FlightSearchRequest request) {
        List<Flight> fromSource = flightRepository.findBySourceAndDepartureDate(
                request.getSource(), request.getDate());
        List<Flight> toDestination = flightRepository.findByDestinationAndDepartureDate(
                request.getDestination(), request.getDate());

        Map<String, List<Flight>> destIndex = toDestination.stream()
                .filter(f -> f.hasAvailableSeats(request.getPassengers()))
                .collect(Collectors.groupingBy(Flight::getSource));

        Set<String> seen = new HashSet<>();
        List<FlightResultDTO> results = new ArrayList<>();

        for (Flight leg1 : fromSource) {
            if (!leg1.hasAvailableSeats(request.getPassengers())) continue;
            List<Flight> candidates = destIndex.get(leg1.getDestination());
            if (candidates == null) continue;

            for (Flight leg2 : candidates) {
                long layover = java.time.Duration.between(
                        leg1.getArrivalDateTime(), leg2.getDepartureDateTime()).toMinutes();
                if (layover < minLayoverMinutes || layover > maxLayoverMinutes) continue;

                String key = leg1.getId() + CONNECTING_FLIGHT_KEY_SEPARATOR + leg2.getId();
                if (!seen.add(key)) continue;

                double totalPrice = leg1.getPrice() + leg2.getPrice();
                String token = pricingService.generatePriceToken(List.of(leg1, leg2), totalPrice);
                results.add(new FlightResultDTO(leg1, leg2, token));
            }
        }
        return results;
    }
}
