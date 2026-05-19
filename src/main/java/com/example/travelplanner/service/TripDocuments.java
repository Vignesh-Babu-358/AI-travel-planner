package com.example.travelplanner.service;

import com.example.travelplanner.domain.Trip;
import com.example.travelplanner.dto.SimilarTripResponse;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * Helpers for converting trips to/from Spring AI {@link Document}s so the same
 * embedding/text representation is used when saving and when retrieving.
 */
final class TripDocuments {

	static final String META_TRIP_ID = "tripId";
	static final String META_DESTINATION = "destination";
	static final String META_INTERESTS = "interests";
	static final String META_ROUTE_PREFERENCE = "routePreference";

	private TripDocuments() {
	}

	/** The text that gets embedded into the vector store. */
	static String toText(Trip trip) {
		return """
				Motorcycle road trip: %s -> %s (via %s)
				Dates: %s to %s
				Motorcycle: %s
				Rider experience: %s
				Max daily distance: %s km
				Fuel range: %s km
				Route preference: %s
				Avoid highways: %s | Avoid tolls: %s
				Scenery & points of interest: %s
				Budget: %s

				Itinerary:
				%s""".formatted(
				nullSafe(trip.getOrigin()),
				nullSafe(trip.getDestination()),
				nullSafe(trip.getWaypoints()),
				trip.getStartDate(),
				trip.getEndDate(),
				nullSafe(trip.getMotorcycleModel()),
				nullSafe(trip.getRidingExperience()),
				str(trip.getMaxDailyDistanceKm()),
				str(trip.getFuelRangeKm()),
				nullSafe(trip.getRoutePreference()),
				str(trip.getAvoidHighways()),
				str(trip.getAvoidTolls()),
				nullSafe(trip.getInterests()),
				nullSafe(trip.getBudget()),
				nullSafe(trip.getItinerary()));
	}

	static Document toDocument(Trip trip) {
		Map<String, Object> metadata = new HashMap<>();
		if (trip.getId() != null) {
			metadata.put(META_TRIP_ID, trip.getId());
		}
		metadata.put(META_DESTINATION, nullSafe(trip.getDestination()));
		metadata.put(META_INTERESTS, nullSafe(trip.getInterests()));
		metadata.put(META_ROUTE_PREFERENCE, nullSafe(trip.getRoutePreference()));
		return Document.builder()
				.text(toText(trip))
				.metadata(metadata)
				.build();
	}

	static SimilarTripResponse toSimilarResponse(Document doc) {
		Object rawId = doc.getMetadata().get(META_TRIP_ID);
		Long tripId = switch (rawId) {
			case Number n -> n.longValue();
			case String s -> parseLongOrNull(s);
			case null, default -> null;
		};
		Double score = doc.getScore();
		return new SimilarTripResponse(
				tripId,
				String.valueOf(doc.getMetadata().getOrDefault(META_DESTINATION, "")),
				String.valueOf(doc.getMetadata().getOrDefault(META_INTERESTS, "")),
				score != null ? score : 0.0,
				snippet(doc.getText()));
	}

	private static String snippet(String text) {
		if (text == null) {
			return "";
		}
		String trimmed = text.strip();
		return trimmed.length() <= 280 ? trimmed : trimmed.substring(0, 280) + "...";
	}

	private static Long parseLongOrNull(String s) {
		try {
			return Long.valueOf(s);
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private static String nullSafe(String s) {
		return s == null ? "" : s;
	}

	private static String str(Object o) {
		return o == null ? "unspecified" : String.valueOf(o);
	}
}
