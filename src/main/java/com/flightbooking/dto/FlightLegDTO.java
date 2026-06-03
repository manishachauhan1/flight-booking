package com.flightbooking.dto;

import com.flightbooking.entity.Flight;

public record FlightLegDTO(
        Long flightId,
        String flightNumber,
        String airline,
        String source,
        String destination,
        String departureDateTime,
        String arrivalDateTime,
        double price
) {
    public FlightLegDTO(Flight f) {
        this(f.getId(), f.getFlightNumber(), f.getAirline(), f.getSource(), f.getDestination(),
                f.getDepartureDateTime().toString(), f.getArrivalDateTime().toString(), f.getPrice());
    }
}
