package com.flightbooking.dto;

import com.flightbooking.entity.Booking;
import com.flightbooking.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class BookingResponse {

    private Long id;
    private String pnr;
    private BookingStatus status;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private List<FlightLegDTO> flights;
    private List<PassengerDTO> passengers;

    public BookingResponse() {}

    public BookingResponse(Booking booking) {
        this.id = booking.getId();
        this.pnr = booking.getPnr();
        this.status = booking.getStatus();
        this.totalAmount = booking.getTotalAmount();
        this.createdAt = booking.getCreatedAt();
        this.expiresAt = booking.getExpiresAt();
        this.flights = booking.getFlights().stream()
                .map(FlightLegDTO::new)
                .collect(Collectors.toList());
        this.passengers = booking.getPassengers().stream()
                .map(p -> {
                    PassengerDTO dto = new PassengerDTO(p.getName(), p.getAge(), p.getType(), p.getSeatNumber());
                    dto.setId(p.getId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public List<FlightLegDTO> getFlights() { return flights; }
    public void setFlights(List<FlightLegDTO> flights) { this.flights = flights; }
    public List<PassengerDTO> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerDTO> passengers) { this.passengers = passengers; }
}
