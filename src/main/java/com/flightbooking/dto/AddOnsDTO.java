package com.flightbooking.dto;

public class AddOnsDTO {

    private Integer luggageKg;
    private Boolean food;
    private Boolean insurance;

    public AddOnsDTO() {}

    public Integer getLuggageKg() { return luggageKg; }
    public void setLuggageKg(Integer luggageKg) { this.luggageKg = luggageKg; }
    public Boolean getFood() { return food; }
    public void setFood(Boolean food) { this.food = food; }
    public Boolean getInsurance() { return insurance; }
    public void setInsurance(Boolean insurance) { this.insurance = insurance; }
}
