package com.flightbooking.dto;

import com.flightbooking.entity.Flight;

import java.util.ArrayList;
import java.util.List;

public class FlightResultDTO {

    private List<FlightLegDTO> legs = new ArrayList<>();
    private double totalPrice;
    private String priceToken;
    private boolean direct;
    private String layoverCity;
    private Long layoverDurationMinutes;
    private long totalDurationMinutes;

    public FlightResultDTO() {}

    public FlightResultDTO(Flight flight, String priceToken) {
        this.legs.add(new FlightLegDTO(flight));
        this.totalPrice = flight.getPrice();
        this.priceToken = priceToken;
        this.direct = true;
        this.totalDurationMinutes = flight.getDurationMinutes();
    }

    public FlightResultDTO(Flight leg1, Flight leg2, String priceToken) {
        this.legs.add(new FlightLegDTO(leg1));
        this.legs.add(new FlightLegDTO(leg2));
        this.totalPrice = leg1.getPrice() + leg2.getPrice();
        this.priceToken = priceToken;
        this.direct = false;
        this.layoverCity = leg1.getDestination();
        this.layoverDurationMinutes = java.time.Duration.between(
                leg1.getArrivalDateTime(), leg2.getDepartureDateTime()).toMinutes();
        this.totalDurationMinutes = leg1.getDurationMinutes() + layoverDurationMinutes + leg2.getDurationMinutes();
    }

    public List<FlightLegDTO> getLegs() { return legs; }
    public void setLegs(List<FlightLegDTO> legs) { this.legs = legs; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public String getPriceToken() { return priceToken; }
    public void setPriceToken(String priceToken) { this.priceToken = priceToken; }
    public boolean isDirect() { return direct; }
    public void setDirect(boolean direct) { this.direct = direct; }
    public String getLayoverCity() { return layoverCity; }
    public void setLayoverCity(String layoverCity) { this.layoverCity = layoverCity; }
    public Long getLayoverDurationMinutes() { return layoverDurationMinutes; }
    public void setLayoverDurationMinutes(Long layoverDurationMinutes) { this.layoverDurationMinutes = layoverDurationMinutes; }
    public long getTotalDurationMinutes() { return totalDurationMinutes; }
    public void setTotalDurationMinutes(long totalDurationMinutes) { this.totalDurationMinutes = totalDurationMinutes; }

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
}
