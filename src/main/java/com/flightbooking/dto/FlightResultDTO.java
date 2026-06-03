package com.flightbooking.dto;

import com.flightbooking.entity.Flight;
import com.flightbooking.service.search.RoutePath;
import com.flightbooking.service.search.Stopover;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FlightResultDTO {

    private List<FlightLegDTO> legs = new ArrayList<>();
    private double totalPrice;
    private String priceToken;
    private boolean direct;
    private String layoverCity;
    private Long layoverDurationMinutes;
    private long totalDurationMinutes;
    private List<StopoverDTO> stops;

    public FlightResultDTO() {}

    public FlightResultDTO(Flight flight, String priceToken) {
        this.legs.add(new FlightLegDTO(flight));
        this.totalPrice = flight.getPrice();
        this.priceToken = priceToken;
        this.direct = true;
        this.totalDurationMinutes = flight.getDurationMinutes();
        this.stops = List.of();
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
        this.stops = List.of(new StopoverDTO(leg1.getDestination(), this.layoverDurationMinutes,
                leg1.getArrivalDateTime().toString(), leg2.getDepartureDateTime().toString()));
    }

    public FlightResultDTO(RoutePath route, String priceToken) {
        this.legs = route.segments().stream()
                .map(FlightLegDTO::new)
                .collect(Collectors.toList());
        this.totalPrice = route.totalPrice();
        this.priceToken = priceToken;
        this.totalDurationMinutes = route.totalDurationMinutes();
        this.stops = route.stopovers().stream()
                .map(s -> new StopoverDTO(s.city(), s.layoverMinutes(),
                        s.arrival().toString(), s.departure().toString()))
                .collect(Collectors.toList());
        this.direct = route.segments().size() == 1;
        if (!direct && !route.stopovers().isEmpty()) {
            this.layoverCity = route.stopovers().get(0).city();
            this.layoverDurationMinutes = route.stopovers().get(0).layoverMinutes();
        }
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
    public List<StopoverDTO> getStops() { return stops; }
    public void setStops(List<StopoverDTO> stops) { this.stops = stops; }
}
