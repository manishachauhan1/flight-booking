package com.flightbooking.controller;

import com.flightbooking.dto.CancellationContext;
import com.flightbooking.dto.CancellationRequest;
import com.flightbooking.dto.CancellationResponse;
import com.flightbooking.service.CancellationHandler;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.flightbooking.Constants.API_BOOKINGS;

@RestController
@RequestMapping(API_BOOKINGS)
public class CancellationController {

    private final CancellationHandler fullCancellationHandler;
    private final CancellationHandler partialCancellationHandler;

    public CancellationController(
            @Qualifier("fullCancellationHandler") CancellationHandler fullCancellationHandler,
            @Qualifier("partialCancellationHandler") CancellationHandler partialCancellationHandler) {
        this.fullCancellationHandler = fullCancellationHandler;
        this.partialCancellationHandler = partialCancellationHandler;
    }

    @PostMapping("/{pnr}/cancel")
    public ResponseEntity<CancellationResponse> cancelBooking(
            @PathVariable String pnr,
            @Valid @RequestBody CancellationRequest request) {

        CancellationContext ctx = new CancellationContext(pnr, request);
        CancellationHandler handler = request.isPartial()
                ? partialCancellationHandler
                : fullCancellationHandler;
        return ResponseEntity.ok(handler.handle(ctx));
    }
}
