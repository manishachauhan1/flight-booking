package com.flightbooking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BookingRequest {

    @NotEmpty(message = "At least one flight ID is required")
    private List<@NotNull Long> flightIds;

    @NotBlank(message = "Price token is required")
    private String priceToken;

    @NotEmpty(message = "At least one passenger is required")
    @Valid
    private List<PassengerDTO> passengers;

    private AddOnsDTO addOns;

    public BookingRequest() {}

    public BookingRequest(List<Long> flightIds, String priceToken, List<PassengerDTO> passengers) {
        this.flightIds = flightIds;
        this.priceToken = priceToken;
        this.passengers = passengers;
    }

    public List<Long> getFlightIds() { return flightIds; }
    public void setFlightIds(List<Long> flightIds) { this.flightIds = flightIds; }
    public String getPriceToken() { return priceToken; }
    public void setPriceToken(String priceToken) { this.priceToken = priceToken; }
    public List<PassengerDTO> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerDTO> passengers) { this.passengers = passengers; }
    public AddOnsDTO getAddOns() { return addOns; }
    public void setAddOns(AddOnsDTO addOns) { this.addOns = addOns; }
}
