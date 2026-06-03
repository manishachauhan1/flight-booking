package com.flightbooking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "seats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"flight_id", "seatNumber"})
}, indexes = {
    @Index(name = "idx_seat_flight_available", columnList = "flight_id, isAvailable")
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
    private Boolean isAvailable;

    private LocalDateTime lockedUntil;

    @Column(name = "locked_by_booking_id")
    private Long lockedByBookingId;

    public Seat() {}

    public Seat(Flight flight, String seatNumber, Boolean isAvailable) {
        this.flight = flight;
        this.seatNumber = seatNumber;
        this.isAvailable = isAvailable;
        this.lockedUntil = null;
        this.lockedByBookingId = null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean available) { isAvailable = available; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public Long getLockedByBookingId() { return lockedByBookingId; }
    public void setLockedByBookingId(Long lockedByBookingId) { this.lockedByBookingId = lockedByBookingId; }

    public boolean isBookable() {
        return isAvailable && (lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now()));
    }

    public void lock(Long bookingId, int durationMinutes) {
        this.isAvailable = false;
        this.lockedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
        this.lockedByBookingId = bookingId;
    }

    public void release() {
        this.isAvailable = true;
        this.lockedUntil = null;
        this.lockedByBookingId = null;
    }

    public void confirm() {
        this.isAvailable = false;
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
