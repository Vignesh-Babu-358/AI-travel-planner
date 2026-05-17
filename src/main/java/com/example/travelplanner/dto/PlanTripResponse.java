package com.example.travelplanner.dto;

import java.util.List;

/**
 * The generated itinerary plus the past trips that were used as RAG context.
 */
public record PlanTripResponse(
		String destination,
		String model,
		String itinerary,
		List<SimilarTripResponse> usedContext
) {
}
