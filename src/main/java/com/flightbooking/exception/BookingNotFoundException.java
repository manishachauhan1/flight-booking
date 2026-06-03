package com.flightbooking.exception;

import com.flightbooking.enums.ErrorCode;

public class BookingNotFoundException extends BusinessException {
    public BookingNotFoundException(String pnr) {
        super(ErrorCode.BOOKING_NOT_FOUND, "Booking not found with PNR: " + pnr);
    }
}
