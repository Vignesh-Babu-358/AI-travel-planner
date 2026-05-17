package com.example.travelplanner.dto;

import com.example.travelplanner.domain.Trip;

import java.time.Instant;
import java.time.LocalDate;

public record TripResponse(
		Long id,
		String origin,
		String destination,
		LocalDate startDate,
		LocalDate endDate,
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
				trip.getStartDate(),
				trip.getEndDate(),
				trip.getInterests(),
				trip.getBudget(),
				trip.getItinerary(),
				trip.getCreatedAt()
		);
	}
}
