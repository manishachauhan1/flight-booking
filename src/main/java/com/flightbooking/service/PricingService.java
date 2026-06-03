package com.flightbooking.service;

import com.flightbooking.entity.Flight;
import com.flightbooking.exception.PriceChangedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static com.flightbooking.Constants.HMAC_ALGORITHM;
import static com.flightbooking.Constants.PRICE_FORMAT;

// Generates and verifies HMAC-SHA256 price tokens.
// Token = HMAC(sorted flight IDs + "|" + total price, secret).
// Prevents price-manipulation: user sees a price during search, token binds them
// to that price. If prices change by booking time, verification fails → 409.
@Service
public class PricingService {

    private final String secret;

    public PricingService(@Value("${app.price-token-secret}") String secret) {
        this.secret = secret;
    }

    public String generatePriceToken(List<Flight> flights, double basePrice) {
        String data = flights.stream()
                .map(f -> f.getId().toString())
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("")
                + "|" + String.format(PRICE_FORMAT, basePrice);
        return hmac(data);
    }

    public void verifyPriceToken(List<Flight> flights, String token) {
        double currentPrice = flights.stream().mapToDouble(Flight::getPrice).sum();
        String expected = generatePriceToken(flights, currentPrice);
        if (!expected.equals(token)) {
            throw new PriceChangedException(currentPrice);
        }
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] bytes = mac.doFinal(data.getBytes());
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }
}
