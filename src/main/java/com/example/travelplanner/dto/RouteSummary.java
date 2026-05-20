package com.example.travelplanner.dto;

import java.util.List;

/**
 * The full ground-truth route for a plan request: total real distance/duration
 * from OpenRouteService and the daily legs after chunking. The LLM narrative
 * is built around this; it does not invent any of these numbers or names.
 */
public record RouteSummary(
		String profile,
		double totalDistanceKm,
		String totalDuration,
		List<RouteDayLeg> days
) {
}
