package com.example.travelplanner.service.routing;

import com.example.travelplanner.dto.PlanTripRequest;
import com.example.travelplanner.dto.RouteDayLeg;
import com.example.travelplanner.dto.RouteSummary;
import com.example.travelplanner.service.routing.GoogleMapsClient.DirectionsLeg;
import com.example.travelplanner.service.routing.GoogleMapsClient.DirectionsResponse;
import com.example.travelplanner.service.routing.GoogleMapsClient.DirectionsStep;
import com.example.travelplanner.service.routing.GoogleMapsClient.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the real route lookup: geocode endpoints + waypoints,
 * call Google Maps Directions, then chunk the polyline into daily legs
 * of at most {@code maxDailyDistanceKm} ending at a real reverse-geocoded
 * town. The {@link RouteSummary} it returns is the ground truth fed to
 * the LLM.
 */
@Service
public class RoutingService {

	private static final Logger log = LoggerFactory.getLogger(RoutingService.class);
	private static final double DEFAULT_DAILY_KM = 250.0;

	private final GoogleMapsClient client;

	public RoutingService(GoogleMapsClient client) {
		this.client = client;
	}

	public RouteSummary planRoute(PlanTripRequest request) {
		// 1. Geocode origin, destination, and any comma-separated waypoints.
		GeoPoint origin = client.geocode(request.origin());
		GeoPoint destination = client.geocode(request.destination());
		List<GeoPoint> waypoints = new ArrayList<>();
		if (request.waypoints() != null && !request.waypoints().isBlank()) {
			for (String w : request.waypoints().split(",")) {
				String trimmed = w.trim();
				if (!trimmed.isEmpty()) {
					waypoints.add(client.geocode(trimmed));
				}
			}
		}
		log.info("Routing {} -> {} via {} waypoint(s)",
				origin.label(), destination.label(), waypoints.size());
		for (GeoPoint p : waypoints) {
			log.info("  waypoint -> {} ({}, {})", p.label(), p.lat(), p.lng());
		}

		// 2. Build avoid_features list.
		List<String> avoid = new ArrayList<>();
		if (Boolean.TRUE.equals(request.avoidHighways())) {
			avoid.add("highways");
		}
		if (Boolean.TRUE.equals(request.avoidTolls())) {
			avoid.add("tolls");
		}

		// 3. Real route from Google.
		DirectionsResponse resp = client.directions(origin, destination, waypoints, avoid);
		List<DirectionsLeg> legs = resp.routes.get(0).legs;

		double totalMeters = legs.stream().mapToDouble(l -> l.distance.value).sum();
		double totalSeconds = legs.stream().mapToDouble(l -> l.duration.value).sum();
		double totalKm = totalMeters / 1000.0;
		double totalHours = totalSeconds / 3600.0;

		// 4. Chunk into daily legs.
		double targetKm = request.maxDailyDistanceKm() != null && request.maxDailyDistanceKm() > 0
				? request.maxDailyDistanceKm().doubleValue()
				: DEFAULT_DAILY_KM;

		List<RouteDayLeg> days = waypoints.isEmpty()
				? chunkIntoDays(legs, targetKm, origin, destination)
				: chunkByWaypoints(legs, origin, destination, waypoints);

		log.info("Routed: {} km, {} (split into {} day(s))",
				String.format("%.1f", totalKm), formatHours(totalHours), days.size());

		return new RouteSummary("driving", round1(totalKm), formatHours(totalHours), days);
	}

	/**
	 * When the user gives explicit waypoints, treat each waypoint as a day
	 * boundary instead of chunking by distance. Google's Directions response
	 * already returns one {@link DirectionsLeg} per consecutive waypoint pair,
	 * so the legs map 1:1 to days. End-town labels come from the user's
	 * geocoded waypoint names — no reverse geocode needed, and the table
	 * surfaces exactly what the user typed (e.g. "Bangalore, Pune, Mumbai").
	 */
	private List<RouteDayLeg> chunkByWaypoints(List<DirectionsLeg> legs,
											   GeoPoint origin,
											   GeoPoint destination,
											   List<GeoPoint> waypoints) {
		List<String> labels = new java.util.ArrayList<>();
		labels.add(origin.label());
		for (GeoPoint wp : waypoints) {
			labels.add(wp.label());
		}
		labels.add(destination.label());

		List<RouteDayLeg> days = new ArrayList<>();
		for (int i = 0; i < legs.size(); i++) {
			DirectionsLeg leg = legs.get(i);
			double km = leg.distance.value / 1000.0;
			double hours = leg.duration.value / 3600.0;
			double startLat = leg.start_location != null ? leg.start_location.lat : 0;
			double startLng = leg.start_location != null ? leg.start_location.lng : 0;
			double endLat = leg.end_location != null ? leg.end_location.lat : 0;
			double endLng = leg.end_location != null ? leg.end_location.lng : 0;
			days.add(new RouteDayLeg(
					i + 1,
					labels.get(i),
					labels.get(i + 1),
					round1(km),
					formatHours(hours),
					new double[] { startLat, startLng },
					new double[] { endLat, endLng }));
		}
		return days;
	}

	private List<RouteDayLeg> chunkIntoDays(List<DirectionsLeg> legs,
											double targetKm,
											GeoPoint origin,
											GeoPoint destination) {
		List<RouteDayLeg> days = new ArrayList<>();
		double dayDistM = 0;
		double dayDurS = 0;
		double dayStartLat = origin.lat();
		double dayStartLng = origin.lng();
		String dayStartName = origin.label();
		double prevStepEndLat = origin.lat();
		double prevStepEndLng = origin.lng();

		for (DirectionsLeg leg : legs) {
			if (leg.steps == null) continue;
			for (DirectionsStep step : leg.steps) {
				double stepKm = step.distance.value / 1000.0;
				// Pre-check: if adding this step would push the day past the
				// daily cap and we already have some distance today, close the
				// day at the END of the previous step rather than overshooting.
				if (dayDistM > 0 && (dayDistM / 1000.0 + stepKm) > targetKm) {
					String endName = client.reverseGeocode(prevStepEndLat, prevStepEndLng);
					days.add(new RouteDayLeg(
							days.size() + 1,
							dayStartName,
							endName,
							round1(dayDistM / 1000.0),
							formatHours(dayDurS / 3600.0),
							new double[] { dayStartLat, dayStartLng },
							new double[] { prevStepEndLat, prevStepEndLng }));
					dayDistM = 0;
					dayDurS = 0;
					dayStartLat = prevStepEndLat;
					dayStartLng = prevStepEndLng;
					dayStartName = endName;
				}
				dayDistM += step.distance.value;
				dayDurS += step.duration.value;
				if (step.end_location != null) {
					prevStepEndLat = step.end_location.lat;
					prevStepEndLng = step.end_location.lng;
				}
			}
		}
		// Tail: remaining distance becomes the final day, ending at destination.
		if (dayDistM > 0 || days.isEmpty()) {
			days.add(new RouteDayLeg(
					days.size() + 1,
					dayStartName,
					destination.label(),
					round1(dayDistM / 1000.0),
					formatHours(dayDurS / 3600.0),
					new double[] { dayStartLat, dayStartLng },
					new double[] { destination.lat(), destination.lng() }));
		}
		else {
			// Rename final day's end to the destination label for accuracy.
			RouteDayLeg last = days.remove(days.size() - 1);
			days.add(new RouteDayLeg(last.day(), last.startName(), destination.label(),
					last.distanceKm(), last.duration(), last.startLatLng(), last.endLatLng()));
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
