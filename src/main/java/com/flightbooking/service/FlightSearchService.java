package com.flightbooking.service;

import com.flightbooking.dto.FlightResultDTO;
import com.flightbooking.dto.FlightSearchRequest;
import com.flightbooking.dto.FlightSearchResponse;
import com.flightbooking.entity.Flight;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.service.search.RouteCache;
import com.flightbooking.service.search.RouteKey;
import com.flightbooking.service.search.RoutePath;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FlightSearchService {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchService.class);

    private final FlightRepository flightRepository;
    private final PricingService pricingService;
    private final RouteCache routeCache;

    public FlightSearchService(FlightRepository flightRepository, PricingService pricingService,
                               @Value("${app.search.max-stops}") int maxStops,
                               @Value("${app.layover.min-minutes}") long minLayoverMinutes,
                               @Value("${app.layover.max-minutes}") long maxLayoverMinutes) {
        this.flightRepository = flightRepository;
        this.pricingService = pricingService;
        this.routeCache = new RouteCache(maxStops, minLayoverMinutes, maxLayoverMinutes);
    }

    @PostConstruct
    public void initializeRouteCache() {
        rebuildCache();
    }

    @Scheduled(fixedRateString = "${app.search.cache-refresh-ms}")
    public void refreshRouteCache() {
        rebuildCache();
    }

    private void rebuildCache() {
        List<Flight> allFlights = flightRepository.findAll();
        routeCache.rebuild(allFlights);
        log.info("Route cache rebuilt with {} flights, {} routes discovered",
                allFlights.size(), countRoutes());
    }

    private long countRoutes() {
        return 0;
    }

    public FlightSearchResponse search(FlightSearchRequest request) {
        RouteKey key = new RouteKey(request.getSource(), request.getDestination(), request.getDate());
        List<RoutePath> paths = routeCache.lookup(key);

        List<FlightResultDTO> results = new ArrayList<>();
        for (RoutePath path : paths) {
            if (request.getAirline() != null &&
                    path.segments().stream().anyMatch(f -> !f.getAirline().equalsIgnoreCase(request.getAirline()))) {
                continue;
            }
            if (path.segments().stream().allMatch(f -> f.hasAvailableSeats(request.getPassengerCount()))) {
                String token = pricingService.generatePriceToken(path.segments(), path.totalPrice());
                results.add(new FlightResultDTO(path, token));
            }
        }

        if (request.isDirectOnly()) {
            results.removeIf(r -> !r.isDirect());
        }

        results.sort(Comparator.comparingDouble(FlightResultDTO::getTotalPrice));

        FlightSearchResponse response = new FlightSearchResponse();
        response.setResults(results);

        log.info("Search {} -> {} on {}: found {} results",
                request.getSource(), request.getDestination(), request.getDate(), results.size());
        return response;
    }
}
