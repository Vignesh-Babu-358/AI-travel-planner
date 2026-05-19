package com.example.travelplanner.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Request to persist a motorcycle road trip and embed it into the vector store
 * for future RAG retrieval.
 */
public record SaveTripRequest(
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
		@NotBlank String itinerary
) {
}
