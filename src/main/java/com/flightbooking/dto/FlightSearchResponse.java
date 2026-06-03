package com.flightbooking.dto;

import java.util.ArrayList;
import java.util.List;

public class FlightSearchResponse {

    private List<FlightResultDTO> results = new ArrayList<>();
    private int totalCount;

    public FlightSearchResponse() {}

    public FlightSearchResponse(List<FlightResultDTO> results) {
        this.results = results;
        this.totalCount = results.size();
    }

    public List<FlightResultDTO> getResults() { return results; }
    public void setResults(List<FlightResultDTO> results) {
        this.results = results;
        this.totalCount = results.size();
    }
    public int getTotalCount() { return totalCount; }
}
