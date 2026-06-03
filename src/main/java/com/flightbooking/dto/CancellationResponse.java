package com.flightbooking.dto;

import com.flightbooking.enums.BookingStatus;

public class CancellationResponse {

    private String pnr;
    private BookingStatus status;
    private String message;

    public CancellationResponse() {}

    public CancellationResponse(String pnr, BookingStatus status, String message) {
        this.pnr = pnr;
        this.status = status;
        this.message = message;
    }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
