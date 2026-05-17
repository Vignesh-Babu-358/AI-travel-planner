package com.example.travelplanner.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Request to persist a trip and embed it into the vector store for future RAG.
 */
public record SaveTripRequest(
		@NotBlank String origin,
		@NotBlank String destination,
		LocalDate startDate,
		LocalDate endDate,
		String interests,
		String budget,
		@NotBlank String itinerary
) {
}
