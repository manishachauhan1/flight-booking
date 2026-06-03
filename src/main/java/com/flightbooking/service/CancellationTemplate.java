package com.flightbooking.service;

import com.flightbooking.dto.CancellationContext;
import com.flightbooking.dto.CancellationRequest;
import com.flightbooking.dto.CancellationResponse;
import com.flightbooking.entity.Booking;
import com.flightbooking.entity.Flight;
import com.flightbooking.entity.Passenger;
import com.flightbooking.enums.BookingStatus;
import com.flightbooking.exception.BookingNotFoundException;
import com.flightbooking.exception.InvalidCancellationException;
import com.flightbooking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.flightbooking.Constants.CANCEL_MSG;

// Template Method: handle() defines the skeleton (find → validate → release → update).
// Subclasses (FullCancellationHandler, PartialCancellationHandler) override the
// abstract updateAfterCancellation() step.
public abstract class CancellationTemplate implements CancellationHandler {

    private static final Logger log = LoggerFactory.getLogger(CancellationTemplate.class);

    protected final BookingRepository bookingRepository;
    protected final SeatLockService seatLockService;

    protected CancellationTemplate(BookingRepository bookingRepository, SeatLockService seatLockService) {
        this.bookingRepository = bookingRepository;
        this.seatLockService = seatLockService;
    }

    @Override
    @Transactional
    public CancellationResponse handle(CancellationContext ctx) {
        String pnr = ctx.pnr();
        CancellationRequest request = ctx.request();
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new BookingNotFoundException(pnr));

        validateCancellation(booking, request);

        releaseSeats(booking, request);
        updateAfterCancellation(booking, request);

        log.info("Cancellation completed: PNR={}, type={}", pnr, request.getCancellationType());
        return new CancellationResponse(pnr, booking.getStatus(), CANCEL_MSG);
    }

    // Validates: booking status allows cancellation, no flights have departed,
    // and (for partial) all passenger IDs exist in the booking.
    protected void validateCancellation(Booking booking, CancellationRequest request) {
        if (!booking.canTransitionTo(BookingStatus.CANCELLED)) {
            throw new InvalidCancellationException(
                    "Booking " + booking.getPnr() + " is in " + booking.getStatus() + " state and cannot be cancelled");
        }

        boolean allFlightsDeparted = booking.getFlights().stream()
                .allMatch(f -> f.getDepartureDateTime().isBefore(LocalDateTime.now()));
        if (allFlightsDeparted) {
            throw new InvalidCancellationException("Cannot cancel booking after departure");
        }

        if (request.isPartial()) {
            if (request.getPassengerIds() == null || request.getPassengerIds().isEmpty()) {
                throw new InvalidCancellationException("Passenger IDs required for partial cancellation");
            }
            for (Long pid : request.getPassengerIds()) {
                boolean exists = booking.getPassengers().stream()
                        .anyMatch(p -> p.getId().equals(pid));
                if (!exists) {
                    throw new InvalidCancellationException("Passenger " + pid + " not found in booking");
                }
            }
        }
    }

    protected void releaseSeats(Booking booking, CancellationRequest request) {
        for (Flight flight : booking.getFlights()) {
            if (request.isPartial()) {
                List<String> seatNumbers = booking.getPassengers().stream()
                        .filter(p -> request.getPassengerIds().contains(p.getId()))
                        .map(Passenger::getSeatNumber)
                        .collect(Collectors.toList());
                seatLockService.releaseSeatsByBookingAndNumbers(flight.getId(), seatNumbers);
                flight.setAvailableSeats(flight.getAvailableSeats() + seatNumbers.size());
            } else {
                seatLockService.releaseSeatsByBooking(booking.getId(), flight.getId());
                flight.setAvailableSeats(flight.getAvailableSeats() + booking.getPassengers().size());
            }
        }
    }

    protected abstract void updateAfterCancellation(Booking booking, CancellationRequest request);
}
