package com.flightbooking.exception;

import com.flightbooking.enums.ErrorCode;

public class SeatUnavailableException extends BusinessException {
    public SeatUnavailableException(String seatNumber, String flightNumber) {
        super(ErrorCode.SEAT_UNAVAILABLE, "Seat " + seatNumber + " is not available on flight " + flightNumber);
    }

    public SeatUnavailableException(String message) {
        super(ErrorCode.SEAT_UNAVAILABLE, message);
    }
}
