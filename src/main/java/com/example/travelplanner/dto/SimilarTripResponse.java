package com.example.travelplanner.dto;

/**
 * A past trip surfaced from the vector store via similarity search.
 */
public record SimilarTripResponse(
		Long tripId,
		String destination,
		String interests,
		double score,
		String snippet
) {
}
