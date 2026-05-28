package com.example.travelplanner.dto;

import java.util.List;

/**
 * The generated ride plan and the past rides used as RAG context.
 */
public record PlanTripResponse(
		String destination,
		String model,
		String itinerary,
		List<SimilarTripResponse> usedContext
) {
}
