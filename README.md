# Flight Booking System

A production-grade Spring Boot REST API for searching flights, booking seats with pessimistic locking, and managing cancellations using Template Method, State, and Strategy design patterns. Uses H2 in-memory database — zero setup required.

## Table of Contents

- [Problem Statement](#problem-statement)
- [Scope](#scope)
- [Assumptions](#assumptions)
- [High-Level Architecture](#high-level-architecture)
- [Architecture Diagram](#architecture-diagram)
- [Technology Stack](#technology-stack)
- [API Specifications](#api-specifications)
- [Low-Level Design](#low-level-design)
  - [Design Patterns](#design-patterns)
  - [Booking State Machine](#booking-state-machine)
  - [Cancellation Flow (Template Method)](#cancellation-flow-template-method)
  - [Price Token Strategy](#price-token-strategy)
  - [Concurrency Model](#concurrency-model)
- [Sequence Diagrams](#sequence-diagrams)
  - [Full Booking Flow](#full-booking-flow)
  - [Cancellation Flow](#cancellation-flow)
  - [Booking Expiry](#booking-expiry)
- [Responsibilities by Module](#responsibilities-by-module)
- [Project Structure](#project-structure)
- [Error Handling](#error-handling)
- [Seed Data](#seed-data)
- [Quick Start](#quick-start)
- [Run Tests & Coverage](#run-tests--coverage)
- [Future Scope](#future-scope)

---

## Problem Statement

Build a flight booking system that allows users to:
- Search for direct and connecting flights between cities
- Lock specific seats during the booking process (preventing double-booking)
- Book with multiple passengers and optional add-ons (luggage, food, insurance)
- Confirm bookings and manage the booking lifecycle
- Cancel bookings fully or partially (removing specific passengers)
- Automatically expire unpaid bookings after a configurable timeout

The system must demonstrate:
- Type-safe state transitions for booking lifecycle (State pattern)
- Reusable cancellation algorithm with variant steps (Template Method pattern)
- Generic handler interfaces using Java generics (`StepHandler<T, U>`)
- Secure price tokens (HMAC-SHA256) to prevent price manipulation between search and booking
- Pessimistic locking on seat selection + optimistic locking (`@Version`) on flight inventory
- Comprehensive error handling via a global exception handler and machine-readable `ErrorCode` enum

## Scope

### In Scope
- Flight search (direct + 1-stop connecting) with layover validation
- Seat locking with `PESSIMISTIC_WRITE` (15-min configurable timeout)
- Multi-passenger booking with add-ons
- Booking confirmation (status transitions: INITIATED → SEAT_LOCKED → CONFIRMED)
- Full and partial cancellation (Template Method pattern)
- Scheduled job that expires unpaid bookings and releases seats
- Price token generation and verification (HMAC-SHA256)
- Integration tests + unit tests with JaCoCo coverage

### Out of Scope
- Payment processing (no `PaymentStrategy`, no refunds)
- User authentication / authorization (no security)
- Email or SMS notifications
- Third-party GDS integration
- Web UI (REST API only)
- Docker containerization

## Assumptions

1. All prices are in a single currency (INR). No multi-currency support.
2. Connecting flights are limited to 1 stop. Multi-stop is future scope.
3. A booking must have at least one ADULT passenger; INFANT count cannot exceed ADULT count.
4. Price token uses a shared secret (`app.price-token-secret`) — in production, this should be managed by a secure vault.
5. Seat lock duration is global (15 min). Per-class or per-route config is future scope.
6. Layover validation uses configurable min/max bounds (default 60–480 min).
7. No real airline API integration — flights and seats are seeded via `data.sql`.

## High-Level Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   REST API   │────▶│  Controller  │────▶│   Service    │
│  (Tomcat)    │     │   Layer      │     │   Layer      │
└──────────────┘     └──────────────┘     └──────┬───────┘
                                                 │
                    ┌────────────────────────────┼──────────────┐
                    │                            │              │
              ┌─────▼──────┐  ┌─────────────┐  ┌─▼──────────┐
              │  Flight    │  │  Seat Lock  │  │  Booking   │
              │  Search    │  │  Service    │  │  Service   │
              └─────┬──────┘  └──────┬──────┘  └──────┬─────┘
                    │                │                │
              ┌─────▼────────────────▼────────────────▼─────┐
              │              Repository Layer               │
              │  (Spring Data JPA + @Lock PESSIMISTIC_WRITE)│
              └─────────────────────┬───────────────────────┘
                                    │
              ┌─────────────────────▼───────────────────────┐
              │              H2 In-Memory DB                │
              │           (seeded via data.sql)             │
              └─────────────────────────────────────────────┘
```

## Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────┐
│                        Flight Booking System                       │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │                     REST Controllers                     │      │
│  │  ┌─────────────────┐  ┌──────────────┐  ┌───────────┐   │      │
│  │  │FlightSearchCtrl │  │BookingCtrl   │  │CancelCtrl │   │      │
│  │  │  GET /flights   │  │POST /bookings│  │POST /cncl │   │      │
│  │  └────────┬────────┘  └──────┬───────┘  └─────┬─────┘   │      │
│  └───────────┼──────────────────┼────────────────┼──────────┘      │
│              ▼                  ▼                ▼                  │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │                     Service Layer                         │      │
│  │                                                          │      │
│  │  ┌────────────────┐  ┌──────────────┐  ┌──────────┐     │      │
│  │  │FlightSearchSvc │  │PricingSvc    │  │SeatLock  │     │      │
│  │  │ (direct+conn)  │  │(HMAC tokens) │  │ (pess.   │     │      │
│  │  │                │  │              │  │  lock)   │     │      │
│  │  └────────────────┘  └──────────────┘  └────┬─────┘     │      │
│  │                                              │           │      │
│  │  ┌────────────────┐  ┌──────────────────┐    │           │      │
│  │  │  BookingSvc    │  │ Cancellation     │    │           │      │
│  │  │ (@Transactional│  │  (Template)      │    │           │      │
│  │  │  @Retryable)   │  │  ├─ FullHandler  │    │           │      │
│  │  │                │  │  └─ PartialHndlr │    │           │      │
│  │  └────────────────┘  └──────────────────┘    │           │      │
│  └──────────────────────────────────────────────┼───────────┘      │
│                                                  │                  │
│  ┌──────────────────────────────────────────────┼───────────┐      │
│  │                 Repository Layer              │           │      │
│  │  ┌──────────┐  ┌──────────┐  ┌───────────┐   │           │      │
│  │  │FlightRepo│  │SeatRepo  │  │BookingRepo│   │           │      │
│  │  │  (JPA)   │  │@Lock(PW) │  │  (JPA)    │   │           │      │
│  │  └──────────┘  └──────────┘  └───────────┘   │           │      │
│  └──────────────────────────────────────────────┼───────────┘      │
│                                                  ▼                  │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │                  H2 In-Memory Database                    │      │
│  │  flights │ seats │ bookings │ passengers │ booking_flights│      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │                    Scheduled Tasks                        │      │
│  │  ┌─────────────────────────────┐                         │      │
│  │  │  BookingExpiryJob (@Scheduled)                        │      │
│  │  │  Runs every 60s, REQUIRES_NEW per expired booking    │      │
│  │  └─────────────────────────────┘                         │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │              Global Exception Handler                     │      │
│  │  Maps BusinessException → ErrorCode → HTTP status        │      │
│  │  All errors return: {timestamp, status, errorCode,       │      │
│  │   message, path, traceId}                                │      │
│  └──────────────────────────────────────────────────────────┘      │
└────────────────────────────────────────────────────────────────────┘
```

## Technology Stack

| Component | Version |
|-----------|---------|
| Java | 17+ (target: 17, tested: up to 26) |
| Spring Boot | 3.2.5 |
| Spring Data JPA | 3.2.5 |
| H2 Database | (runtime) |
| Maven | 3.8+ |
| JaCoCo | 0.8.12 |
| Bean Validation | (jakarta.validation) |
| Spring Retry | (for `@Retryable`) |

## API Specifications

### 1. Search Flights

```
GET /api/v1/flights/search
```

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `source` | String | Yes | IATA code (e.g., DEL, BOM) |
| `destination` | String | Yes | IATA code |
| `date` | LocalDate | Yes | Departure date (yyyy-MM-dd) |
| `passengers` | int | Yes | Number of passengers |
| `flightType` | String | No | `DIRECT` to exclude connecting flights |

**Response (200):**
```json
{
  "results": [
    {
      "direct": true,
      "totalPrice": 4500.0,
      "priceToken": "a1b2c3d4e5f6...",
      "legs": [
        {
          "flightId": 1,
          "flightNumber": "AI101",
          "airline": "Air India",
          "source": "DEL",
          "destination": "BOM",
          "departureDate": "2026-06-10",
          "departureTime": "08:00",
          "arrivalDate": "2026-06-10",
          "arrivalTime": "10:00",
          "price": 4500.0,
          "availableSeats": 36
        }
      ]
    }
  ],
  "totalCount": 1
}
```

### 2. Initiate Booking

```
POST /api/v1/bookings
```

**Request Body:**
```json
{
  "flightIds": [1],
  "priceToken": "<token-from-search>",
  "passengers": [
    {"name": "Alice", "age": 30, "type": "ADULT", "seatNumber": "A1"},
    {"name": "Bob", "age": 28, "type": "ADULT", "seatNumber": "A2"}
  ],
  "addOns": {
    "luggageKg": 20,
    "food": true,
    "insurance": false
  }
}
```

**Validation rules:**
- At least one passenger must be `ADULT`
- Number of `INFANT` passengers ≤ number of `ADULT` passengers
- `priceToken` must match current price (HMAC-SHA256 verification)
- All seat numbers must exist on their respective flights
- No seat can be already locked or booked

**Response (201 CREATED):**
```json
{
  "id": 1,
  "pnr": "A1B2C3",
  "status": "SEAT_LOCKED",
  "totalAmount": 5400.0,
  "createdAt": "2026-06-03T12:00:00",
  "expiresAt": "2026-06-03T12:15:00",
  "flights": [...],
  "passengers": [...]
}
```

### 3. Confirm Booking

```
POST /api/v1/bookings/{pnr}/confirm
```

No request body required (payment is out of scope). Transitions booking from `SEAT_LOCKED` → `CONFIRMED`.

**Response (200 OK):** Full `BookingResponse` with `status: "CONFIRMED"`.

### 4. Get Booking

```
GET /api/v1/bookings/{pnr}
```

**Response (200 OK):** Full `BookingResponse`.

### 5. Cancel Booking

```
POST /api/v1/bookings/{pnr}/cancel
```

**Full cancellation:**
```json
{"cancellationType": "FULL"}
```

**Partial cancellation:**
```json
{"cancellationType": "PARTIAL", "passengerIds": [1, 3]}
```

Partial cancellation removes specified passengers. If no passengers remain, the booking is marked `CANCELLED`.

**Response (200 OK):**
```json
{
  "pnr": "A1B2C3",
  "status": "CANCELLED",
  "refundAmount": 0.0
}
```

### Error Response Format

All errors return (status varies by error):

```json
{
  "timestamp": "2026-06-03T12:00:00",
  "status": 409,
  "errorCode": "SEAT_UNAVAILABLE",
  "message": "Seat A1 is currently locked or already booked",
  "path": "/api/v1/bookings",
  "traceId": "550e8400-e29b-..."
}
```

## Low-Level Design

### Design Patterns

| Pattern | Location | Purpose |
|---------|----------|---------|
| **State** | `BookingStatus.canTransitionTo()` | Type-safe booking lifecycle. Prevents invalid transitions (e.g., confirming an expired booking). |
| **Template Method** | `CancellationTemplate` → `FullCancellationHandler` / `PartialCancellationHandler` | Reusable cancellation algorithm. Base class defines `handle()` flow; subclasses override `cancelSeats()` and `updateAfterCancellation()`. |
| **Strategy** | (Designed for `StepHandler<T, U>` interface) | Generic handler pattern. `StepHandler<CancellationContext, CancellationResponse>` enables pluggable step logic. |
| **Generic Handler** | `StepHandler<T, U>` | Interface with `default` method returning null. `CancellationHandler extends StepHandler<CancellationContext, CancellationResponse>`. |
| **Pessimistic Lock** | `SeatRepository.findByFlightIdAndSeatNumberWithLock()` + `@Lock(PESSIMISTIC_WRITE)` | `SELECT ... FOR UPDATE` prevents two users booking the same seat. |
| **Optimistic Lock** | `Flight.@Version` | `@Retryable` on booking service retries when concurrent inventory updates cause `OptimisticLockException`. |
| **DTO Pattern** | All DTOs in `dto/` | Separation of API contract from entity model. `BookingResponse` builds from `Booking` entity. |
| **Exception Hierarchy** | `BusinessException` + 7 subclasses | Each exception maps to a specific `ErrorCode`. Single `@ControllerAdvice` handles all. |

### Booking State Machine

```
    ┌──────────┐
    │INITIATED │
    └────┬─────┘
         │
         ▼
    ┌──────────┐
    │SEAT_LOCK │◄──── SEAT_LOCK → CONFIRMED (payment step removed)
    └────┬─────┘
         │
    ┌────▼─────┐     ┌───────────┐
    │CONFIRMED │────►│ CANCELLED │
    └──────────┘     └───────────┘
         │                ▲
         ▼                │
    ┌─────────┐    ┌──────────┐
    │ EXPIRED │    │SEAT_LOCK │──► CANCELLED (new!)
    └─────────┘    └──────────┘
```

**Transition rules:**

| From | To | Trigger |
|------|----|---------|
| INITIATED | SEAT_LOCKED | Seats locked successfully |
| INITIATED | EXPIRED | Booking expiry job |
| SEAT_LOCKED | CONFIRMED | Confirmation |
| SEAT_LOCKED | EXPIRED | Booking expiry job |
| SEAT_LOCKED | CANCELLED | Cancellation (before confirm) |
| CONFIRMED | CANCELLED | Cancellation (full or partial) |
| EXPIRED | — | Terminal state |
| CANCELLED | — | Terminal state |

### Cancellation Flow (Template Method)

```
StepHandler<T, U> (interface)
  │ default T handle(T input) → returns null
  │
  └── CancellationHandler extends StepHandler<CancellationContext, CancellationResponse>
       │ (marks interface with specific types)
       │
       └── CancellationTemplate (abstract)
            │ final handle(ctx):
            │   1. Find booking by PNR
            │   2. Validate cancellation is allowed (status check)
            │   3. cancelSeats(ctx, booking)  ← abstract
            │   4. updateBookingStatus(booking) ← abstract
            │   5. Save booking
            │   6. Build response
            │
            ├── FullCancellationHandler
            │   cancelSeats(): releases ALL seats via releaseSeatsByBooking()
            │   updateAfterCancellation(): status = CANCELLED
            │
            └── PartialCancellationHandler
                cancelSeats(): releases specific passenger seats
                updateAfterCancellation(): removes passengers, sets CANCELLED if none left
```

### Price Token Strategy

```
Generation (FlightSearchService):
  data = sorted(flightIds).join(",") + "|" + basePrice
  token = HMAC-SHA256(data, secret)

Verification (BookingService):
  Re-compute HMAC-SHA256 of same data with same secret
  Check each flight's current price matches the price in the token
  Fail: PriceChangedException (409 CONFLICT)
```

The price token prevents a class of race conditions where a user sees a price, and by the time they book, the price has changed. The token binds the user to the exact price they saw during search.

### Concurrency Model

| Resource | Mechanism | Scope |
|----------|-----------|-------|
| Seat selection | `@Lock(PESSIMISTIC_WRITE)` | Per seat row. Locks during `findByFlightIdAndSeatNumberWithLock` until transaction commits. |
| Flight inventory | `@Version` (int) + `@Retryable` | Optimistic. Retries up to 3 times on `StaleObjectStateException`. |
| Booking expiry | `Propagation.REQUIRES_NEW` | Each expired booking gets its own transaction. One failure doesn't affect others. |
| Partial cancellation | Remove from `passengers` collection | JPA cascades. If no passengers left, status → CANCELLED and seats released. |

## Sequence Diagrams

### Full Booking Flow

```
Client          FlightSearchCtrl    FlightSearchSvc     PricingSvc      DB
  │                    │                    │                │           │
  │─ GET /search ──────▶                    │                │           │
  │                    │── search() ────────▶                │           │
  │                    │                    │── query ───────┼──────────▶│
  │                    │                    │◄─ results ────┼───────────│
  │                    │                    │── genToken ───▶│           │
  │                    │                    │◄─ token ───────│           │
  │                    │◄─ response ────────│                │           │
  │◄─ 200 + results ───│                    │                │           │
  │                    │                    │                │           │
  │─ POST /bookings ───▶─ BookingCtrl ──▶ BookingSvc        │           │
  │                    │                    │── verifyToken ─▶           │
  │                    │                    │◄─ ok ──────────│           │
  │                    │                    │── lock seats ──┼──────────▶│
  │                    │                    │   (PESSIMISTIC │ SELECT    │
  │                    │                    │    WRITE)     │ FOR UPDATE│
  │                    │                    │◄─ locked ─────┼───────────│
  │                    │                    │── create bkg ──┼──────────▶│
  │                    │                    │── update inv ──┼──────────▶│
  │                    │                    │◄─ saved ──────┼───────────│
  │                    │◄─ BookingResponse ─│                │           │
  │◄─ 201 CREATED ─────│                    │                │           │
  │                    │                    │                │           │
  │─ POST /{pnr}/cnfm ─▶                    │── confirm ─────┼──────────▶│
  │                    │                    │── confirm seats│           │
  │                    │                    │── update bkg   │           │
  │◄─ 200 CONFIRMED ───│                    │                │           │
```

### Cancellation Flow

```
Client          CancellationCtrl      CancellationTemplate    DB
  │                    │                      │               │
  │─ POST /{pnr}/cancel──────────────────────▶               │
  │                    │                      │               │
  │                    │     handle(ctx):     │               │
  │                    │      1. findByPnr ───┼─────────────▶│
  │                    │      2. validate     │               │
  │                    │         status       │               │
  │                    │                      │               │
  │                    │  ┌─ FullHandler:     │               │
  │                    │  │  release all      │               │
  │                    │  │  seats by booking │               │
  │                    │  │  status→CANCELLED │               │
  │                    │  │                   │               │
  │                    │  └─ PartialHandler:  │               │
  │                    │     release N seats  │               │
  │                    │     remove N pass.   │               │
  │                    │     if 0 pass. left→ │               │
  │                    │       status→CANCELL │               │
  │                    │                      │               │
  │                    │      3. save ────────┼─────────────▶│
  │                    │◄─ response ──────────│               │
  │◄─ 200 CANCELLED ───│                      │               │
```

### Booking Expiry Job

```
Scheduler        BookingExpiryJob         BookingSvc        DB
    │                  │                      │              │
    │─ @Scheduled ─────▶                      │              │
    │  (every 60s)     │                      │              │
    │                  │── findByExpired ──────┼────────────▶│
    │                  │◄─ [booking list] ─────┼─────────────│
    │                  │                      │              │
    │                  │  for each booking:   │              │
    │                  │── expire(booking) ────▶              │
    │                  │  (REQUIRES_NEW tx)   │── release ──▶│
    │                  │                      │── update ───▶│
    │                  │◄─ done ──────────────│              │
    │                  │                      │              │
    │                  │  ...next booking...  │              │
```

## Responsibilities by Module

| Module | Responsibility |
|--------|---------------|
| `controller/` | HTTP request mapping, input validation (`@Valid`), delegation to services |
| `service/` | Business logic: search algorithm, pricing tokens, seat locking, booking lifecycle, cancellation |
| `service/impl/` | Concrete cancellation handlers (FullCancellationHandler, PartialCancellationHandler) |
| `service/StepHandler.java` | Generic `T → U` handler interface, base of Template Method pattern |
| `entity/` | JPA entities with `@Version`, cascading, column constraints |
| `repository/` | Spring Data JPA repositories with custom `@Query` and `@Lock(PESSIMISTIC_WRITE)` |
| `dto/` | Request/response DTOs, `CancellationContext` record for generic handler |
| `enums/` | `BookingStatus` with state transition matrix, `ErrorCode`, `CancellationType`, `PassengerType` |
| `exception/` | `BusinessException` hierarchy + `GlobalExceptionHandler` with machine-readable `ErrorResponse` |
| `scheduled/` | `BookingExpiryJob` — `@Scheduled(60s)`, per-booking `REQUIRES_NEW` |
| `config/` | `AddOnPricingConfig` — `@ConfigurationProperties(prefix = "app.addon")` |

## Project Structure

```
src/main/java/com/flightbooking/
├── config/
│   └── AddOnPricingConfig.java         # @ConfigurationProperties for addon prices
├── controller/
│   ├── FlightSearchController.java      # GET /api/v1/flights/search
│   ├── BookingController.java           # POST /bookings, GET/{pnr}, POST/{pnr}/confirm
│   └── CancellationController.java      # POST /bookings/{pnr}/cancel
├── dto/
│   ├── FlightSearchRequest/Response     # Search parameter & result DTOs
│   ├── FlightResultDTO.java             # Flight search result + FlightLegDTO (inner class)
│   ├── BookingRequest/Response          # Booking creation & response DTOs
│   ├── PassengerDTO.java                # Passenger info (name, age, type, seatNumber)
│   ├── AddOnsDTO.java                   # Addon selections
│   ├── CancellationRequest/Response     # Cancel request with type + passengerIds
│   └── CancellationContext.java         # Record: pnr + CancellationRequest
├── entity/
│   ├── Flight.java                      # Flight schedule, inventory, @Version
│   ├── Booking.java                     # Booking details, @OneToMany passengers
│   ├── Passenger.java                   # Passenger info linked to Booking
│   └── Seat.java                        # Seat state (isAvailable, lockedUntil, lockedBy)
├── enums/
│   ├── BookingStatus.java               # State machine with canTransitionTo()
│   ├── CancellationType.java            # FULL, PARTIAL
│   ├── ErrorCode.java                   # Machine-readable error codes
│   └── PassengerType.java               # ADULT, CHILD, INFANT
├── exception/
│   ├── BusinessException.java           # Base exception with ErrorCode
│   ├── FlightNotFoundException.java     # ErrorCode.FLIGHT_NOT_FOUND
│   ├── SeatUnavailableException.java    # ErrorCode.SEAT_UNAVAILABLE
│   ├── BookingNotFoundException.java    # ErrorCode.BOOKING_NOT_FOUND
│   ├── BookingExpiredException.java     # ErrorCode.BOOKING_EXPIRED
│   ├── PriceChangedException.java       # ErrorCode.PRICE_CHANGED
│   ├── IllegalBookingStateException.java # ErrorCode.ILLEGAL_STATE
│   ├── CancellationInvalidException.java # Cancellation not allowed
│   └── GlobalExceptionHandler.java      # @ControllerAdvice, ErrorResponse record
├── repository/
│   ├── FlightRepository.java            # Search queries (by route/date)
│   ├── SeatRepository.java              # @Lock(PESSIMISTIC_WRITE) query
│   └── BookingRepository.java           # Find by PNR, find expired
├── scheduled/
│   └── BookingExpiryJob.java            # @Scheduled, REQUIRES_NEW per booking
├── service/
│   ├── StepHandler.java                 # Generic <T, U> handler interface
│   ├── CancellationHandler.java         # extends StepHandler<CancellationContext, CancellationResponse>
│   ├── CancellationTemplate.java        # Abstract base with final handle()
│   ├── impl/
│   │   ├── FullCancellationHandler.java   # Cancel entire booking
│   │   └── PartialCancellationHandler.java # Remove specific passengers
│   ├── FlightSearchService.java         # Direct + 1-stop connecting search
│   ├── PricingService.java              # HMAC-SHA256 price token
│   ├── SeatLockService.java             # Lock/confirm/release seats
│   └── BookingService.java              # Initiate/confirm/get/expire bookings
├── Constants.java                       # Shared constants (API paths, column lengths)
└── FlightBookingApplication.java        # @SpringBootApplication entry point

src/main/resources/
├── application.yml                      # All config values
└── data.sql                             # Seed data (9 flights, 36+ seats each)

src/test/java/com/flightbooking/
├── enums/
│   └── BookingStatusTest.java           # 13 tests: all state transitions
├── service/
│   ├── PricingServiceTest.java          # 7 tests: token gen/verify/exception
│   ├── SeatLockServiceTest.java         # 7 tests: lock/confirm/release seats
│   ├── FlightSearchServiceTest.java     # 6 tests: direct + connecting search
│   └── CancellationTemplateTest.java    # 7 tests: full + partial cancel
├── testutil/
│   └── TestJpaRepository.java           # Abstract stub for repository mocking
└── FlightBookingIntegrationTest.java    # 16 tests: full flow, edge cases, validation
```

## Error Handling

All exceptions extend `BusinessException`, which carries an `ErrorCode` enum value.

| ErrorCode | HTTP Status | Trigger |
|-----------|-------------|---------|
| `FLIGHT_NOT_FOUND` | 404 | Invalid flight ID in request |
| `BOOKING_NOT_FOUND` | 404 | Invalid PNR in request |
| `SEAT_UNAVAILABLE` | 409 | Seat not found, locked, or already booked |
| `PRICE_CHANGED` | 409 | Price token doesn't match current price |
| `BOOKING_EXPIRED` | 410 | Seat lock duration expired |
| `ILLEGAL_STATE` | 400 | Invalid booking status transition |
| `VALIDATION_ERROR` | 400 | Bean validation failure or business rule violation |
| `INTERNAL_ERROR` | 500 | Unhandled exception |

The `GlobalExceptionHandler` logs each error with a unique `traceId` for debugging.

## Seed Data

**9 flights** pre-loaded on startup:

| Flight | Airline | Route | Time | Price | Connecting Via |
|--------|---------|-------|------|-------|----------------|
| AI101 | Air India | DEL → BOM | 08:00–10:00 | ₹4,500 | — |
| AI202 | Air India | DEL → BOM | 14:00–16:00 | ₹5,200 | — |
| 6E301 | IndiGo | BOM → MAA | 11:00–13:00 | ₹3,200 | — |
| 6E302 | IndiGo | BOM → MAA | 17:00–19:00 | ₹3,800 | — |
| UK401 | Vistara | DEL → MAA | 06:00–09:00 | ₹6,500 | — |
| SG501 | SpiceJet | DEL → CCU | 07:00–09:30 | ₹2,800 | — |
| AI601 | Air India | CCU → MAA | 11:00–13:30 | ₹3,500 | — |
| 6E701 | IndiGo | DEL → HYD | 09:00–11:30 | ₹3,000 | — |
| UK801 | Vistara | HYD → MAA | 13:00–14:30 | ₹2,200 | — |

**Connecting routes (1-stop):**
- DEL → MAA via BOM (layover ~3h): ₹4,500 + ₹3,200 = ₹7,700
- DEL → MAA via CCU (layover ~4h): ₹2,800 + ₹3,500 = ₹6,300
- DEL → MAA via HYD (layover ~3h): ₹3,000 + ₹2,200 = ₹5,200

**36 seats per flight** (rows A–F, seats 1–6).

## Quick Start

```bash
# Build
mvn clean package

# Run
java -jar target/flight-booking-1.0-SNAPSHOT.jar
```

Server starts at **http://localhost:8080**

> **Port already in use?** `lsof -ti :8080 | xargs kill` then retry.

**Prerequisites:** Java 17+, Maven 3.8+. Install: `brew install maven` (macOS) / `sudo apt install maven` (Linux).

### Sample API Calls

```bash
# 1. Search flights
curl "http://localhost:8080/api/v1/flights/search?source=DEL&destination=BOM&date=2026-06-10&passengers=1"

# 2. Book a flight (save PNR from response)
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{"flightIds":[1],"priceToken":"<token>","passengers":[{"name":"Alice","age":30,"type":"ADULT","seatNumber":"A1"}]}'

# 3. Confirm booking
curl -X POST http://localhost:8080/api/v1/bookings/A1B2C3/confirm

# 4. Get booking
curl http://localhost:8080/api/v1/bookings/A1B2C3

# 5. Cancel booking
curl -X POST http://localhost:8080/api/v1/bookings/A1B2C3/cancel \
  -H "Content-Type: application/json" \
  -d '{"cancellationType":"FULL"}'
```

### H2 Console

```
http://localhost:8080/h2-console
```

- JDBC URL: `jdbc:h2:mem:flightbooking`
- User: `sa`
- Password: *(blank)*

## Run Tests & Coverage

```bash
# Run all tests
mvn test

# View coverage report (opens in browser)
open target/site/jacoco/index.html
```

**Test summary:** 56 tests across 6 test classes:
- 13 unit tests: `BookingStatusTest` (all state transitions)
- 7 unit tests: `PricingServiceTest` (token gen/verify/exception)
- 7 unit tests: `SeatLockServiceTest` (lock/confirm/release)
- 6 unit tests: `FlightSearchServiceTest` (direct + connecting search)
- 7 unit tests: `CancellationTemplateTest` (full + partial cancel)
- 16 integration tests: `FlightBookingIntegrationTest` (full flow, edge cases, error scenarios, add-ons)

JaCoCo generates a detailed line/branch coverage report at `target/site/jacoco/index.html`.

## H2 Console

```
http://localhost:8080/h2-console
```

- JDBC URL: `jdbc:h2:mem:flightbooking`
- User: `sa`
- Password: *(blank)*

## Future Scope

1. **Multi-stop flights** — Extend `FlightSearchService` to find itineraries with 2+ stops using BFS/DFS on the flight graph.
2. **Per-class seat lock duration** — Allow different lock durations by fare class, route, or loyalty tier.
3. **Real payment integration** — Re-introduce `PaymentStrategy` pattern with actual PSP gateways (Razorpay, Stripe).
4. **Refund processing** — Introduce configurable refund tiers in the cancellation flow (time-based percentage).
5. **Caching** — Add Redis-backed cache for flight search results to reduce database load (keyed by route + date).
6. **API rate limiting** — Protect booking endpoints with token-bucket or sliding-window rate limiting.
7. **Audit logging** — Track all state transitions with timestamps and actor identity.
8. **Distributed locking** — Replace `PESSIMISTIC_WRITE` with Redis Redlock for horizontal scaling across multiple instances.
9. **User accounts** — Associate bookings with users, add role-based access.
10. **Idempotency** — Add idempotency keys on booking creation to prevent duplicate bookings on network retry.
11. **Configuration server** — Externalize all configurable properties to Spring Cloud Config or Vault.
12. **Reactive API** — Consider Spring WebFlux for non-blocking flight search under high load.
