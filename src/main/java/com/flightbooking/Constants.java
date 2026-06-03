package com.flightbooking;

public final class Constants {

    private Constants() {}

    public static final String API_BOOKINGS = "/api/v1/bookings";
    public static final String API_FLIGHTS = "/api/v1/flights";

    public static final int PNR_LENGTH = 6;
    public static final int STATUS_COLUMN_LENGTH = 20;

    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String PRICE_FORMAT = "%.2f";

    public static final String DEFAULT_TRIP_TYPE = "ONE_WAY";
    public static final String DEFAULT_FLIGHT_TYPE = "ALL";
    public static final String TRIP_TYPE_ROUND_TRIP = "ROUND_TRIP";
    public static final String FLIGHT_TYPE_DIRECT = "DIRECT";

    public static final String MSG_PASSENGER_REQUIRED = "At least one passenger is required";
    public static final String MSG_ADULT_REQUIRED = "At least one adult is required per booking";
    public static final String MSG_INFANT_LIMIT = "Number of infants cannot exceed number of adults";

    public static final String CANCEL_MSG = "Cancellation completed";

    public static final String ERROR_MSG_SEPARATOR = "; ";
    public static final String GENERIC_ERROR_MSG = "An unexpected error occurred";

    public static final int PASSENGER_TYPE_COLUMN_LENGTH = 10;
    public static final int MIN_PASSENGERS = 1;
    public static final int DEFAULT_PASSENGERS = 1;
    public static final String CONNECTING_FLIGHT_KEY_SEPARATOR = "-";
}
