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
        ResponseEntity<FlightSearchResponse> response = search(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getTotalCount() > 0);
        assertTrue(response.getBody().getResults().stream().anyMatch(FlightResultDTO::isDirect));
    }

    @Test
    void testSearchFlights_connectingResults() {
        FlightSearchRequest request = new FlightSearchRequest("DEL", "MAA", LocalDate.of(2026, 6, 10), 1);
        ResponseEntity<FlightSearchResponse> response = search(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getTotalCount() > 0);
        assertTrue(response.getBody().getResults().stream().anyMatch(r -> !r.isDirect()));
    }

    @Test
    void testSearchFlights_directOnlyReturnsOnlyDirect() {
        ResponseEntity<FlightSearchResponse> response = restTemplate.exchange(
                "/api/v1/flights/search?source={source}&destination={destination}&date={date}&flightType={flightType}",
                HttpMethod.GET, null, FlightSearchResponse.class,
                "DEL", "MAA", LocalDate.of(2026, 6, 10), "DIRECT");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getResults().stream().allMatch(FlightResultDTO::isDirect));
    }

    @Test
    void testSearchFlights_noResults() {
        FlightSearchRequest request = new FlightSearchRequest("XYZ", "ABC", LocalDate.of(2026, 6, 10), 1);
        ResponseEntity<FlightSearchResponse> response = search(request);
        assertEquals(0, response.getBody().getTotalCount());
    }

    @Test
    void testFullBookingFlow() {
        String pnr = initiateBooking("DEL", "BOM", 2, "A1", "A2");

        ResponseEntity<BookingResponse> confirmResponse = restTemplate.postForEntity(
                "/api/v1/bookings/" + pnr + "/confirm", null, BookingResponse.class);
        assertEquals(HttpStatus.OK, confirmResponse.getStatusCode());
        assertEquals(BookingStatus.CONFIRMED, confirmResponse.getBody().getStatus());

        ResponseEntity<BookingResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/bookings/" + pnr, BookingResponse.class);
        assertEquals(BookingStatus.CONFIRMED, getResponse.getBody().getStatus());
        assertEquals(2, getResponse.getBody().getPassengers().size());

        CancellationRequest cancelReq = new CancellationRequest(CancellationType.FULL);
        ResponseEntity<CancellationResponse> cancelResponse = restTemplate.postForEntity(
                "/api/v1/bookings/" + pnr + "/cancel", cancelReq, CancellationResponse.class);
        assertEquals(HttpStatus.OK, cancelResponse.getStatusCode());
        assertEquals(BookingStatus.CANCELLED, cancelResponse.getBody().getStatus());
    }

    @Test
    void testPartialCancellation() {
        String pnr = initiateBooking("DEL", "BOM", 2, "C1", "C2");
        confirmBooking(pnr);

        ResponseEntity<BookingResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/bookings/" + pnr, BookingResponse.class);
        Long firstPassengerId = getResponse.getBody().getPassengers().get(0).getId();

        CancellationRequest cancelReq = new CancellationRequest(CancellationType.PARTIAL);
        cancelReq.setPassengerIds(List.of(firstPassengerId));

        ResponseEntity<CancellationResponse> cancelResponse = restTemplate.postForEntity(
                "/api/v1/bookings/" + pnr + "/cancel", cancelReq, CancellationResponse.class);
        assertEquals(HttpStatus.OK, cancelResponse.getStatusCode());

        ResponseEntity<BookingResponse> afterCancel = restTemplate.getForEntity(
                "/api/v1/bookings/" + pnr, BookingResponse.class);
        assertEquals(1, afterCancel.getBody().getPassengers().size());
        assertEquals(BookingStatus.CONFIRMED, afterCancel.getBody().getStatus());
    }

    @Test
    void testCancelNonExistentBooking() {
        CancellationRequest cancelReq = new CancellationRequest(CancellationType.FULL);
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings/INVALID/cancel", cancelReq, GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, response.getBody().errorCode());
    }

    @Test
    void testGetNonExistentBooking() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = restTemplate.getForEntity(
                "/api/v1/bookings/INVALID", GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, response.getBody().errorCode());
    }

    @Test
    void testConfirmNonExistentBooking() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings/INVALID/confirm", null, GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, response.getBody().errorCode());
    }

    @Test
    void testConfirmAlreadyCancelledBooking() {
        String pnr = initiateBooking("DEL", "BOM", 1, "D1");
        confirmBooking(pnr);
        cancelFull(pnr);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings/" + pnr + "/confirm", null, GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.ILLEGAL_STATE, response.getBody().errorCode());
    }

    @Test
    void testCancelAlreadyCancelledBooking() {
        String pnr = initiateBooking("DEL", "BOM", 1, "E1");
        confirmBooking(pnr);
        cancelFull(pnr);

        CancellationRequest cancelReq = new CancellationRequest(CancellationType.FULL);
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings/" + pnr + "/cancel", cancelReq, GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testBookingValidation_requiresAdult() {
        FlightResultDTO flight = getFirstFlight("DEL", "BOM");
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
    void testBookingValidation_infantExceedsAdult() {
        FlightResultDTO flight = getFirstFlight("DEL", "BOM");
        BookingRequest bookingReq = new BookingRequest(
                List.of(flight.getLegs().get(0).flightId()),
                flight.getPriceToken(),
                List.of(
                        new PassengerDTO("Adult", 30, PassengerType.ADULT, "F1"),
                        new PassengerDTO("Infant1", 1, PassengerType.INFANT, "F2"),
                        new PassengerDTO("Infant2", 1, PassengerType.INFANT, "F3")
                )
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
    void testBookingValidation_seatUnavailable() {
        ResponseEntity<FlightSearchResponse> searchResponse = search("DEL", "BOM", 1);
        FlightResultDTO flight = searchResponse.getBody().getResults().get(0);

        // Lock the seat by doing a first booking
        String pnr1 = initiateBooking("DEL", "BOM", 1, "F6");

        BookingRequest bookingReq = new BookingRequest(
                List.of(flight.getLegs().get(0).flightId()),
                flight.getPriceToken(),
                List.of(new PassengerDTO("Bob", 25, PassengerType.ADULT, "F6"))
        );

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings", bookingReq, GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testBookingWithAddOns() {
        ResponseEntity<FlightSearchResponse> searchResponse = search("DEL", "BOM", 1);
        FlightResultDTO flight = searchResponse.getBody().getResults().get(0);

        BookingRequest bookingReq = new BookingRequest(
                List.of(flight.getLegs().get(0).flightId()),
                flight.getPriceToken(),
                List.of(new PassengerDTO("Alice", 30, PassengerType.ADULT, "B5"))
        );
        AddOnsDTO addOns = new AddOnsDTO();
        addOns.setLuggageKg(10);
        addOns.setFood(true);
        addOns.setInsurance(true);
        bookingReq.setAddOns(addOns);

        ResponseEntity<BookingResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings", bookingReq, BookingResponse.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getPnr());
    }

    private ResponseEntity<FlightSearchResponse> search(String source, String dest, int passengers) {
        FlightSearchRequest request = new FlightSearchRequest(source, dest, LocalDate.of(2026, 6, 10), passengers);
        return search(request);
    }

    private ResponseEntity<FlightSearchResponse> search(FlightSearchRequest request) {
        return restTemplate.exchange(
                "/api/v1/flights/search?source={source}&destination={destination}&date={date}&passengers={passengers}",
                HttpMethod.GET, null, FlightSearchResponse.class,
                request.getSource(), request.getDestination(), request.getDate(), request.getPassengers());
    }

    private FlightResultDTO getFirstFlight(String source, String dest) {
        ResponseEntity<FlightSearchResponse> response = search(source, dest, 1);
        return response.getBody().getResults().get(0);
    }

    private String initiateBooking(String source, String dest, int passengers, String... seats) {
        FlightResultDTO flight = getFirstFlight(source, dest);

        List<PassengerDTO> passengerList = new java.util.ArrayList<>();
        for (int i = 0; i < passengers; i++) {
            passengerList.add(new PassengerDTO("Passenger" + i, 20 + i, PassengerType.ADULT, seats[i]));
        }

        BookingRequest bookingReq = new BookingRequest(
                List.of(flight.getLegs().get(0).flightId()),
                flight.getPriceToken(),
                passengerList
        );

        ResponseEntity<BookingResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings", bookingReq, BookingResponse.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody().getPnr();
    }

    private void confirmBooking(String pnr) {
        ResponseEntity<BookingResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings/" + pnr + "/confirm", null, BookingResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private void cancelFull(String pnr) {
        ResponseEntity<CancellationResponse> response = restTemplate.postForEntity(
                "/api/v1/bookings/" + pnr + "/cancel",
                new CancellationRequest(CancellationType.FULL),
                CancellationResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
