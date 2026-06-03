package com.flightbooking.service;

import com.flightbooking.entity.Flight;
import com.flightbooking.exception.PriceChangedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService("test-secret-key");
    }

    @Test
    void generatePriceTokenReturnsNonNullToken() {
        Flight flight = createFlight(1L, 100.0);
        String token = pricingService.generatePriceToken(List.of(flight), 100.0);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void verifyPriceTokenDoesNotThrowForValidToken() {
        Flight flight = createFlight(1L, 100.0);
        String token = pricingService.generatePriceToken(List.of(flight), 100.0);
        assertDoesNotThrow(() -> pricingService.verifyPriceToken(List.of(flight), token));
    }

    @Test
    void verifyPriceTokenThrowsForInvalidToken() {
        Flight flight = createFlight(1L, 100.0);
        assertThrows(PriceChangedException.class,
                () -> pricingService.verifyPriceToken(List.of(flight), "invalid-token"));
    }

    @Test
    void verifyPriceTokenThrowsForWrongPrice() {
        Flight flight = createFlight(1L, 100.0);
        String token = pricingService.generatePriceToken(List.of(flight), 50.0);
        assertThrows(PriceChangedException.class,
                () -> pricingService.verifyPriceToken(List.of(flight), token));
    }

    @Test
    void generatePriceTokenIsDeterministic() {
        Flight f1 = createFlight(1L, 100.0);
        Flight f2 = createFlight(2L, 200.0);
        String token1 = pricingService.generatePriceToken(List.of(f1, f2), 300.0);
        String token2 = pricingService.generatePriceToken(List.of(f1, f2), 300.0);
        assertEquals(token1, token2);
    }

    @Test
    void verifyPriceTokenPassesForMultipleFlights() {
        Flight f1 = createFlight(1L, 100.0);
        Flight f2 = createFlight(2L, 200.0);
        String token = pricingService.generatePriceToken(List.of(f1, f2), 300.0);
        assertDoesNotThrow(() -> pricingService.verifyPriceToken(List.of(f1, f2), token));
    }

    @Test
    void verifyPriceTokenFailsForPriceChange() {
        Flight f1 = createFlight(1L, 100.0);
        String token = pricingService.generatePriceToken(List.of(f1), 100.0);
        f1.setPrice(150.0);
        assertThrows(PriceChangedException.class,
                () -> pricingService.verifyPriceToken(List.of(f1), token));
    }

    private Flight createFlight(Long id, double price) {
        Flight flight = new Flight();
        flight.setId(id);
        flight.setPrice(price);
        return flight;
    }
}
