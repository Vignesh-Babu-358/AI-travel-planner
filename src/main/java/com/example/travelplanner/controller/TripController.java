package com.example.travelplanner.controller;

import com.example.travelplanner.dto.PlanTripRequest;
import com.example.travelplanner.dto.PlanTripResponse;
import com.example.travelplanner.dto.SaveTripRequest;
import com.example.travelplanner.dto.SimilarTripResponse;
import com.example.travelplanner.dto.TripResponse;
import com.example.travelplanner.service.TripPlanningService;
import com.example.travelplanner.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@Tag(name = "Trips", description = "AI itinerary generation, persistence and RAG similarity search")
public class TripController {

	private final TripPlanningService planningService;
	private final TripService tripService;

	public TripController(TripPlanningService planningService, TripService tripService) {
		this.planningService = planningService;
		this.tripService = tripService;
	}

	@PostMapping("/plan")
	@Operation(summary = "Generate an itinerary",
			description = "Uses the OpenAI LLM grounded with RAG context from similar past trips.")
	public PlanTripResponse plan(@Valid @RequestBody PlanTripRequest request) {
		return planningService.plan(request);
	}

	@PostMapping
	@Operation(summary = "Save a trip",
			description = "Persists the trip and embeds it into PGVector for future RAG retrieval.")
	public ResponseEntity<TripResponse> save(@Valid @RequestBody SaveTripRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(tripService.save(request));
	}

	@GetMapping
	@Operation(summary = "List all saved trips (newest first)")
	public List<TripResponse> list() {
		return tripService.list();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a saved trip by id")
	public TripResponse get(@PathVariable Long id) {
		return tripService.get(id);
	}

	@GetMapping("/similar")
	@Operation(summary = "Find semantically similar past trips")
	public List<SimilarTripResponse> similar(
			@RequestParam String query,
			@RequestParam(defaultValue = "5") int k) {
		return planningService.findSimilar(query, k);
	}
}
