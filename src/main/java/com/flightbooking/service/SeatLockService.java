package com.flightbooking.service;

import com.flightbooking.entity.Seat;
import com.flightbooking.exception.SeatUnavailableException;
import com.flightbooking.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

// Manages seat lifecycle: lock (PESSIMISTIC_WRITE), confirm, release.
// Each lock call executes SELECT ... FOR UPDATE on the seat row,
// preventing concurrent bookings of the same seat.
@Service
public class SeatLockService {

    private static final Logger log = LoggerFactory.getLogger(SeatLockService.class);

    private final SeatRepository seatRepository;
    private final int lockDurationMinutes;

    public SeatLockService(SeatRepository seatRepository,
                           @Value("${app.seat-lock-duration-minutes}") int lockDurationMinutes) {
        this.seatRepository = seatRepository;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    public void lockAllSeatsForFlight(Long flightId, List<String> seatNumbers, Long bookingId) {
        for (String seatNumber : seatNumbers) {
            Seat seat = seatRepository.findByFlightIdAndSeatNumberWithLock(flightId, seatNumber)
                    .orElseThrow(() -> new SeatUnavailableException(seatNumber, String.valueOf(flightId)));
            if (!seat.isBookable()) {
                throw new SeatUnavailableException(
                        "Seat " + seatNumber + " is currently locked or already booked");
            }
            seat.lock(bookingId, lockDurationMinutes);
        }
        log.info("Locked {} seats for booking {} on flight {}", seatNumbers.size(), bookingId, flightId);
    }

    public void confirmAllSeatsForFlight(Long flightId, List<String> seatNumbers) {
        for (String seatNumber : seatNumbers) {
            seatRepository.findByFlightIdAndSeatNumberWithLock(flightId, seatNumber)
                    .ifPresent(Seat::confirm);
        }
    }

    public void releaseSeatsByBooking(Long bookingId, Long flightId) {
        List<Seat> seats = seatRepository.findByFlightId(flightId);
        for (Seat seat : seats) {
            if (bookingId.equals(seat.getLockedByBookingId())) {
                seat.release();
            }
        }
    }

    public void releaseSeatsByBookingAndNumbers(Long flightId, List<String> seatNumbers) {
        for (String seatNumber : seatNumbers) {
            seatRepository.findByFlightIdAndSeatNumberWithLock(flightId, seatNumber)
                    .ifPresent(Seat::release);
        }
    }
}
