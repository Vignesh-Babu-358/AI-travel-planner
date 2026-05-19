package com.example.travelplanner.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Request to generate (but not persist) a motorcycle road-trip itinerary.
 * Only {@code origin} (ride start) and {@code destination} are required;
 * everything else is optional and falls back to sensible defaults.
 */
public record PlanTripRequest(
		@NotBlank String origin,
		@NotBlank String destination,
		String waypoints,
		LocalDate startDate,
		LocalDate endDate,
		String motorcycleModel,
		String ridingExperience,
		Integer maxDailyDistanceKm,
		Integer fuelRangeKm,
		String routePreference,
		Boolean avoidHighways,
		Boolean avoidTolls,
		String interests,
		String budget,
		String notes
) {
}
