package com.example.travelplanner.service.routing;

import com.example.travelplanner.dto.PlanTripRequest;
import com.example.travelplanner.dto.RouteDayLeg;
import com.example.travelplanner.dto.RouteSummary;
import com.example.travelplanner.service.routing.RoutingClient.DirectionsFeature;
import com.example.travelplanner.service.routing.RoutingClient.DirectionsResponse;
import com.example.travelplanner.service.routing.RoutingClient.DirectionsSegment;
import com.example.travelplanner.service.routing.RoutingClient.DirectionsStep;
import com.example.travelplanner.service.routing.RoutingClient.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the real route lookup: geocode endpoints + waypoints,
 * call directions, then chunk the polyline into daily legs of at most
 * {@code maxDailyDistanceKm} ending at a real reverse-geocoded town.
 * The {@link RouteSummary} it returns is the ground truth fed to the LLM.
 */
@Service
public class RoutingService {

	private static final Logger log = LoggerFactory.getLogger(RoutingService.class);
	private static final double DEFAULT_DAILY_KM = 250.0;

	private final RoutingClient client;

	public RoutingService(RoutingClient client) {
		this.client = client;
	}

	public RouteSummary planRoute(PlanTripRequest request) {
		// 1. Geocode origin, destination, and any comma-separated waypoints.
		List<GeoPoint> points = new ArrayList<>();
		points.add(client.geocode(request.origin()));
		if (request.waypoints() != null && !request.waypoints().isBlank()) {
			for (String w : request.waypoints().split(",")) {
				String trimmed = w.trim();
				if (!trimmed.isEmpty()) {
					points.add(client.geocode(trimmed));
				}
			}
		}
		points.add(client.geocode(request.destination()));

		List<double[]> coords = points.stream()
				.map(p -> new double[] { p.lon(), p.lat() })
				.toList();

		List<String> avoid = new ArrayList<>();
		if (Boolean.TRUE.equals(request.avoidHighways())) {
			avoid.add("highways");
		}
		if (Boolean.TRUE.equals(request.avoidTolls())) {
			avoid.add("tollways");
		}

		// 2. Real route from ORS.
		DirectionsResponse resp = client.directions(coords, avoid);
		if (resp == null || resp.features == null || resp.features.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"OpenRouteService returned no route for the given coordinates.");
		}
		DirectionsFeature feature = resp.features.get(0);
		List<double[]> geometry = feature.geometry.coordinates;
		double totalKm = feature.properties.summary.distance / 1000.0;
		double totalHours = feature.properties.summary.duration / 3600.0;

		// 3. Chunk into daily legs.
		double targetKm = request.maxDailyDistanceKm() != null && request.maxDailyDistanceKm() > 0
				? request.maxDailyDistanceKm().doubleValue()
				: DEFAULT_DAILY_KM;

		List<RouteDayLeg> days = chunkIntoDays(
				feature.properties.segments,
				geometry,
				targetKm,
				points.get(0),
				points.get(points.size() - 1));

		log.info("Routed {} -> {} via {} waypoints: {} km, {} (split into {} day(s))",
				request.origin(), request.destination(), points.size() - 2,
				String.format("%.1f", totalKm), formatHours(totalHours), days.size());

		return new RouteSummary("driving-car", round1(totalKm), formatHours(totalHours), days);
	}

	private List<RouteDayLeg> chunkIntoDays(List<DirectionsSegment> segments,
											List<double[]> geometry,
											double targetKm,
											GeoPoint origin,
											GeoPoint destination) {
		List<RouteDayLeg> days = new ArrayList<>();
		double dayDistM = 0;
		double dayDurS = 0;
		double[] dayStartCoord = geometry.get(0);
		String dayStartName = origin.label();

		for (DirectionsSegment segment : segments) {
			if (segment.steps == null) continue;
			for (DirectionsStep step : segment.steps) {
				dayDistM += step.distance;
				dayDurS += step.duration;
				if (dayDistM / 1000.0 >= targetKm) {
					int endIdx = (step.way_points != null && step.way_points.length >= 2)
							? step.way_points[1]
							: geometry.size() - 1;
					endIdx = Math.min(endIdx, geometry.size() - 1);
					double[] endCoord = geometry.get(endIdx);
					String endName = client.reverseGeocode(endCoord[0], endCoord[1]);

					days.add(new RouteDayLeg(
							days.size() + 1,
							dayStartName,
							endName,
							round1(dayDistM / 1000.0),
							formatHours(dayDurS / 3600.0),
							dayStartCoord,
							endCoord));

					dayDistM = 0;
					dayDurS = 0;
					dayStartCoord = endCoord;
					dayStartName = endName;
				}
			}
		}
		// Tail: any remaining distance becomes the final day, ending at the destination label.
		if (dayDistM > 0 || days.isEmpty()) {
			double[] endCoord = geometry.get(geometry.size() - 1);
			days.add(new RouteDayLeg(
					days.size() + 1,
					dayStartName,
					destination.label(),
					round1(dayDistM / 1000.0),
					formatHours(dayDurS / 3600.0),
					dayStartCoord,
					endCoord));
		}
		else {
			// Rename the final day's end to the destination label for accuracy.
			RouteDayLeg last = days.remove(days.size() - 1);
			days.add(new RouteDayLeg(last.day(), last.startName(), destination.label(),
					last.distanceKm(), last.duration(), last.startLngLat(), last.endLngLat()));
		}
		return days;
	}

	/** A compact Markdown block the prompt template injects as ground truth. */
	public static String toMarkdown(RouteSummary route) {
		StringBuilder sb = new StringBuilder();
		sb.append("Total: ").append(route.totalDistanceKm()).append(" km, ")
				.append(route.totalDuration()).append(" (profile: ").append(route.profile())
				.append(")\n\n");
		sb.append("| Day | From | To | km | Ride time |\n");
		sb.append("|---:|---|---|---:|---|\n");
		for (RouteDayLeg d : route.days()) {
			sb.append("| ").append(d.day())
					.append(" | ").append(d.startName())
					.append(" | ").append(d.endName())
					.append(" | ").append(d.distanceKm())
					.append(" | ").append(d.duration())
					.append(" |\n");
		}
		return sb.toString();
	}

	private static double round1(double v) {
		return Math.round(v * 10.0) / 10.0;
	}

	private static String formatHours(double hours) {
		int h = (int) Math.floor(hours);
		int m = (int) Math.round((hours - h) * 60);
		if (m == 60) {
			h++;
			m = 0;
		}
		return h + "h " + m + "m";
	}

}
