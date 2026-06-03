package com.flightbooking.service.search;

import java.time.LocalDateTime;

public record Stopover(
    String city,
    long layoverMinutes,
    LocalDateTime arrival,
    LocalDateTime departure
) {}
