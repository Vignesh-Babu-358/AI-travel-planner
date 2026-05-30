package com.example.travelplanner.service.routing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thin HTTP wrapper around the Google Maps Platform APIs (legacy
 * {@code /maps/api/*} endpoints). The API key is appended as a query
 * parameter on every request. Any missing key or upstream failure
 * surfaces as a 503/400 so the user sees a clear error instead of a
 * hallucinated route.
 */
@Component
public class GoogleMapsClient {

	private static final Logger log = LoggerFactory.getLogger(GoogleMapsClient.class);

	// Google geocode result.types[] buckets used to filter coarse matches.
	private static final Set<String> PRECISE_TYPES = Set.of(
			"locality", "sublocality", "sublocality_level_1",
			"sublocality_level_2", "administrative_area_level_3",
			"neighborhood", "street_address", "premise"
	);
	private static final Set<String> TOO_COARSE_TYPES = Set.of(
			"administrative_area_level_1", "country", "continent", "archipelago"
	);

	private final RestClient client;
	private final boolean apiKeyConfigured;
	private final String apiKey;
	private final String region;
	private final String countryBias;

	public GoogleMapsClient(RestClient googleMapsClient,
							@Value("${app.routing.google.api-key:}") String apiKey,
							@Value("${app.routing.google.region:in}") String region,
							@Value("${app.routing.google.country-bias:in}") String countryBias) {
		this.client = googleMapsClient;
		this.apiKey = apiKey == null ? "" : apiKey;
		this.apiKeyConfigured = !this.apiKey.isBlank();
		this.region = region == null ? "" : region.trim();
		this.countryBias = countryBias == null ? "" : countryBias.trim();
	}

	public GeoPoint geocode(String text) {
		ensureKey();
		GeocodeResponse resp = call(() -> client.get()
				.uri(uri -> {
					var b = uri.path("/maps/api/geocode/json")
							.queryParam("address", text)
							.queryParam("key", apiKey);
					if (!region.isEmpty()) {
						b.queryParam("region", region);
					}
					if (!countryBias.isEmpty()) {
						// Restricts results to the given country (ISO alpha-2,
						// lowercased). Prevents "Bomdila" / "Tezpur" matching
						// places on other continents.
						b.queryParam("components", "country:" + countryBias);
					}
					return b.build();
				})
				.retrieve()
				.body(GeocodeResponse.class), "geocode '" + text + "'");
		checkStatus(resp, "geocode '" + text + "'");
		if (resp.results == null || resp.results.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Could not geocode location: '" + text
							+ "'. Try a specific town/city name (e.g. 'Gangtok' instead of 'Sikkim').");
		}

		// Pass 1: any precise (town/subdistrict-level) result wins, even if
		// Google ranked a coarser admin polygon first.
		for (GeocodeResult r : resp.results) {
			if (r.types != null && r.types.stream().anyMatch(PRECISE_TYPES::contains)) {
				return toGeoPoint(r, text);
			}
		}

		// Pass 2: only state/country left — too vague to route from.
		GeocodeResult top = resp.results.get(0);
		if (top.types != null && top.types.stream().anyMatch(TOO_COARSE_TYPES::contains)) {
			String layer = top.types.stream().filter(TOO_COARSE_TYPES::contains).findFirst().orElse("region");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"'" + text + "' is a " + layer
							+ " (too vague to route from). Please enter a specific town or "
							+ "city — e.g. 'Gangtok' instead of 'Sikkim', 'Guwahati' instead of 'Assam'.");
		}
		// Medium-coarse (district / admin_area_level_2) — useable centroid.
		log.warn("Geocode '{}' resolved only to types {} (no town-level match)",
				text, top.types);
		return toGeoPoint(top, text);
	}

	public String reverseGeocode(double lat, double lng) {
		ensureKey();
		String label = reverseOnce(lat, lng, "locality|sublocality|administrative_area_level_3");
		if (label != null) {
			return label;
		}
		// Fallback: any result — accept road/POI rather than raw coords.
		label = reverseOnce(lat, lng, null);
		if (label != null) {
			return label;
		}
		return String.format("%.4f, %.4f", lat, lng);
	}

	private String reverseOnce(double lat, double lng, String resultType) {
		GeocodeResponse resp = call(() -> client.get()
				.uri(uri -> {
					var b = uri.path("/maps/api/geocode/json")
							.queryParam("latlng", lat + "," + lng)
							.queryParam("key", apiKey);
					if (resultType != null) {
						b.queryParam("result_type", resultType);
					}
					return b.build();
				})
				.retrieve()
				.body(GeocodeResponse.class), "reverse-geocode " + lat + "," + lng);
		if (resp == null || "ZERO_RESULTS".equals(resp.status)
				|| resp.results == null || resp.results.isEmpty()) {
			return null;
		}
		checkStatus(resp, "reverse-geocode " + lat + "," + lng);
		// Prefer the most specific result that's not a plus-code.
		for (GeocodeResult r : resp.results) {
			if (r.types == null || !r.types.contains("plus_code")) {
				return shortLabel(r);
			}
		}
		return shortLabel(resp.results.get(0));
	}

	public DirectionsResponse directions(GeoPoint origin, GeoPoint destination,
										  List<GeoPoint> waypoints, List<String> avoidFeatures) {
		ensureKey();
		String waypointParam = (waypoints == null || waypoints.isEmpty())
				? null
				: waypoints.stream()
						.map(p -> p.lat() + "," + p.lng())
						.collect(Collectors.joining("|"));
		String avoidParam = (avoidFeatures == null || avoidFeatures.isEmpty())
				? null
				: String.join("|", avoidFeatures);

		DirectionsResponse resp = call(() -> client.get()
				.uri(uri -> {
					var b = uri.path("/maps/api/directions/json")
							.queryParam("origin", origin.lat() + "," + origin.lng())
							.queryParam("destination", destination.lat() + "," + destination.lng())
							.queryParam("mode", "driving")
							.queryParam("key", apiKey);
					if (waypointParam != null) {
						b.queryParam("waypoints", waypointParam);
					}
					if (avoidParam != null) {
						b.queryParam("avoid", avoidParam);
					}
					if (!region.isEmpty()) {
						b.queryParam("region", region);
					}
					return b.build();
				})
				.retrieve()
				.body(DirectionsResponse.class), "directions");
		checkStatus(resp, "directions");
		if (resp.routes == null || resp.routes.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Google Maps returned no route for the given coordinates.");
		}
		return resp;
	}

	private void ensureKey() {
		if (!apiKeyConfigured) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Google Maps API key not configured. Set GOOGLE_MAPS_API_KEY "
							+ "and enable Directions API + Geocoding API on the GCP project.");
		}
	}

	private static void checkStatus(StatusResponse resp, String what) {
		if (resp == null || resp.status == null || "OK".equals(resp.status)) {
			return;
		}
		String detail = resp.error_message != null ? ": " + resp.error_message : "";
		switch (resp.status) {
			case "ZERO_RESULTS" -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Google Maps " + what + " returned no results" + detail);
			case "REQUEST_DENIED" -> throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Google Maps " + what + " denied" + detail
							+ " — check the API key and that Directions API + Geocoding API are enabled.");
			case "OVER_QUERY_LIMIT" -> throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Google Maps " + what + " quota exceeded" + detail);
			case "INVALID_REQUEST" -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Google Maps " + what + " rejected the request" + detail);
			default -> throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Google Maps " + what + " failed (" + resp.status + ")" + detail);
		}
	}

	private <T> T call(java.util.function.Supplier<T> op, String what) {
		try {
			return op.get();
		}
		catch (org.springframework.web.client.RestClientResponseException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Google Maps " + what + " HTTP " + ex.getStatusCode().value()
							+ ": " + ex.getResponseBodyAsString(), ex);
		}
		catch (org.springframework.web.client.RestClientException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Google Maps " + what + " unreachable: " + ex.getMessage(), ex);
		}
	}

	private static GeoPoint toGeoPoint(GeocodeResult r, String fallbackLabel) {
		double lat = r.geometry != null && r.geometry.location != null ? r.geometry.location.lat : 0;
		double lng = r.geometry != null && r.geometry.location != null ? r.geometry.location.lng : 0;
		String label = r.formatted_address != null ? shortLabel(r) : fallbackLabel;
		return new GeoPoint(lat, lng, label);
	}

	/** Compact label: the first 1-3 comma-separated parts of formatted_address. */
	private static String shortLabel(GeocodeResult r) {
		if (r == null || r.formatted_address == null) {
			return "";
		}
		String[] parts = r.formatted_address.split(",");
		if (parts.length <= 3) {
			return r.formatted_address.trim();
		}
		// Drop the postcode/country tail; keep "Town, District, State".
		return (parts[0] + "," + parts[1] + "," + parts[2]).trim();
	}

	public record GeoPoint(double lat, double lng, String label) {
	}

	// ---- Minimal JSON shapes for Google responses ------------------------------

	/** Common base for {@code status} / {@code error_message}. */
	public static class StatusResponse {
		public String status;
		public String error_message;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class GeocodeResponse extends StatusResponse {
		public List<GeocodeResult> results;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class GeocodeResult {
		public String formatted_address;
		public List<String> types;
		public Geometry geometry;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Geometry {
		public LatLng location;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class LatLng {
		public double lat;
		public double lng;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsResponse extends StatusResponse {
		public List<DirectionsRoute> routes;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsRoute {
		public List<DirectionsLeg> legs;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsLeg {
		public String start_address;
		public String end_address;
		public LatLng start_location;
		public LatLng end_location;
		public Measure distance;
		public Measure duration;
		public List<DirectionsStep> steps;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsStep {
		public Measure distance;
		public Measure duration;
		public LatLng start_location;
		public LatLng end_location;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Measure {
		/** meters (distance) or seconds (duration) */
		public double value;
		public String text;
		@JsonProperty("value")
		public void setValue(double value) { this.value = value; }
	}
}
