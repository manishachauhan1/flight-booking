package com.flightbooking.exception;

import com.flightbooking.enums.ErrorCode;

public class BookingExpiredException extends BusinessException {
    public BookingExpiredException(String pnr) {
        super(ErrorCode.BOOKING_EXPIRED, "Booking " + pnr + " has expired. Seat lock is no longer valid.");
    }
}
