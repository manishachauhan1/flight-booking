package com.flightbooking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

// A specific seat on a specific flight.
// Lifecycle: available → lock() [PESSIMISTIC_WRITE] → confirm() or release().
@Entity
@Table(name = "seats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"flight_id", "seatNumber"})
}, indexes = {
    @Index(name = "idx_seat_flight_available", columnList = "flight_id, available")
})
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private String seatNumber;

    @Column(nullable = false)
    private Boolean available;

    private LocalDateTime lockedUntil;

    @Column(name = "locked_by_booking_id")
    private Long lockedByBookingId;

    public Seat() {}

    public Seat(Flight flight, String seatNumber, Boolean available) {
        this.flight = flight;
        this.seatNumber = seatNumber;
        this.available = available;
        this.lockedUntil = null;
        this.lockedByBookingId = null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public Long getLockedByBookingId() { return lockedByBookingId; }
    public void setLockedByBookingId(Long lockedByBookingId) { this.lockedByBookingId = lockedByBookingId; }

    // Available and not currently locked by any booking.
    public boolean isBookable() {
        return available && (lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now()));
    }

    public void lock(Long bookingId, int durationMinutes) {
        this.available = false;
        this.lockedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
        this.lockedByBookingId = bookingId;
    }

    public void release() {
        this.available = true;
        this.lockedUntil = null;
        this.lockedByBookingId = null;
    }

    public void confirm() {
        this.available = false;
        this.lockedUntil = null;
        this.lockedByBookingId = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat seat)) return false;
        return Objects.equals(id, seat.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
