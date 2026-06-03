package com.flightbooking.service;

import com.flightbooking.dto.CancellationContext;
import com.flightbooking.dto.CancellationRequest;
import com.flightbooking.dto.CancellationResponse;
import com.flightbooking.entity.Booking;
import com.flightbooking.entity.Flight;
import com.flightbooking.entity.Passenger;
import com.flightbooking.enums.BookingStatus;
import com.flightbooking.enums.CancellationType;
import com.flightbooking.enums.PassengerType;
import com.flightbooking.exception.BookingNotFoundException;
import com.flightbooking.exception.CancellationInvalidException;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.service.impl.FullCancellationHandler;
import com.flightbooking.service.impl.PartialCancellationHandler;
import com.flightbooking.testutil.TestJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CancellationTemplateTest {

    private Map<String, Booking> bookingDb;
    private FullCancellationHandler fullHandler;
    private PartialCancellationHandler partialHandler;

    static class TestBookingRepo extends TestJpaRepository<Booking, Long> implements BookingRepository {
        final Map<String, Booking> db;

        TestBookingRepo(Map<String, Booking> db) { this.db = db; }

        @Override
        public Optional<Booking> findByPnr(String pnr) {
            return Optional.ofNullable(db.get(pnr));
        }

        @Override
        public List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime now) {
            return db.values().stream()
                    .filter(b -> b.getStatus() == status && b.getExpiresAt() != null && b.getExpiresAt().isBefore(now))
                    .collect(Collectors.toList());
        }

        @Override
        public <S extends Booking> S save(S entity) {
            db.put(entity.getPnr(), entity);
            return entity;
        }

        @Override
        public Optional<Booking> findById(Long id) {
            return db.values().stream().filter(b -> b.getId().equals(id)).findFirst();
        }

        @Override
        public boolean existsById(Long id) {
            return db.values().stream().anyMatch(b -> b.getId().equals(id));
        }

        @Override
        public List<Booking> findAll() {
            return new ArrayList<>(db.values());
        }

        @Override
        public long count() {
            return db.size();
        }

        @Override
        public void deleteById(Long id) {
            db.values().removeIf(b -> b.getId().equals(id));
        }

        @Override
        public void deleteAll() {
            db.clear();
        }
    }

    @BeforeEach
    void setUp() {
        bookingDb = new HashMap<>();

        SeatLockService seatLockService = new SeatLockService(null, 15) {
            @Override
            public void releaseSeatsByBooking(Long bookingId, Long flightId) {}

            @Override
            public void releaseSeatsByBookingAndNumbers(Long flightId, List<String> seatNumbers) {}
        };

        fullHandler = new FullCancellationHandler(new TestBookingRepo(bookingDb), seatLockService);
        partialHandler = new PartialCancellationHandler(new TestBookingRepo(bookingDb), seatLockService);
    }

    @Test
    void fullCancelSetsStatusToCancelled() {
        Booking booking = createConfirmedBooking(1L, "PNR123", 2);
        bookingDb.put("PNR123", booking);

        CancellationContext ctx = new CancellationContext("PNR123", new CancellationRequest(CancellationType.FULL));
        CancellationResponse response = fullHandler.handle(ctx);

        assertEquals("PNR123", response.getPnr());
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
    }

    @Test
    void fullCancelThrowsForNonExistentPnr() {
        CancellationContext ctx = new CancellationContext("INVALID", new CancellationRequest(CancellationType.FULL));
        assertThrows(BookingNotFoundException.class, () -> fullHandler.handle(ctx));
    }

    @Test
    void fullCancelThrowsForAlreadyCancelledBooking() {
        Booking booking = createConfirmedBooking(1L, "PNR123", 2);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingDb.put("PNR123", booking);

        CancellationContext ctx = new CancellationContext("PNR123", new CancellationRequest(CancellationType.FULL));
        assertThrows(CancellationInvalidException.class, () -> fullHandler.handle(ctx));
    }

    @Test
    void partialCancelRemovesPassengers() {
        Booking booking = createConfirmedBooking(1L, "PNR123", 3);
        bookingDb.put("PNR123", booking);

        Long passengerId = booking.getPassengers().get(0).getId();
        CancellationRequest request = new CancellationRequest(CancellationType.PARTIAL);
        request.setPassengerIds(List.of(passengerId));

        CancellationContext ctx = new CancellationContext("PNR123", request);
        CancellationResponse response = partialHandler.handle(ctx);

        assertEquals(2, booking.getPassengers().size());
        assertNotEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void partialCancelSetsCancelledWhenNoPassengersLeft() {
        Booking booking = createConfirmedBooking(1L, "PNR123", 1);
        bookingDb.put("PNR123", booking);

        Long passengerId = booking.getPassengers().get(0).getId();
        CancellationRequest request = new CancellationRequest(CancellationType.PARTIAL);
        request.setPassengerIds(List.of(passengerId));

        CancellationContext ctx = new CancellationContext("PNR123", request);
        CancellationResponse response = partialHandler.handle(ctx);

        assertEquals(0, booking.getPassengers().size());
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
    }

    @Test
    void partialCancelThrowsForNonExistentPassenger() {
        Booking booking = createConfirmedBooking(1L, "PNR123", 2);
        bookingDb.put("PNR123", booking);

        CancellationRequest request = new CancellationRequest(CancellationType.PARTIAL);
        request.setPassengerIds(List.of(999L));

        CancellationContext ctx = new CancellationContext("PNR123", request);
        assertThrows(CancellationInvalidException.class, () -> partialHandler.handle(ctx));
    }

    @Test
    void partialCancelThrowsForEmptyPassengerIds() {
        Booking booking = createConfirmedBooking(1L, "PNR123", 2);
        bookingDb.put("PNR123", booking);

        CancellationRequest request = new CancellationRequest(CancellationType.PARTIAL);
        request.setPassengerIds(List.of());

        CancellationContext ctx = new CancellationContext("PNR123", request);
        assertThrows(CancellationInvalidException.class, () -> partialHandler.handle(ctx));
    }

    private Booking createConfirmedBooking(Long bookingId, String pnr, int passengerCount) {
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setPnr(pnr);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(1000.0);
        booking.setCreatedAt(LocalDateTime.now());

        Flight flight = new Flight();
        flight.setId(1L);
        flight.setFlightNumber("FL001");
        flight.setSource("DEL");
        flight.setDestination("BOM");
        flight.setDepartureDate(LocalDateTime.now().plusDays(1).toLocalDate());
        flight.setDepartureTime(LocalDateTime.now().plusDays(1).toLocalTime());
        flight.setAvailableSeats(100);
        booking.setFlights(List.of(flight));

        List<Passenger> passengers = new ArrayList<>();
        for (int i = 0; i < passengerCount; i++) {
            Passenger p = new Passenger("Passenger" + i, 20 + i, PassengerType.ADULT, "A" + (i + 1));
            p.setId((long) (i + 1));
            p.setBooking(booking);
            passengers.add(p);
        }
        booking.setPassengers(passengers);

        return booking;
    }
}
