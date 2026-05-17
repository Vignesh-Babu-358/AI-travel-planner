package com.example.travelplanner.service;

import com.example.travelplanner.domain.Trip;
import com.example.travelplanner.dto.SaveTripRequest;
import com.example.travelplanner.dto.TripResponse;
import com.example.travelplanner.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Persists trips and embeds them into the vector store so they become
 * retrievable RAG context for future plan requests.
 */
@Service
public class TripService {

	private static final Logger log = LoggerFactory.getLogger(TripService.class);

	private final TripRepository tripRepository;
	private final VectorStore vectorStore;

	public TripService(TripRepository tripRepository, VectorStore vectorStore) {
		this.tripRepository = tripRepository;
		this.vectorStore = vectorStore;
	}

	@Transactional
	public TripResponse save(SaveTripRequest request) {
		Trip trip = new Trip(
				request.origin(),
				request.destination(),
				request.startDate(),
				request.endDate(),
				request.interests(),
				request.budget(),
				request.itinerary());
		Trip saved = tripRepository.save(trip);

		// Embed into PGVector (OpenAI embeddings happen inside VectorStore.add).
		Document document = TripDocuments.toDocument(saved);
		vectorStore.add(List.of(document));
		log.info("Saved trip {} and embedded it into the vector store", saved.getId());

		return TripResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public List<TripResponse> list() {
		return tripRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
				.map(TripResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public TripResponse get(Long id) {
		Trip trip = tripRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Trip not found: " + id));
		return TripResponse.from(trip);
	}
}
