package com.flightbooking.scheduled;

import com.flightbooking.entity.Booking;
import com.flightbooking.entity.Flight;
import com.flightbooking.enums.BookingStatus;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.service.SeatLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingRepository bookingRepository;
    private final SeatLockService seatLockService;

    public BookingExpiryJob(BookingRepository bookingRepository, SeatLockService seatLockService) {
        this.bookingRepository = bookingRepository;
        this.seatLockService = seatLockService;
    }

    @Scheduled(fixedRateString = "${app.expiry-job.rate-ms}")
    public void expireStaleBookings() {
        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndExpiresAtBefore(BookingStatus.SEAT_LOCKED, LocalDateTime.now());

        for (Booking booking : expiredBookings) {
            try {
                expireSingleBooking(booking);
            } catch (Exception e) {
                log.error("Failed to expire booking PNR={}: {}", booking.getPnr(), e.getMessage());
            }
        }

        if (!expiredBookings.isEmpty()) {
            log.info("Expired {} stale bookings", expiredBookings.size());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void expireSingleBooking(Booking booking) {
        for (Flight flight : booking.getFlights()) {
            seatLockService.releaseSeatsByBooking(booking.getId(), flight.getId());
            flight.setAvailableSeats(flight.getAvailableSeats() + booking.getPassengers().size());
        }
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setExpiresAt(null);
        log.info("Expired booking PNR={}, released {} seats", booking.getPnr(), booking.getPassengers().size());
    }
}
