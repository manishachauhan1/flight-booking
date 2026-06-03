package com.flightbooking.repository;

import com.flightbooking.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long>, JpaSpecificationExecutor<Flight> {

    List<Flight> findBySourceAndDestinationAndDepartureDate(String source, String destination, LocalDate departureDate);

    List<Flight> findBySourceAndDepartureDate(String source, LocalDate departureDate);

    List<Flight> findByDestinationAndDepartureDate(String destination, LocalDate departureDate);
}
