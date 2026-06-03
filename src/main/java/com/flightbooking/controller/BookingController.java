package com.flightbooking.controller;

import com.flightbooking.dto.BookingRequest;
import com.flightbooking.dto.BookingResponse;
import com.flightbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.flightbooking.Constants.API_BOOKINGS;

@RestController
@RequestMapping(API_BOOKINGS)
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> proceedBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.proceedBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{pnr}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable String pnr) {
        BookingResponse response = bookingService.confirmBooking(pnr);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{pnr}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable String pnr) {
        BookingResponse response = bookingService.getBooking(pnr);
        return ResponseEntity.ok(response);
    }
}
