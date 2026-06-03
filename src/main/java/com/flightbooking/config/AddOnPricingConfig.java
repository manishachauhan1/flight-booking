package com.flightbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.addon")
public class AddOnPricingConfig {

    private double luggagePricePerKg = 2.0;
    private double foodPrice = 15.0;
    private double insurancePrice = 10.0;

    public double getLuggagePricePerKg() { return luggagePricePerKg; }
    public void setLuggagePricePerKg(double luggagePricePerKg) { this.luggagePricePerKg = luggagePricePerKg; }
    public double getFoodPrice() { return foodPrice; }
    public void setFoodPrice(double foodPrice) { this.foodPrice = foodPrice; }
    public double getInsurancePrice() { return insurancePrice; }
    public void setInsurancePrice(double insurancePrice) { this.insurancePrice = insurancePrice; }
}
