package com.flightbooking.service.search;

import com.flightbooking.entity.Flight;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RouteCache {

    private final int maxStops;
    private final long minLayoverMinutes;
    private final long maxLayoverMinutes;

    // volatile + atomic swap: rebuild() builds a new map off-side, then assigns.
    // Readers see either the old map or the complete new map — no locks at read time.
    private volatile Map<RouteKey, List<RoutePath>> cache = Map.of();

    public RouteCache(int maxStops, long minLayoverMinutes, long maxLayoverMinutes) {
        this.maxStops = maxStops;
        this.minLayoverMinutes = minLayoverMinutes;
        this.maxLayoverMinutes = maxLayoverMinutes;
    }

    public void rebuild(List<Flight> allFlights) {
        if (allFlights.isEmpty()) {
            this.cache = Map.of();
            return;
        }

        Map<String, List<Flight>> departureIndex = buildDepartureIndex(allFlights);

        Set<String> airports = new HashSet<>();
        for (Flight f : allFlights) {
            airports.add(f.getSource());
            airports.add(f.getDestination());
        }

        LocalDate date = allFlights.get(0).getDepartureDate();
        Map<RouteKey, List<RoutePath>> newCache = new ConcurrentHashMap<>();

        for (String origin : airports) {
            for (String destination : airports) {
                if (origin.equals(destination)) continue;
                RouteKey key = new RouteKey(origin, destination, date);
                List<RoutePath> paths = exploreConnections(origin, destination, departureIndex);
                newCache.put(key, paths);
            }
        }

        this.cache = newCache;
    }

    public List<RoutePath> lookup(RouteKey key) {
        return cache.getOrDefault(key, List.of());
    }

    private Map<String, List<Flight>> buildDepartureIndex(List<Flight> flights) {
        return flights.stream()
                .collect(Collectors.groupingBy(Flight::getSource));
    }

    // BFS from origin: try all direct flights, then extend each with valid connections
    // up to maxStops. Validates layover bounds. Deduplicates identical flight chains.
    private List<RoutePath> exploreConnections(String origin, String destination,
                                                Map<String, List<Flight>> departureIndex) {
        List<RoutePath> discovered = new ArrayList<>();
        Deque<RoutePath> frontier = new ArrayDeque<>();

        List<Flight> directFlights = departureIndex.getOrDefault(origin, List.of());
        for (Flight flight : directFlights) {
            frontier.add(new RoutePath(flight));
        }

        while (!frontier.isEmpty()) {
            RoutePath current = frontier.poll();
            Flight last = current.lastSegment();

            if (last.getDestination().equals(destination)) {
                discovered.add(current);
                continue;
            }

            if (current.stopCount() >= maxStops) continue;

            List<Flight> connections = departureIndex.getOrDefault(last.getDestination(), List.of());
            for (Flight next : connections) {
                long layover = Duration.between(
                        last.getArrivalDateTime(), next.getDepartureDateTime()).toMinutes();
                if (layover >= minLayoverMinutes && layover <= maxLayoverMinutes) {
                    frontier.add(current.extend(next, layover));
                }
            }
        }

        return deduplicate(discovered);
    }

    // Removes same-flight-chain duplicates (e.g. DEL→BOM→MAA could be reached
    // via different layover windows, but the flight IDs are the same).
    private List<RoutePath> deduplicate(List<RoutePath> paths) {
        Set<String> seen = new HashSet<>();
        List<RoutePath> result = new ArrayList<>();
        for (RoutePath path : paths) {
            if (seen.add(path.flightIdKey())) {
                result.add(path);
            }
        }
        return result;
    }
}
