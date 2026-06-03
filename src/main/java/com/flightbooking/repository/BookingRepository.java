package com.flightbooking.repository;

import com.flightbooking.entity.Booking;
import com.flightbooking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByPnr(String pnr);

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime now);
}
