package com.example.travelplanner.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Request to generate (but not persist) an AI itinerary.
 */
public record PlanTripRequest(
		@NotBlank String origin,
		@NotBlank String destination,
		LocalDate startDate,
		LocalDate endDate,
		String interests,
		String budget,
		String notes
) {
}
