package com.example.travelplanner;

import com.example.travelplanner.dto.PlanTripRequest;
import com.example.travelplanner.dto.PlanTripResponse;
import com.example.travelplanner.dto.SimilarTripResponse;
import com.example.travelplanner.service.TripPlanningService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TripPlanningServiceTest {

	private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
	private final VectorStore vectorStore = mock(VectorStore.class);

	private TripPlanningService newService() {
		return new TripPlanningService(
				chatClient,
				vectorStore,
				new ClassPathResource("prompts/plan-user-prompt.st"),
				4,
				0.5,
				"gpt-4o-mini");
	}

	@Test
	void planUsesSimilarTripsAsContextAndReturnsItinerary() {
		Document past = Document.builder()
				.text("Trip to Leh (from Manali)\nInterests: high passes")
				.metadata(Map.of("tripId", 7L, "destination", "Leh", "interests", "high passes"))
				.build();
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(past));
		when(chatClient.prompt().user(any(Consumer.class)).call().content())
				.thenReturn("Day 1: ...");

		PlanTripRequest request = new PlanTripRequest(
				"Manali", "Leh", "Jispa, Sarchu",
				LocalDate.of(2025, 7, 5), LocalDate.of(2025, 7, 8),
				"Royal Enfield Himalayan", "experienced", 180, 250,
				"high-altitude passes", false, false,
				"glaciers, remote villages", "moderate", null);

		PlanTripResponse response = newService().plan(request);

		assertThat(response.itinerary()).isEqualTo("Day 1: ...");
		assertThat(response.destination()).isEqualTo("Leh");
		assertThat(response.model()).isEqualTo("gpt-4o-mini");
		assertThat(response.usedContext()).hasSize(1);
		SimilarTripResponse ctx = response.usedContext().get(0);
		assertThat(ctx.tripId()).isEqualTo(7L);
		assertThat(ctx.destination()).isEqualTo("Leh");
	}

	@Test
	void findSimilarMapsDocumentsToResponses() {
		Document doc = Document.builder()
				.text("Trip to Lisbon")
				.metadata(Map.of("tripId", 3L, "destination", "Lisbon", "interests", "seafood"))
				.build();
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

		List<SimilarTripResponse> result = newService().findSimilar("seafood in Portugal", 5);

		assertThat(result).singleElement()
				.satisfies(r -> {
					assertThat(r.tripId()).isEqualTo(3L);
					assertThat(r.destination()).isEqualTo("Lisbon");
					assertThat(r.snippet()).contains("Lisbon");
				});
	}
}
