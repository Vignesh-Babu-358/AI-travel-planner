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

	private TripDocuments() {
	}

	/** The text that gets embedded into the vector store. */
	static String toText(Trip trip) {
		return """
				Trip to %s (from %s)
				Dates: %s to %s
				Interests: %s
				Budget: %s

				Itinerary:
				%s""".formatted(
				trip.getDestination(),
				trip.getOrigin(),
				trip.getStartDate(),
				trip.getEndDate(),
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
}
