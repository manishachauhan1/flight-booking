package com.flightbooking.dto;

public record StopoverDTO(
        String city,
        long layoverMinutes,
        String arrival,
        String departure
) {}
