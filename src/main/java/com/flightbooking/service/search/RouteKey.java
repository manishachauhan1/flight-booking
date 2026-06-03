package com.flightbooking.service.search;

import java.time.LocalDate;

public record RouteKey(String source, String destination, LocalDate date) {}
