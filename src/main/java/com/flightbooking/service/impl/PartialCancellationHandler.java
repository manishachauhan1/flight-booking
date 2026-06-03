package com.flightbooking.service.impl;

import com.flightbooking.dto.CancellationRequest;
import com.flightbooking.entity.Booking;
import com.flightbooking.enums.BookingStatus;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.service.CancellationTemplate;
import com.flightbooking.service.SeatLockService;
import org.springframework.stereotype.Service;

@Service
public class PartialCancellationHandler extends CancellationTemplate {

    public PartialCancellationHandler(BookingRepository bookingRepository, SeatLockService seatLockService) {
        super(bookingRepository, seatLockService);
    }

    @Override
    protected void updateAfterCancellation(Booking booking, CancellationRequest request) {
        booking.getPassengers().removeIf(p -> request.getPassengerIds().contains(p.getId()));
        if (booking.getPassengers().isEmpty()) {
            booking.setStatus(BookingStatus.CANCELLED);
        }
    }
}
