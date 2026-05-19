package com.example.travelplanner.dto;

import com.example.travelplanner.domain.Trip;

import java.time.Instant;
import java.time.LocalDate;

public record TripResponse(
		Long id,
		String origin,
		String destination,
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
		String itinerary,
		Instant createdAt
) {
	public static TripResponse from(Trip trip) {
		return new TripResponse(
				trip.getId(),
				trip.getOrigin(),
				trip.getDestination(),
				trip.getWaypoints(),
				trip.getStartDate(),
				trip.getEndDate(),
				trip.getMotorcycleModel(),
				trip.getRidingExperience(),
				trip.getMaxDailyDistanceKm(),
				trip.getFuelRangeKm(),
				trip.getRoutePreference(),
				trip.getAvoidHighways(),
				trip.getAvoidTolls(),
				trip.getInterests(),
				trip.getBudget(),
				trip.getItinerary(),
				trip.getCreatedAt()
		);
	}
}
