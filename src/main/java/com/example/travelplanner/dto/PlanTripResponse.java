package com.example.travelplanner.dto;

import java.util.List;

/**
 * The generated ride plan: ground-truth {@link RouteSummary route} from
 * Google Maps, the LLM-written narrative built around it, and the past
 * rides used as RAG context.
 */
public record PlanTripResponse(
		String destination,
		String model,
		RouteSummary route,
		String itinerary,
		List<SimilarTripResponse> usedContext
) {
}
