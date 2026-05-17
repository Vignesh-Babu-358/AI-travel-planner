package com.example.travelplanner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

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

	private LocalDate startDate;

	private LocalDate endDate;

	private String interests;

	private String budget;

	@Column(columnDefinition = "TEXT")
	private String itinerary;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	protected Trip() {
	}

	public Trip(String origin, String destination, LocalDate startDate, LocalDate endDate,
				String interests, String budget, String itinerary) {
		this.origin = origin;
		this.destination = destination;
		this.startDate = startDate;
		this.endDate = endDate;
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

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
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
