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
				: chunkByWaypoints(legs, targetKm, origin, destination, waypoints);

		log.info("Routed: {} km, {} (split into {} day(s))",
				String.format("%.1f", totalKm), formatHours(totalHours), days.size());

		return new RouteSummary("driving", round1(totalKm), formatHours(totalHours), days);
	}

	/**
	 * Mixed mode: each user waypoint is a guaranteed day boundary (so the
	 * table shows what the user typed — "Bangalore", "Pune"), AND if a single
	 * leg between two waypoints is longer than {@code targetKm} we sub-chunk
	 * it into multiple days with reverse-geocoded intermediate towns.
	 *
	 * Example with maxDailyDistanceKm=300, waypoints "Bangalore, Pune"
	 * for Chennai -> Mumbai:
	 *   Day 1: Chennai -> Bangalore (~340 km, waypoint boundary)
	 *   Day 2: Bangalore -> &lt;reverse-geocoded town near 300 km mark&gt;
	 *   Day 3: &lt;that town&gt; -> &lt;another mid-leg town&gt;
	 *   Day 4: &lt;that town&gt; -> Pune (waypoint boundary)
	 *   Day 5: Pune -> Mumbai (~150 km)
	 */
	private List<RouteDayLeg> chunkByWaypoints(List<DirectionsLeg> legs,
											   double targetKm,
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
		for (int legIdx = 0; legIdx < legs.size(); legIdx++) {
			DirectionsLeg leg = legs.get(legIdx);
			String legStartLabel = labels.get(legIdx);
			String legEndLabel = labels.get(legIdx + 1);
			double legStartLat = leg.start_location != null ? leg.start_location.lat : 0;
			double legStartLng = leg.start_location != null ? leg.start_location.lng : 0;
			double legEndLat = leg.end_location != null ? leg.end_location.lat : 0;
			double legEndLng = leg.end_location != null ? leg.end_location.lng : 0;

			double dayDistM = 0;
			double dayDurS = 0;
			double dayStartLat = legStartLat;
			double dayStartLng = legStartLng;
			String dayStartName = legStartLabel;
			double prevStepEndLat = legStartLat;
			double prevStepEndLng = legStartLng;

			if (leg.steps != null) {
				for (DirectionsStep step : leg.steps) {
					double stepKm = step.distance.value / 1000.0;
					// Pre-check threshold: close before overshooting if we
					// already have some distance and the next step would push
					// us over the daily cap.
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

			// End of leg: close the day at the waypoint label, regardless of
			// remaining distance. This is what makes waypoints "guaranteed
			// stops" — even a short remainder becomes its own short day if
			// that's what reaching the waypoint requires.
			days.add(new RouteDayLeg(
					days.size() + 1,
					dayStartName,
					legEndLabel,
					round1(dayDistM / 1000.0),
					formatHours(dayDurS / 3600.0),
					new double[] { dayStartLat, dayStartLng },
					new double[] { legEndLat, legEndLng }));
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
