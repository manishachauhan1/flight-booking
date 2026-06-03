package com.flightbooking.service;

public interface StepHandler<T, U> {
    default U handle(T request) {
        return null;
    }
}
