package com.flightbooking.entity;

import com.flightbooking.enums.BookingStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.flightbooking.Constants.PNR_LENGTH;
import static com.flightbooking.Constants.STATUS_COLUMN_LENGTH;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_pnr", columnList = "pnr", unique = true),
    @Index(name = "idx_booking_status_expires", columnList = "status, expiresAt")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = PNR_LENGTH)
    private String pnr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = STATUS_COLUMN_LENGTH)
    private BookingStatus status;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    @Column(columnDefinition = "TEXT")
    private String addOnsJson;

    @Version
    private Integer version;

    @ManyToMany
    @JoinTable(
        name = "booking_flight",
        joinColumns = @JoinColumn(name = "booking_id"),
        inverseJoinColumns = @JoinColumn(name = "flight_id")
    )
    private List<Flight> flights = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Passenger> passengers = new ArrayList<>();

    public Booking() {}

    public Booking(String pnr, BookingStatus status, Double totalAmount, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.pnr = pnr;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static String generatePnr() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, PNR_LENGTH).toUpperCase();
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
    public String getAddOnsJson() { return addOnsJson; }
    public void setAddOnsJson(String addOnsJson) { this.addOnsJson = addOnsJson; }
    public Integer getVersion() { return version; }
    public List<Flight> getFlights() { return flights; }
    public void setFlights(List<Flight> flights) { this.flights = flights; }
    public List<Passenger> getPassengers() { return passengers; }
    public void setPassengers(List<Passenger> passengers) { this.passengers = passengers; }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canTransitionTo(BookingStatus target) {
        return this.status.canTransitionTo(target);
    }

    public void addFlight(Flight flight) {
        this.flights.add(flight);
    }

    public void addPassenger(Passenger passenger) {
        this.passengers.add(passenger);
        passenger.setBooking(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking booking)) return false;
        return Objects.equals(id, booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
