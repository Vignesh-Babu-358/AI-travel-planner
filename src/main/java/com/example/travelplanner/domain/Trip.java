package com.example.travelplanner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A motorcycle road trip. {@code origin} is the ride start and
 * {@code destination} the end/primary goal (a loop is allowed). All road-trip
 * attributes are optional/nullable so existing rows survive a schema update.
 */
@Entity
@Table(name = "trips")
public class Trip {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String origin;

	@Column(nullable = false)
	private String destination;

	private String waypoints;

	private LocalDate startDate;

	private LocalDate endDate;

	private String motorcycleModel;

	private String ridingExperience;

	private Integer maxDailyDistanceKm;

	private Integer fuelRangeKm;

	private String routePreference;

	private Boolean avoidHighways;

	private Boolean avoidTolls;

	private String interests;

	private String budget;

	@Column(columnDefinition = "TEXT")
	private String itinerary;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	protected Trip() {
	}

	public Trip(String origin, String destination, String waypoints,
				LocalDate startDate, LocalDate endDate,
				String motorcycleModel, String ridingExperience,
				Integer maxDailyDistanceKm, Integer fuelRangeKm,
				String routePreference, Boolean avoidHighways, Boolean avoidTolls,
				String interests, String budget, String itinerary) {
		this.origin = origin;
		this.destination = destination;
		this.waypoints = waypoints;
		this.startDate = startDate;
		this.endDate = endDate;
		this.motorcycleModel = motorcycleModel;
		this.ridingExperience = ridingExperience;
		this.maxDailyDistanceKm = maxDailyDistanceKm;
		this.fuelRangeKm = fuelRangeKm;
		this.routePreference = routePreference;
		this.avoidHighways = avoidHighways;
		this.avoidTolls = avoidTolls;
		this.interests = interests;
		this.budget = budget;
		this.itinerary = itinerary;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getOrigin() {
		return origin;
	}

	public String getDestination() {
		return destination;
	}

	public String getWaypoints() {
		return waypoints;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public String getMotorcycleModel() {
		return motorcycleModel;
	}

	public String getRidingExperience() {
		return ridingExperience;
	}

	public Integer getMaxDailyDistanceKm() {
		return maxDailyDistanceKm;
	}

	public Integer getFuelRangeKm() {
		return fuelRangeKm;
	}

	public String getRoutePreference() {
		return routePreference;
	}

	public Boolean getAvoidHighways() {
		return avoidHighways;
	}

	public Boolean getAvoidTolls() {
		return avoidTolls;
	}

	public String getInterests() {
		return interests;
	}

	public String getBudget() {
		return budget;
	}

	public String getItinerary() {
		return itinerary;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
