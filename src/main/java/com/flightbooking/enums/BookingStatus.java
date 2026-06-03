package com.flightbooking.enums;

// Booking state machine. canTransitionTo() encodes all valid transitions.
//
// INITIATED ──► SEAT_LOCKED ──► CONFIRMED ──► CANCELLED
//      │               │
//      └──► EXPIRED ◄──┘
//
// EXPIRED and CANCELLED are terminal states.
public enum BookingStatus {
    INITIATED,
    SEAT_LOCKED,
    CONFIRMED,
    CANCELLED,
    EXPIRED;

    public boolean canTransitionTo(BookingStatus target) {
        return switch (this) {
            case INITIATED -> target == SEAT_LOCKED || target == EXPIRED;
            case SEAT_LOCKED -> target == CONFIRMED || target == EXPIRED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case EXPIRED, CANCELLED -> false;
        };
    }
}
