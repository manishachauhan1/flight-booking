package com.flightbooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

import static com.flightbooking.Constants.DEFAULT_FLIGHT_TYPE;
import static com.flightbooking.Constants.DEFAULT_TRIP_TYPE;
import static com.flightbooking.Constants.FLIGHT_TYPE_DIRECT;
import static com.flightbooking.Constants.MIN_PASSENGERS;
import static com.flightbooking.Constants.DEFAULT_PASSENGERS;
import static com.flightbooking.Constants.TRIP_TYPE_ROUND_TRIP;

public class FlightSearchRequest {

    @NotBlank(message = "Source is required")
    private String source;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @Min(value = MIN_PASSENGERS, message = "At least 1 passenger required")
    private int passengerCount = DEFAULT_PASSENGERS;

    private String tripType = DEFAULT_TRIP_TYPE;
    private String flightType = DEFAULT_FLIGHT_TYPE;
    private LocalDate returnDate;
    private String airline;

    public FlightSearchRequest() {}

    public FlightSearchRequest(String source, String destination, LocalDate date, int passengerCount) {
        this.source = source;
        this.destination = destination;
        this.date = date;
        this.passengerCount = passengerCount;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }
    public String getTripType() { return tripType; }
    public void setTripType(String tripType) { this.tripType = tripType; }
    public String getFlightType() { return flightType; }
    public void setFlightType(String flightType) { this.flightType = flightType; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }

    public boolean isRoundTrip() {
        return TRIP_TYPE_ROUND_TRIP.equalsIgnoreCase(tripType);
    }

    public boolean isDirectOnly() {
        return FLIGHT_TYPE_DIRECT.equalsIgnoreCase(flightType);
    }
}
