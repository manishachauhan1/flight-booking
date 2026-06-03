package com.flightbooking.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookingStatusTest {

    @Test
    void initiatedCanTransitionToSeatLocked() {
        assertTrue(BookingStatus.INITIATED.canTransitionTo(BookingStatus.SEAT_LOCKED));
    }

    @Test
    void initiatedCanTransitionToExpired() {
        assertTrue(BookingStatus.INITIATED.canTransitionTo(BookingStatus.EXPIRED));
    }

    @Test
    void initiatedCannotTransitionToConfirmed() {
        assertFalse(BookingStatus.INITIATED.canTransitionTo(BookingStatus.CONFIRMED));
    }

    @Test
    void initiatedCannotTransitionToCancelled() {
        assertFalse(BookingStatus.INITIATED.canTransitionTo(BookingStatus.CANCELLED));
    }

    @Test
    void seatLockedCanTransitionToConfirmed() {
        assertTrue(BookingStatus.SEAT_LOCKED.canTransitionTo(BookingStatus.CONFIRMED));
    }

    @Test
    void seatLockedCanTransitionToExpired() {
        assertTrue(BookingStatus.SEAT_LOCKED.canTransitionTo(BookingStatus.EXPIRED));
    }

    @Test
    void seatLockedCanTransitionToCancelled() {
        assertTrue(BookingStatus.SEAT_LOCKED.canTransitionTo(BookingStatus.CANCELLED));
    }

    @Test
    void seatLockedCannotTransitionToInitiated() {
        assertFalse(BookingStatus.SEAT_LOCKED.canTransitionTo(BookingStatus.INITIATED));
    }

    @Test
    void confirmedCanTransitionToCancelled() {
        assertTrue(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.CANCELLED));
    }

    @Test
    void confirmedCannotTransitionToExpired() {
        assertFalse(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.EXPIRED));
    }

    @Test
    void confirmedCannotTransitionToSeatLocked() {
        assertFalse(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.SEAT_LOCKED));
    }

    @Test
    void cancelledCannotTransitionToAnything() {
        assertFalse(BookingStatus.CANCELLED.canTransitionTo(BookingStatus.INITIATED));
        assertFalse(BookingStatus.CANCELLED.canTransitionTo(BookingStatus.SEAT_LOCKED));
        assertFalse(BookingStatus.CANCELLED.canTransitionTo(BookingStatus.CONFIRMED));
        assertFalse(BookingStatus.CANCELLED.canTransitionTo(BookingStatus.EXPIRED));
        assertFalse(BookingStatus.CANCELLED.canTransitionTo(BookingStatus.CANCELLED));
    }

    @Test
    void expiredCannotTransitionToAnything() {
        assertFalse(BookingStatus.EXPIRED.canTransitionTo(BookingStatus.INITIATED));
        assertFalse(BookingStatus.EXPIRED.canTransitionTo(BookingStatus.SEAT_LOCKED));
        assertFalse(BookingStatus.EXPIRED.canTransitionTo(BookingStatus.CONFIRMED));
        assertFalse(BookingStatus.EXPIRED.canTransitionTo(BookingStatus.CANCELLED));
        assertFalse(BookingStatus.EXPIRED.canTransitionTo(BookingStatus.EXPIRED));
    }
}
