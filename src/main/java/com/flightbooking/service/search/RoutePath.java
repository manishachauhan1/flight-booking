package com.flightbooking.service.search;

import com.flightbooking.entity.Flight;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Immutable flight chain. All fields are final.
// extend() returns a new copy — safe for concurrent reads from the cache.
public class RoutePath {

    private final List<Flight> segments;
    private final List<Stopover> stopovers;
    private final double totalPrice;
    private final long totalDurationMinutes;
    private final String flightIdKey;

    public RoutePath(Flight flight) {
        this.segments = List.of(flight);
        this.stopovers = List.of();
        this.totalPrice = flight.getPrice();
        this.totalDurationMinutes = flight.getDurationMinutes();
        this.flightIdKey = flight.getId().toString();
    }

    private RoutePath(List<Flight> segments, List<Stopover> stopovers,
                      double totalPrice, long totalDurationMinutes, String flightIdKey) {
        this.segments = segments;
        this.stopovers = stopovers;
        this.totalPrice = totalPrice;
        this.totalDurationMinutes = totalDurationMinutes;
        this.flightIdKey = flightIdKey;
    }

    // Returns a new RoutePath with the next flight appended.
    // The original path is unchanged (immutable).
    public RoutePath extend(Flight next, long layoverMinutes) {
        List<Flight> newSegments = new ArrayList<>(segments);
        newSegments.add(next);

        Flight last = segments.get(segments.size() - 1);
        Stopover stopover = new Stopover(last.getDestination(), layoverMinutes,
                last.getArrivalDateTime(), next.getDepartureDateTime());

        List<Stopover> newStopovers = new ArrayList<>(stopovers);
        newStopovers.add(stopover);

        double newPrice = totalPrice + next.getPrice();
        long newDuration = totalDurationMinutes + layoverMinutes + next.getDurationMinutes();
        String newKey = flightIdKey + "-" + next.getId();

        return new RoutePath(Collections.unmodifiableList(newSegments),
                Collections.unmodifiableList(newStopovers),
                newPrice, newDuration, newKey);
    }

    public Flight lastSegment() {
        return segments.get(segments.size() - 1);
    }

    public int stopCount() {
        return stopovers.size();
    }

    public List<Flight> segments() { return segments; }
    public List<Stopover> stopovers() { return stopovers; }
    public double totalPrice() { return totalPrice; }
    public long totalDurationMinutes() { return totalDurationMinutes; }
    public String flightIdKey() { return flightIdKey; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoutePath routePath)) return false;
        return flightIdKey.equals(routePath.flightIdKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightIdKey);
    }
}
