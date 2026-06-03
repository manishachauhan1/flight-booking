# Flight Booking System

A Spring Boot REST API for searching flights, booking seats, processing payments, and managing cancellations. Uses H2 in-memory database — zero setup required.

## Quick Start

```bash
# Build & run
mvn clean package
java -jar target/flight-booking-1.0-SNAPSHOT.jar
```

Server starts at **http://localhost:8080**

> **Port already in use?** `lsof -ti :8080 | xargs kill` then retry.

## Prerequisites

- Java 17+ (`java --version`)
- Maven 3.8+ (`mvn --version`)

Install Maven: `brew install maven` (macOS) / `sudo apt install maven` (Linux).

## Features

| Feature | Details |
|---------|---------|
| **Flight Search** | Direct flights + 1-stop connecting flights with price tokens |
| **Seat Locking** | 15-minute pessimistic lock on selected seats during booking |
| **Booking** | Multi-passenger bookings with add-ons (luggage, food, insurance) |
| **Payment** | 4 modes — UPI, Card, NetBanking, Wallet |
| **Cancellation** | Full or partial with time-based refund policy |
| **Auto-expiry** | Unpaid bookings auto-release seats every 60 seconds |

## API Endpoints

### 1. Search Flights

```
GET /api/v1/flights/search?source=DEL&destination=BOM&date=2026-06-10&passengers=2
```

Returns direct + connecting results. Each includes a **priceToken** — save this for booking.

**Try it:**
```bash
curl "http://localhost:8080/api/v1/flights/search?source=DEL&destination=BOM&date=2026-06-10&passengers=2"
```

### 2. Book a Flight

```
POST /api/v1/bookings
```

```json
{
  "flightIds": [1],
  "priceToken": "<token-from-search>",
  "passengers": [
    {"name": "Alice", "age": 30, "type": "ADULT", "seatNumber": "A1"},
    {"name": "Bob", "age": 28, "type": "ADULT", "seatNumber": "A2"}
  ],
  "addOns": {"luggageKg": 20, "food": true, "insurance": false}
}
```

Returns PNR and status `SEAT_LOCKED`. Seats held for 15 minutes.

```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{"flightIds":[1],"priceToken":"<token>","passengers":[{"name":"Alice","age":30,"type":"ADULT","seatNumber":"A1"}]}'
```

### 3. Confirm Payment

```
POST /api/v1/bookings/{PNR}/confirm
```

```json
{"paymentMode": "UPI", "upiId": "alice@bank"}
```

Other modes: `CARD` (send cardNumber/expiry/cvv), `NET_BANKING` (send bankName), `WALLET` (send walletId).

```bash
curl -X POST http://localhost:8080/api/v1/bookings/A1B2C3/confirm \
  -H "Content-Type: application/json" \
  -d '{"paymentMode":"UPI","upiId":"alice@bank"}'
```

### 4. Get Booking

```
GET /api/v1/bookings/{PNR}
```

```bash
curl http://localhost:8080/api/v1/bookings/A1B2C3
```

### 5. Cancel Booking

```
POST /api/v1/bookings/{PNR}/cancel
```

**Full cancellation:**
```json
{"cancellationType": "FULL"}
```

**Partial cancellation:**
```json
{"cancellationType": "PARTIAL", "passengerIds": [1]}
```

**Refund policy:**

| Time before departure | Refund |
|----------------------|--------|
| > 72 hours | 90% |
| 24–72 hours | 50% |
| 12–24 hours | 25% |
| < 12 hours | 0% |

```bash
curl -X POST http://localhost:8080/api/v1/bookings/A1B2C3/cancel \
  -H "Content-Type: application/json" \
  -d '{"cancellationType":"FULL"}'
```

## Seed Data

Pre-loaded flights on startup:

| Flight | Route | Time | Price |
|--------|-------|------|-------|
| AI101 | DEL → BOM | 08:00–10:00 | ₹4,500 |
| AI202 | DEL → BOM | 14:00–16:00 | ₹5,200 |
| 6E301 | BOM → MAA | 11:00–13:00 | ₹3,200 |
| 6E302 | BOM → MAA | 17:00–19:00 | ₹3,800 |
| UK401 | DEL → MAA | 06:00–09:00 | ₹6,500 |
| SG501 | DEL → CCU | 07:00–09:30 | ₹2,800 |
| AI601 | CCU → MAA | 11:00–13:30 | ₹3,500 |
| 6E701 | DEL → HYD | 09:00–11:30 | ₹3,000 |
| UK801 | HYD → MAA | 13:00–14:30 | ₹2,200 |

Connecting routes available: DEL → MAA via BOM (layover ~3h), CCU (~4h), or HYD (~3h).

## Error Codes

All errors return:

```json
{
  "errorCode": "PRICE_CHANGED",
  "status": 409,
  "message": "Price has changed from 4500.0 to 5200.0"
}
```

| Code | Status | Meaning |
|------|--------|---------|
| `FLIGHT_NOT_FOUND` | 404 | Invalid flight ID |
| `BOOKING_NOT_FOUND` | 404 | Invalid PNR |
| `SEAT_UNAVAILABLE` | 409 | Seat already booked |
| `PRICE_CHANGED` | 409 | Price changed since search |
| `BOOKING_EXPIRED` | 410 | 15-min lock expired |
| `ILLEGAL_STATE` | 400 | Invalid booking status transition |
| `VALIDATION_ERROR` | 400 | Invalid input |
| `PAYMENT_FAILED` | 502 | Payment declined |
| `INTERNAL_ERROR` | 500 | Unexpected error |

## H2 Console

```
http://localhost:8080/h2-console
```

- JDBC URL: `jdbc:h2:mem:flightbooking`
- User: `sa`
- Password: *(blank)*

## Run Tests

```bash
mvn test
```

6 integration tests covering the full flow: search → book → confirm → cancel.

## Debug in IntelliJ

1. Open project in IntelliJ
2. Run → Edit Configurations → Add → Spring Boot
3. Main class: `com.flightbooking.FlightBookingApplication`
4. Click Debug (or `^D` / `Shift+F9`)

A run configuration is already saved at `.idea/runConfigurations/FlightBookingApplication.xml`.

## Project Structure

```
src/main/java/com/flightbooking/
├── controller/        # REST endpoints
├── service/           # Business logic + design patterns
│   └── payment/       # Payment strategy implementations
├── repository/        # Spring Data JPA
├── entity/            # JPA entities
├── enums/             # ErrorCode, BookingStatus, etc.
├── dto/               # Request/response DTOs
├── exception/         # Exception hierarchy + global handler
└── scheduled/         # Booking expiry job
```

## Design Patterns

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Strategy** | `service/payment/` | 4 payment modes selected by `PaymentContext` via `Map<String, PaymentStrategy>` — add new modes without touching existing code |
| **Template Method** | `service/CancellationTemplate.java` | Full and partial cancellation share the same flow; only `calculateRefund()` differs |
| **State** | `enums/BookingStatus.java` | Booking lifecycle with `canTransitionTo()` — blocks invalid transitions like paying for an expired booking |
| **Pessimistic Lock** | `SeatRepository.java` | `SELECT ... FOR UPDATE` prevents two users booking the same seat |
| **Optimistic Lock** | `Flight.java` (`@Version`) | `@Retryable` on concurrent inventory updates |
