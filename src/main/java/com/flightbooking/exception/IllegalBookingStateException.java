package com.flightbooking.exception;

import com.flightbooking.enums.BookingStatus;
import com.flightbooking.enums.ErrorCode;

public class IllegalBookingStateException extends BusinessException {
    public IllegalBookingStateException(String pnr, BookingStatus current, BookingStatus target) {
        super(ErrorCode.ILLEGAL_STATE, "Cannot transition booking " + pnr + " from " + current + " to " + target);
    }
}
