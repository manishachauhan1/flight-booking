package com.flightbooking.service;

import com.flightbooking.entity.Flight;
import com.flightbooking.entity.Seat;
import com.flightbooking.exception.SeatUnavailableException;
import com.flightbooking.repository.SeatRepository;
import com.flightbooking.testutil.TestJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SeatLockServiceTest {

    private Map<String, Seat> seatDb;
    private SeatLockService seatLockService;

    static class TestSeatRepo extends TestJpaRepository<Seat, Long> implements SeatRepository {
        final Map<String, Seat> db;

        TestSeatRepo(Map<String, Seat> db) { this.db = db; }

        @Override
        public Optional<Seat> findByFlightIdAndSeatNumberWithLock(Long flightId, String seatNumber) {
            return Optional.ofNullable(db.get(flightId + ":" + seatNumber));
        }

        @Override
        public List<Seat> findByFlightId(Long flightId) {
            return db.values().stream()
                    .filter(s -> s.getFlight() != null && flightId.equals(s.getFlight().getId()))
                    .collect(Collectors.toList());
        }

        @Override
        public <S extends Seat> S save(S entity) {
            if (entity.getFlight() != null && entity.getSeatNumber() != null) {
                db.put(entity.getFlight().getId() + ":" + entity.getSeatNumber(), entity);
            }
            return entity;
        }

        @Override
        public List<Seat> findAll() { return new ArrayList<>(db.values()); }

        @Override
        public long count() { return db.size(); }

        @Override
        public void deleteAll() { db.clear(); }
    }

    @BeforeEach
    void setUp() {
        seatDb = new HashMap<>();
        seatLockService = new SeatLockService(new TestSeatRepo(seatDb), 15);
    }

    @Test
    void lockAllSeatsForFlightLocksEachSeat() {
        Seat seat = createSeat("A1", 1L);
        seatDb.put("1:A1", seat);

        seatLockService.lockAllSeatsForFlight(1L, List.of("A1"), 100L);

        assertFalse(seat.getIsAvailable());
        assertEquals(100L, seat.getLockedByBookingId());
        assertNotNull(seat.getLockedUntil());
    }

    @Test
    void lockAllSeatsForFlightThrowsWhenSeatNotFound() {
        assertThrows(SeatUnavailableException.class,
                () -> seatLockService.lockAllSeatsForFlight(1L, List.of("Z9"), 100L));
    }

    @Test
    void lockAllSeatsForFlightThrowsWhenSeatNotBookable() {
        Seat seat = createSeat("A1", 1L);
        seat.lock(99L, 15);
        seatDb.put("1:A1", seat);

        assertThrows(SeatUnavailableException.class,
                () -> seatLockService.lockAllSeatsForFlight(1L, List.of("A1"), 100L));
    }

    @Test
    void confirmAllSeatsForFlightConfirmsEachSeat() {
        Seat seat = createSeat("A1", 1L);
        seatDb.put("1:A1", seat);

        seatLockService.confirmAllSeatsForFlight(1L, List.of("A1"));

        assertFalse(seat.getIsAvailable());
        assertNull(seat.getLockedByBookingId());
        assertNull(seat.getLockedUntil());
    }

    @Test
    void confirmAllSeatsForFlightSkipsMissingSeat() {
        assertDoesNotThrow(() -> seatLockService.confirmAllSeatsForFlight(1L, List.of("Z9")));
    }

    @Test
    void releaseSeatsByBookingReleasesMatchingSeats() {
        Seat seat1 = createSeat("A1", 1L);
        seat1.lock(100L, 15);
        Seat seat2 = createSeat("A2", 2L);
        seat2.lock(200L, 15);
        seatDb.put("1:A1", seat1);
        seatDb.put("2:A2", seat2);

        seatLockService.releaseSeatsByBooking(100L, 1L);

        assertTrue(seat1.getIsAvailable());
        assertNull(seat1.getLockedByBookingId());
        assertNull(seat1.getLockedUntil());
        assertFalse(seat2.getIsAvailable());
    }

    @Test
    void releaseSeatsByBookingAndNumbersReleasesEachSeat() {
        Seat seat = createSeat("A1", 1L);
        seat.lock(100L, 15);
        seatDb.put("1:A1", seat);

        seatLockService.releaseSeatsByBookingAndNumbers(1L, List.of("A1"));

        assertTrue(seat.getIsAvailable());
        assertNull(seat.getLockedByBookingId());
        assertNull(seat.getLockedUntil());
    }

    private Seat createSeat(String number, Long flightId) {
        Flight flight = new Flight();
        flight.setId(flightId);
        Seat seat = new Seat();
        seat.setFlight(flight);
        seat.setSeatNumber(number);
        seat.setIsAvailable(true);
        return seat;
    }
}
