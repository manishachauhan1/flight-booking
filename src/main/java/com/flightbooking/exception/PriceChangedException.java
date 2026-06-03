package com.flightbooking.exception;

import com.flightbooking.enums.ErrorCode;

public class PriceChangedException extends BusinessException {
    public PriceChangedException(double currentPrice) {
        super(ErrorCode.PRICE_CHANGED, "Price has changed. Current price: " + currentPrice);
    }
}
