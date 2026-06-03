package com.flightbooking;

import com.flightbooking.dto.*;
import com.flightbooking.enums.BookingStatus;
import com.flightbooking.enums.CancellationType;
import com.flightbooking.enums.ErrorCode;
import com.flightbooking.enums.PassengerType;
import com.flightbooking.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FlightBookingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testSearchFlights_directResults() {
        FlightSearchRequest request = new FlightSearchRequest("DEL", "BOM", LocalDate.of(2026, 6, 10), 1);

        ResponseEntity<FlightSearchResponse> response = restTemplate.exchange(
                "/api/v1/flights/search?source={source}&destination={destination}&date={date}&passengers={passengers}",
                HttpMethod.GET, null, FlightSearchResponse.class,
                request.getSource(), request.getDestination(), request.getDate(), request.getPassengers());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getTotalCount() > 0);

        boolean hasDirect = response.getBody().getResults().stream().anyMatch(FlightResultDTO::isDirect);
        assertTrue(hasDirect, "Should include direct flights");
    }

    @Test
    void testSearchFlights_connectingResults() {
        FlightSearchRequest request = new FlightSearchRequest("DEL", "MAA", LocalDate.of(2026, 6, 10), 1);

        ResponseEntity<FlightSearchResponse> response = restTemplate.exchange(
                "/api/v1/flights/search?source={source}&destination={destination}&date={date}&passengers={passengers}",
                HttpMethod.GET, null, FlightSearchResponse.class,
                request.getSource(), request.getDestination(), request.getDate(), request.getPassengers());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getTotalCount() > 0);

        boolean hasConnecting = response.getBody().getResults().stream().anyMatch(r -> !r.isDirect());
        assertTrue(hasConnecting, "Should include connecting flights for DEL->MAA");
    }

    @Test
    void testFullBookingFlow() {
        // 1. Search
        FlightSearchRequest searchReq = new FlightSearchRequest("DEL", "BOM", LocalDate.of(2026, 6, 10), 2);

        ResponseEntity<FlightSearchResponse> searchResponse = restTemplate.exchange(
                "/api/v1/flights/search?source={source}&destination={destination}&date={date}&passengers={passengers}",
                HttpMethod.GET, null, FlightSearchResponse.class,
                searchReq.getSource(), searchReq.getDestination(), searchReq.getDate(), searchReq.getPassengers());

        assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
        assertNotNull(searchResponse.getBody());
        assertFalse(searchResponse.getBody().getResults().isEmpty());

        FlightResultDTO selectedFlight = searchResponse.getBody().getResults().get(0);
        assertNotNull(selectedFlight.getPriceToken());

        // 2. Initiate booking
        BookingRequest bookingReq = new BookingRequest(
                List.of(selectedFlight.getLegs().get(0).flightId()),
                selectedFlight.getPriceToken(),
                List.of(
                        new PassengerDTO("Alice", 30, PassengerType.ADULT, "A1"),
                        new PassengerDTO("Bob", 28, PassengerType.ADULT, "A2")
                )
        );

        ResponseEntity<BookingResponse> bookingResponse = restTemplate.postForEntity(
                "/api/v1/bookings", bookingReq, BookingResponse.class);

        assertEquals(HttpStatus.CREATED, bookingResponse.getStatusCode());
        assertNotNull(bookingResponse.getBody());
        assertNotNull(bookingResponse.getBody().getPnr());
        assertEquals(BookingStatus.SEAT_LOCKED, bookingResponse.getBody().getStatus());
        assertNotNull(bookingResponse.getBody().getExpiresAt());

        String pnr = bookingResponse.getBody().getPnr();

        // 3. Confirm booking (no payment)
        ResponseEntity<BookingResponse> confirmResponse = restTemplate.postForEntity(
                "/api/v1/bookings/" + pnr + "/confirm", null, BookingResponse.class);

        assertEquals(HttpStatus.OK, confirmResponse.getStatusCode());
        assertNotNull(confirmResponse.getBody());
        assertEquals(BookingStatus.CONFIRMED, confirmResponse.getBody().getStatus());
        assertEquals(pnr, confirmResponse.getBody().getPnr());

        // 4. Get booking by PNR
        ResponseEntity<BookingResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/bookings/" + pnr, BookingResponse.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(BookingStatus.CONFIRMED, getResponse.getBody().getStatus());
        assertEquals(2, getResponse.getBody().getPassengers().size());

        // 5. Cancel booking
        CancellationRequest cancelReq = new CancellationRequest(CancellationType.FULL);

        ResponseEntity<CancellationResponse> cancelResponse = restTemplate.postForEntity(
                "/api/v1/bookings/" + pnr + "/cancel", cancelReq, CancellationResponse.class);

        assertEquals(HttpStatus.OK, cancelResponse.getStatusCode());
        assertNotNull(cancelResponse.getBody());
        assertEquals(BookingStatus.CANCELLED, cancelResponse.getBody().getStatus());
    }

    @Test
    void testBookingValidation_requiresAdult() {
        FlightSearchRequest searchReq = new FlightSearchRequest("DEL", "BOM", LocalDate.of(2026, 6, 10), 1);

        ResponseEntity<FlightSearchResponse> searchResponse = restTemplate.exchange(
                "/api/v1/flights/search?source={source}&destination={destination}&date={date}&passengers={passengers}",
                HttpMethod.GET, null, FlightSearchResponse.class,
                searchReq.getSource(), searchReq.getDestination(), searchReq.getDate(), 1);

        FlightResultDTO flight = searchResponse.getBody().getResults().get(0);

        BookingRequest bookingReq = new BookingRequest(
                List.of(flight.getLegs().get(0).flightId()),
                flight.getPriceToken(),
                List.of(new PassengerDTO("Infant", 1, PassengerType.INFANT, "B1"))
        );

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings", bookingReq, GlobalExceptionHandler.ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testBookingValidation_invalidPriceToken() {
        BookingRequest bookingReq = new BookingRequest(
                List.of(1L),
                "invalid-token",
                List.of(new PassengerDTO("Alice", 30, PassengerType.ADULT, "C1"))
        );

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings", bookingReq, GlobalExceptionHandler.ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(ErrorCode.PRICE_CHANGED, response.getBody().errorCode());
    }

    @Test
    void testSearchFlights_noResults() {
        ResponseEntity<FlightSearchResponse> response = restTemplate.exchange(
                "/api/v1/flights/search?source={source}&destination={destination}&date={date}",
                HttpMethod.GET, null, FlightSearchResponse.class,
                "XYZ", "ABC", LocalDate.of(2026, 6, 10));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getTotalCount());
    }
}
