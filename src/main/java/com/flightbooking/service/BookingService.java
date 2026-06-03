package com.flightbooking.service;

import com.flightbooking.config.AddOnPricingConfig;
import com.flightbooking.dto.*;
import com.flightbooking.entity.*;
import com.flightbooking.enums.BookingStatus;
import com.flightbooking.enums.PassengerType;
import com.flightbooking.exception.*;
import com.flightbooking.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.flightbooking.Constants.MSG_ADULT_REQUIRED;
import static com.flightbooking.Constants.MSG_INFANT_LIMIT;
import static com.flightbooking.Constants.MSG_PASSENGER_REQUIRED;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final SeatLockService seatLockService;
    private final PricingService pricingService;
    private final AddOnPricingConfig addOnPricingConfig;
    private final int lockDurationMinutes;
    private final int maxPassengers;

    public BookingService(BookingRepository bookingRepository, FlightRepository flightRepository,
                          SeatLockService seatLockService, PricingService pricingService,
                          AddOnPricingConfig addOnPricingConfig,
                          @Value("${app.booking.lock-duration-minutes}") int lockDurationMinutes,
                          @Value("${app.booking.max-passengers}") int maxPassengers) {
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
        this.seatLockService = seatLockService;
        this.pricingService = pricingService;
        this.addOnPricingConfig = addOnPricingConfig;
        this.lockDurationMinutes = lockDurationMinutes;
        this.maxPassengers = maxPassengers;
    }

    @Transactional
    public BookingResponse initiateBooking(BookingRequest request) {
        List<Flight> flights = flightRepository.findAllById(request.getFlightIds());
        if (flights.size() != request.getFlightIds().size()) {
            throw new FlightNotFoundException(request.getFlightIds().get(0));
        }

        pricingService.verifyPriceToken(flights, request.getPriceToken());
        validatePassengers(request.getPassengers());

        double basePrice = flights.stream().mapToDouble(Flight::getPrice).sum();
        double addOnPrice = calculateAddOnPrice(request.getAddOns(), request.getPassengers().size());

        Booking booking = new Booking(
                Booking.generatePnr(),
                BookingStatus.INITIATED,
                basePrice + addOnPrice,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(lockDurationMinutes)
        );
        flights.forEach(booking::addFlight);

        for (PassengerDTO p : request.getPassengers()) {
            Passenger passenger = new Passenger(p.getName(), p.getAge(), p.getType(), p.getSeatNumber());
            booking.addPassenger(passenger);
        }

        booking = bookingRepository.save(booking);

        for (Flight flight : flights) {
            List<String> seatNumbers = request.getPassengers().stream()
                    .map(PassengerDTO::getSeatNumber)
                    .collect(Collectors.toList());
            seatLockService.lockAllSeatsForFlight(flight.getId(), seatNumbers, booking.getId());
            flight.setAvailableSeats(flight.getAvailableSeats() - request.getPassengers().size());
        }

        booking.setStatus(BookingStatus.SEAT_LOCKED);

        log.info("Booking initiated: PNR={}, flights={}, amount={}",
                booking.getPnr(), request.getFlightIds(), booking.getTotalAmount());
        return new BookingResponse(booking);
    }

    @Transactional
    @Retryable(
        value = ObjectOptimisticLockingFailureException.class,
        maxAttemptsExpression = "${app.retry.max-attempts}",
        backoff = @Backoff(delayExpression = "${app.retry.backoff-delay-ms}")
    )
    public BookingResponse confirmBooking(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new BookingNotFoundException(pnr));

        if (booking.getStatus() != BookingStatus.SEAT_LOCKED) {
            throw new IllegalBookingStateException(pnr, booking.getStatus(), BookingStatus.CONFIRMED);
        }

        if (booking.isExpired()) {
            expireBooking(booking);
            throw new BookingExpiredException(pnr);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null);

        for (Flight flight : booking.getFlights()) {
            List<String> seatNumbers = booking.getPassengers().stream()
                    .map(Passenger::getSeatNumber)
                    .collect(Collectors.toList());
            seatLockService.confirmAllSeatsForFlight(flight.getId(), seatNumbers);
        }

        log.info("Booking confirmed: PNR={}", pnr);
        return new BookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new BookingNotFoundException(pnr));
        return new BookingResponse(booking);
    }

    private void validatePassengers(List<PassengerDTO> passengers) {
        if (passengers == null || passengers.isEmpty()) {
            throw new IllegalArgumentException(MSG_PASSENGER_REQUIRED);
        }
        if (passengers.size() > maxPassengers) {
            throw new IllegalArgumentException("Maximum " + maxPassengers + " passengers per booking");
        }
        boolean hasAdult = passengers.stream().anyMatch(p -> p.getType() == PassengerType.ADULT);
        if (!hasAdult) {
            throw new IllegalArgumentException(MSG_ADULT_REQUIRED);
        }
        long adultCount = passengers.stream().filter(p -> p.getType() == PassengerType.ADULT).count();
        long infantCount = passengers.stream().filter(p -> p.getType() == PassengerType.INFANT).count();
        if (infantCount > adultCount) {
            throw new IllegalArgumentException(MSG_INFANT_LIMIT);
        }
    }

    private double calculateAddOnPrice(AddOnsDTO addOns, int passengerCount) {
        if (addOns == null) return 0;
        double total = 0;
        if (addOns.getLuggageKg() != null && addOns.getLuggageKg() > 0) {
            total += addOns.getLuggageKg() * addOnPricingConfig.getLuggagePricePerKg() * passengerCount;
        }
        if (Boolean.TRUE.equals(addOns.getFood())) {
            total += addOnPricingConfig.getFoodPrice() * passengerCount;
        }
        if (Boolean.TRUE.equals(addOns.getInsurance())) {
            total += addOnPricingConfig.getInsurancePrice() * passengerCount;
        }
        return total;
    }

    private void expireBooking(Booking booking) {
        booking.setStatus(BookingStatus.EXPIRED);
        for (Flight flight : booking.getFlights()) {
            seatLockService.releaseSeatsByBooking(booking.getId(), flight.getId());
            flight.setAvailableSeats(flight.getAvailableSeats() + booking.getPassengers().size());
        }
    }
}
