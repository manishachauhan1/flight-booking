package com.flightbooking.dto;

import com.flightbooking.enums.PassengerType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PassengerDTO {

    private Long id;

    @NotBlank(message = "Passenger name is required")
    private String name;

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be non-negative")
    private Integer age;

    @NotNull(message = "Passenger type is required")
    private PassengerType type;

    private String seatNumber;

    public PassengerDTO() {}

    public PassengerDTO(String name, Integer age, PassengerType type, String seatNumber) {
        this.name = name;
        this.age = age;
        this.type = type;
        this.seatNumber = seatNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public PassengerType getType() { return type; }
    public void setType(PassengerType type) { this.type = type; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
}
