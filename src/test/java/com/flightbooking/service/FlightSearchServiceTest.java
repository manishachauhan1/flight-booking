package com.flightbooking.service;

import com.flightbooking.dto.FlightSearchRequest;
import com.flightbooking.dto.FlightSearchResponse;
import com.flightbooking.entity.Flight;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.testutil.TestJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FlightSearchServiceTest {

    private Map<Long, Flight> flightDb;
    private FlightSearchService flightSearchService;

    static class TestFlightRepo extends TestJpaRepository<Flight, Long> implements FlightRepository {
        final Map<Long, Flight> db;

        TestFlightRepo(Map<Long, Flight> db) { this.db = db; }

        @Override
        public List<Flight> findBySourceAndDestinationAndDepartureDate(String source, String destination, LocalDate departureDate) {
            return db.values().stream()
                    .filter(f -> f.getSource().equals(source)
                            && f.getDestination().equals(destination)
                            && f.getDepartureDate().equals(departureDate))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Flight> findBySourceAndDepartureDate(String source, LocalDate departureDate) {
            return db.values().stream()
                    .filter(f -> f.getSource().equals(source) && f.getDepartureDate().equals(departureDate))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Flight> findByDestinationAndDepartureDate(String destination, LocalDate departureDate) {
            return db.values().stream()
                    .filter(f -> f.getDestination().equals(destination) && f.getDepartureDate().equals(departureDate))
                    .collect(Collectors.toList());
        }

        @Override
        public <S extends Flight> S save(S entity) {
            db.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<Flight> findById(Long id) {
            return Optional.ofNullable(db.get(id));
        }

        @Override
        public boolean existsById(Long id) {
            return db.containsKey(id);
        }

        @Override
        public List<Flight> findAll() {
            return new ArrayList<>(db.values());
        }

        @Override
        public long count() {
            return db.size();
        }

        @Override
        public void deleteById(Long id) {
            db.remove(id);
        }

        @Override
        public void delete(Flight entity) {
            db.values().remove(entity);
        }

        @Override
        public void deleteAll(Iterable<? extends Flight> entities) {
            for (Flight e : entities) db.values().remove(e);
        }

        @Override
        public void deleteAll() {
            db.clear();
        }
    }

    @BeforeEach
    void setUp() {
        flightDb = new HashMap<>();
        PricingService pricingService = new PricingService("test-secret-key");
        flightSearchService = new FlightSearchService(new TestFlightRepo(flightDb), pricingService, 2, 60, 480);
    }

    @Test
    void searchReturnsDirectFlights() {
        Flight flight = createFlight(1L, "DEL", "BOM", 200, 100.0);
        flightDb.put(1L, flight);
        flightSearchService.initializeRouteCache();

        FlightSearchRequest request = new FlightSearchRequest("DEL", "BOM", LocalDate.of(2026, 6, 10), 1);
        FlightSearchResponse response = flightSearchService.search(request);

        assertEquals(1, response.getTotalCount());
        assertTrue(response.getResults().get(0).isDirect());
    }

    @Test
    void searchReturnsConnectingFlights() {
        Flight leg1 = createFlight(1L, "DEL", "BOM", 200, 100.0);
        leg1.setDepartureDate(LocalDate.of(2026, 6, 10));
        leg1.setDepartureTime(LocalTime.of(8, 0));
        leg1.setArrivalDate(LocalDate.of(2026, 6, 10));
        leg1.setArrivalTime(LocalTime.of(10, 0));

        Flight leg2 = createFlight(2L, "BOM", "MAA", 200, 100.0);
        leg2.setDepartureDate(LocalDate.of(2026, 6, 10));
        leg2.setDepartureTime(LocalTime.of(11, 30));
        leg2.setArrivalDate(LocalDate.of(2026, 6, 10));
        leg2.setArrivalTime(LocalTime.of(13, 0));

        flightDb.put(1L, leg1);
        flightDb.put(2L, leg2);
        flightSearchService.initializeRouteCache();

        FlightSearchRequest request = new FlightSearchRequest("DEL", "MAA", LocalDate.of(2026, 6, 10), 1);
        FlightSearchResponse response = flightSearchService.search(request);

        assertTrue(response.getTotalCount() > 0);
        assertTrue(response.getResults().stream().anyMatch(r -> !r.isDirect()));
    }

    @Test
    void searchDirectOnlyReturnsNoConnecting() {
        Flight flight = createFlight(1L, "DEL", "BOM", 200, 100.0);
        flightDb.put(1L, flight);
        flightSearchService.initializeRouteCache();

        FlightSearchRequest request = new FlightSearchRequest("DEL", "BOM", LocalDate.of(2026, 6, 10), 1);
        request.setFlightType("DIRECT");
        FlightSearchResponse response = flightSearchService.search(request);

        assertEquals(1, response.getTotalCount());
        assertTrue(response.getResults().get(0).isDirect());
    }

    @Test
    void searchNoResultsReturnsEmpty() {
        flightSearchService.initializeRouteCache();

        FlightSearchRequest request = new FlightSearchRequest("XYZ", "ABC", LocalDate.of(2026, 6, 10), 1);
        FlightSearchResponse response = flightSearchService.search(request);

        assertEquals(0, response.getTotalCount());
    }

    @Test
    void searchFiltersByAvailableSeats() {
        Flight flight = createFlight(1L, "DEL", "BOM", 0, 100.0);
        flightDb.put(1L, flight);
        flightSearchService.initializeRouteCache();

        FlightSearchRequest request = new FlightSearchRequest("DEL", "BOM", LocalDate.of(2026, 6, 10), 1);
        FlightSearchResponse response = flightSearchService.search(request);

        assertEquals(0, response.getTotalCount());
    }

    @Test
    void searchConnectingFiltersByLayover() {
        Flight leg1 = createFlight(1L, "DEL", "BOM", 200, 100.0);
        leg1.setDepartureDate(LocalDate.of(2026, 6, 10));
        leg1.setDepartureTime(LocalTime.of(8, 0));
        leg1.setArrivalDate(LocalDate.of(2026, 6, 10));
        leg1.setArrivalTime(LocalTime.of(10, 0));

        Flight leg2 = createFlight(2L, "BOM", "MAA", 200, 100.0);
        leg2.setDepartureDate(LocalDate.of(2026, 6, 10));
        leg2.setDepartureTime(LocalTime.of(10, 30));
        leg2.setArrivalDate(LocalDate.of(2026, 6, 10));
        leg2.setArrivalTime(LocalTime.of(12, 0));

        flightDb.put(1L, leg1);
        flightDb.put(2L, leg2);
        flightSearchService.initializeRouteCache();

        FlightSearchRequest request = new FlightSearchRequest("DEL", "MAA", LocalDate.of(2026, 6, 10), 1);
        FlightSearchResponse response = flightSearchService.search(request);

        assertEquals(0, response.getTotalCount());
    }

    @Test
    void searchReturnsMultiStopRoutes() {
        Flight leg1 = createFlight(1L, "DEL", "BOM", 200, 100.0);
        leg1.setDepartureDate(LocalDate.of(2026, 6, 10));
        leg1.setDepartureTime(LocalTime.of(8, 0));
        leg1.setArrivalDate(LocalDate.of(2026, 6, 10));
        leg1.setArrivalTime(LocalTime.of(10, 0));

        Flight leg2 = createFlight(2L, "BOM", "CCU", 200, 80.0);
        leg2.setDepartureDate(LocalDate.of(2026, 6, 10));
        leg2.setDepartureTime(LocalTime.of(13, 0));
        leg2.setArrivalDate(LocalDate.of(2026, 6, 10));
        leg2.setArrivalTime(LocalTime.of(14, 30));

        Flight leg3 = createFlight(3L, "CCU", "MAA", 200, 120.0);
        leg3.setDepartureDate(LocalDate.of(2026, 6, 10));
        leg3.setDepartureTime(LocalTime.of(16, 0));
        leg3.setArrivalDate(LocalDate.of(2026, 6, 10));
        leg3.setArrivalTime(LocalTime.of(18, 0));

        flightDb.put(1L, leg1);
        flightDb.put(2L, leg2);
        flightDb.put(3L, leg3);
        flightSearchService.initializeRouteCache();

        FlightSearchRequest request = new FlightSearchRequest("DEL", "MAA", LocalDate.of(2026, 6, 10), 1);
        FlightSearchResponse response = flightSearchService.search(request);

        assertTrue(response.getTotalCount() > 0);
        boolean hasMultiStop = response.getResults().stream()
                .anyMatch(r -> r.getLegs().size() >= 3);
        assertTrue(hasMultiStop, "Should include 2-stop route with 3 legs");
    }

    private Flight createFlight(Long id, String source, String dest, int availableSeats, double price) {
        Flight flight = new Flight();
        flight.setId(id);
        flight.setFlightNumber("TEST" + id);
        flight.setAirline("TestAir");
        flight.setSource(source);
        flight.setDestination(dest);
        flight.setDepartureDate(LocalDate.of(2026, 6, 10));
        flight.setDepartureTime(LocalTime.of(8, 0));
        flight.setArrivalDate(LocalDate.of(2026, 6, 10));
        flight.setArrivalTime(LocalTime.of(10, 0));
        flight.setTotalSeats(200);
        flight.setAvailableSeats(availableSeats);
        flight.setPrice(price);
        return flight;
    }
}
