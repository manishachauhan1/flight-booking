package com.flightbooking.exception;

import com.flightbooking.enums.ErrorCode;

public class CancellationInvalidException extends BusinessException {
    public CancellationInvalidException(String message) {
        super(ErrorCode.CANCELLATION_INVALID, message);
    }
}
