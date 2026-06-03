package com.flightbooking.exception;

import com.flightbooking.enums.ErrorCode;

public class FlightNotFoundException extends BusinessException {
    public FlightNotFoundException(String flightNumber) {
        super(ErrorCode.FLIGHT_NOT_FOUND, "Flight not found: " + flightNumber);
    }

    public FlightNotFoundException(Long flightId) {
        super(ErrorCode.FLIGHT_NOT_FOUND, "Flight not found with id: " + flightId);
    }
}
