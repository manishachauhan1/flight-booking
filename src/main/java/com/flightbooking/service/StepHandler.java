package com.flightbooking.service;

// Generic step handler interface for the Template Method pattern.
// T = input context, U = response. The handle() method has a default
// returning null so subtypes only override what they need.
public interface StepHandler<T, U> {
    default U handle(T request) {
        return null;
    }
}
