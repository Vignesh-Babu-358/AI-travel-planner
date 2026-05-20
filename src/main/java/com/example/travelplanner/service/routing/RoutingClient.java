package com.example.travelplanner.service.routing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP wrapper around OpenRouteService:
 * <ul>
 *   <li>{@link #geocode(String)} — forward geocode a place name to lon/lat.</li>
 *   <li>{@link #reverseGeocode(double, double)} — nearest place name for a coordinate.</li>
 *   <li>{@link #directions(List, List)} — driving-car route through coordinates.</li>
 * </ul>
 * Any missing API key or upstream failure is surfaced as a 503 so the user
 * sees a clear error instead of a hallucinated route.
 */
@Component
public class RoutingClient {

	private final RestClient client;
	private final boolean apiKeyConfigured;

	public RoutingClient(RestClient openRouteServiceClient,
						 @Value("${app.routing.ors.api-key:}") String apiKey) {
		this.client = openRouteServiceClient;
		this.apiKeyConfigured = apiKey != null && !apiKey.isBlank();
	}

	public GeoPoint geocode(String text) {
		ensureKey();
		GeocodeResponse resp = call(() -> client.get()
				.uri(uri -> uri.path("/geocode/search")
						.queryParam("text", text)
						.queryParam("size", 1)
						.build())
				.retrieve()
				.body(GeocodeResponse.class), "geocode '" + text + "'");
		if (resp == null || resp.features == null || resp.features.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Could not geocode location: '" + text + "'");
		}
		GeocodeFeature first = resp.features.get(0);
		return new GeoPoint(first.geometry.coordinates[0], first.geometry.coordinates[1],
				first.properties != null ? first.properties.label : text);
	}

	public String reverseGeocode(double lon, double lat) {
		ensureKey();
		GeocodeResponse resp = call(() -> client.get()
				.uri(uri -> uri.path("/geocode/reverse")
						.queryParam("point.lon", lon)
						.queryParam("point.lat", lat)
						.queryParam("size", 1)
						.build())
				.retrieve()
				.body(GeocodeResponse.class), "reverse-geocode " + lon + "," + lat);
		if (resp == null || resp.features == null || resp.features.isEmpty()
				|| resp.features.get(0).properties == null) {
			return String.format("%.4f, %.4f", lat, lon);
		}
		return resp.features.get(0).properties.label;
	}

	public DirectionsResponse directions(List<double[]> coordinates, List<String> avoidFeatures) {
		ensureKey();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("coordinates", coordinates);
		body.put("instructions", true);
		if (avoidFeatures != null && !avoidFeatures.isEmpty()) {
			body.put("options", Map.of("avoid_features", avoidFeatures));
		}
		return call(() -> client.post()
				.uri("/v2/directions/driving-car/geojson")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(DirectionsResponse.class), "directions");
	}

	private void ensureKey() {
		if (!apiKeyConfigured) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Routing API key not configured. Set ORS_API_KEY environment "
							+ "variable (free key at https://openrouteservice.org/dev).");
		}
	}

	private <T> T call(java.util.function.Supplier<T> op, String what) {
		try {
			return op.get();
		}
		catch (org.springframework.web.client.RestClientResponseException ex) {
			HttpStatusCode status = ex.getStatusCode();
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"OpenRouteService " + what + " failed (" + status.value() + "): "
							+ ex.getResponseBodyAsString(), ex);
		}
		catch (org.springframework.web.client.RestClientException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"OpenRouteService " + what + " unreachable: " + ex.getMessage(), ex);
		}
	}

	/** Simple lon/lat + label tuple returned by geocoding. */
	public record GeoPoint(double lon, double lat, String label) {
	}

	// ---- Minimal JSON shapes for ORS responses ---------------------------------

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class GeocodeResponse {
		public List<GeocodeFeature> features;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class GeocodeFeature {
		public GeocodeGeometry geometry;
		public GeocodeProperties properties;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class GeocodeGeometry {
		/** [lon, lat] */
		public double[] coordinates;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class GeocodeProperties {
		public String label;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsResponse {
		public List<DirectionsFeature> features;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsFeature {
		public DirectionsGeometry geometry;
		public DirectionsProperties properties;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsGeometry {
		/** Polyline points as [lon, lat] pairs. */
		public List<double[]> coordinates;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsProperties {
		public DirectionsSummary summary;
		public List<DirectionsSegment> segments;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsSummary {
		/** meters */
		public double distance;
		/** seconds */
		public double duration;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsSegment {
		public List<DirectionsStep> steps;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class DirectionsStep {
		public double distance;
		public double duration;
		/** [startIndex, endIndex] into the geometry coordinates. */
		public int[] way_points;
	}
}
