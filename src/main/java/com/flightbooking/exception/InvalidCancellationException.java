package com.flightbooking.exception;

import com.flightbooking.enums.ErrorCode;

public class InvalidCancellationException extends BusinessException {
    public InvalidCancellationException(String message) {
        super(ErrorCode.CANCELLATION_INVALID, message);
    }
}
