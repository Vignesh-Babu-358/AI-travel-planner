package com.example.travelplanner.service;

import com.example.travelplanner.dto.PlanTripRequest;
import com.example.travelplanner.dto.PlanTripResponse;
import com.example.travelplanner.dto.SimilarTripResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Core RAG service: retrieves semantically similar past trips from the vector
 * store and uses them to ground the LLM's itinerary generation.
 */
@Service
public class TripPlanningService {

	private static final Logger log = LoggerFactory.getLogger(TripPlanningService.class);

	private final ChatClient chatClient;
	private final VectorStore vectorStore;
	private final Resource userPrompt;
	private final int topK;
	private final double similarityThreshold;
	private final String chatModel;

	public TripPlanningService(
			ChatClient chatClient,
			VectorStore vectorStore,
			@Value("classpath:prompts/plan-user-prompt.st") Resource userPrompt,
			@Value("${app.rag.top-k:4}") int topK,
			@Value("${app.rag.similarity-threshold:0.5}") double similarityThreshold,
			@Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String chatModel) {
		this.chatClient = chatClient;
		this.vectorStore = vectorStore;
		this.userPrompt = userPrompt;
		this.topK = topK;
		this.similarityThreshold = similarityThreshold;
		this.chatModel = chatModel;
	}

	public PlanTripResponse plan(PlanTripRequest request) {
		List<Document> similar = retrieve(buildQuery(request));
		List<SimilarTripResponse> usedContext = similar.stream()
				.map(TripDocuments::toSimilarResponse)
				.toList();

		String context = similar.isEmpty()
				? "(no similar past trips found)"
				: similar.stream().map(Document::getText).reduce("", (a, b) -> a + "\n---\n" + b);

		log.info("Planning trip to {} with {} RAG context document(s)",
				request.destination(), similar.size());

		String itinerary = chatClient.prompt()
				.user(u -> u.text(userPrompt)
						.param("origin", nullSafe(request.origin()))
						.param("destination", nullSafe(request.destination()))
						.param("startDate", String.valueOf(request.startDate()))
						.param("endDate", String.valueOf(request.endDate()))
						.param("durationDays", durationDays(request))
						.param("interests", orDefault(request.interests(), "general sightseeing"))
						.param("budget", orDefault(request.budget(), "moderate"))
						.param("notes", orDefault(request.notes(), "none"))
						.param("context", context))
				.call()
				.content();

		return new PlanTripResponse(request.destination(), chatModel, itinerary, usedContext);
	}

	/** Free-form similarity search over past trips (used by GET /api/trips/similar). */
	public List<SimilarTripResponse> findSimilar(String query, int k) {
		return retrieve(query, k).stream()
				.map(TripDocuments::toSimilarResponse)
				.toList();
	}

	private List<Document> retrieve(String query) {
		return retrieve(query, topK);
	}

	private List<Document> retrieve(String query, int k) {
		SearchRequest searchRequest = SearchRequest.builder()
				.query(query)
				.topK(k)
				.similarityThreshold(similarityThreshold)
				.build();
		List<Document> results = vectorStore.similaritySearch(searchRequest);
		return results == null ? List.of() : results;
	}

	private String buildQuery(PlanTripRequest request) {
		return "Trip to %s from %s. Interests: %s. Budget: %s. Notes: %s".formatted(
				nullSafe(request.destination()),
				nullSafe(request.origin()),
				orDefault(request.interests(), "general"),
				orDefault(request.budget(), "moderate"),
				orDefault(request.notes(), ""));
	}

	private String durationDays(PlanTripRequest request) {
		if (request.startDate() == null || request.endDate() == null) {
			return "unspecified";
		}
		long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
		return days > 0 ? String.valueOf(days) : "unspecified";
	}

	private static String nullSafe(String s) {
		return s == null ? "" : s;
	}

	private static String orDefault(String s, String fallback) {
		return (s == null || s.isBlank()) ? fallback : s;
	}
}
