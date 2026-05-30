package com.example.travelplanner.dto;

/**
 * One day of the real route after chunking the full Google Maps polyline
 * into segments of at most {@code maxDailyDistanceKm}. Distances/durations
 * are ground truth from the routing engine — the LLM is forbidden from
 * changing them.
 */
public record RouteDayLeg(
		int day,
		String startName,
		String endName,
		double distanceKm,
		String duration,
		double[] startLatLng,
		double[] endLatLng
) {
}
