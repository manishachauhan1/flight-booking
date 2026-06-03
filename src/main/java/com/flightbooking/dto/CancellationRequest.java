package com.flightbooking.dto;

import com.flightbooking.enums.CancellationType;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CancellationRequest {

    @NotNull(message = "Cancellation type is required")
    private CancellationType cancellationType;

    private List<Long> passengerIds;

    public CancellationRequest() {}

    public CancellationRequest(CancellationType cancellationType) {
        this.cancellationType = cancellationType;
    }

    public CancellationType getCancellationType() { return cancellationType; }
    public void setCancellationType(CancellationType cancellationType) { this.cancellationType = cancellationType; }
    public List<Long> getPassengerIds() { return passengerIds; }
    public void setPassengerIds(List<Long> passengerIds) { this.passengerIds = passengerIds; }

    public boolean isPartial() {
        return CancellationType.PARTIAL == cancellationType;
    }
}
