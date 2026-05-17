package com.example.travelplanner.config;

import com.example.travelplanner.dto.SaveTripRequest;
import com.example.travelplanner.repository.TripRepository;
import com.example.travelplanner.service.TripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds a handful of past trips on first start (when the trips table is empty)
 * so RAG retrieval has content to surface. Embedding these requires a valid
 * OPENAI_API_KEY; failures are logged but do not stop the application.
 */
@Configuration
@ConditionalOnProperty(name = "app.seed-sample-trips", havingValue = "true", matchIfMissing = true)
public class SampleTripSeeder {

	private static final Logger log = LoggerFactory.getLogger(SampleTripSeeder.class);

	@Bean
	ApplicationRunner seedSampleTrips(TripRepository repository, TripService tripService) {
		return args -> {
			if (repository.count() > 0) {
				log.info("Trips already present ({}); skipping sample seeding", repository.count());
				return;
			}
			List<SaveTripRequest> samples = List.of(
					new SaveTripRequest("London", "Kyoto", LocalDate.of(2025, 4, 1),
							LocalDate.of(2025, 4, 5), "temples, food, gardens", "moderate",
							"Day 1: Arrival, Gion evening walk. Day 2: Fushimi Inari at dawn, "
									+ "Nishiki Market lunch. Day 3: Arashiyama bamboo grove and "
									+ "Tenryu-ji. Day 4: Kinkaku-ji, tea ceremony. Day 5: Departure."),
					new SaveTripRequest("New York", "Lisbon", LocalDate.of(2025, 6, 10),
							LocalDate.of(2025, 6, 14), "history, seafood, viewpoints", "budget",
							"Day 1: Alfama and Sao Jorge castle. Day 2: Belem (tower, pasteis). "
									+ "Day 3: Sintra day trip. Day 4: LX Factory and Time Out Market."),
					new SaveTripRequest("Berlin", "Reykjavik", LocalDate.of(2025, 9, 2),
							LocalDate.of(2025, 9, 6), "nature, hot springs, photography", "premium",
							"Day 1: Reykjavik old town. Day 2: Golden Circle. Day 3: South coast "
									+ "waterfalls and black sand beach. Day 4: Blue Lagoon."),
					new SaveTripRequest("Paris", "Rome", LocalDate.of(2025, 5, 20),
							LocalDate.of(2025, 5, 24), "art, ancient history, food", "moderate",
							"Day 1: Colosseum and Roman Forum. Day 2: Vatican Museums and "
									+ "St Peter's. Day 3: Trastevere food walk. Day 4: Borghese Gallery."),
					new SaveTripRequest("Toronto", "Barcelona", LocalDate.of(2025, 7, 8),
							LocalDate.of(2025, 7, 12), "architecture, beaches, tapas", "moderate",
							"Day 1: Gothic Quarter. Day 2: Sagrada Familia and Park Guell. "
									+ "Day 3: Barceloneta beach. Day 4: Montjuic and tapas crawl."));

			int ok = 0;
			for (SaveTripRequest sample : samples) {
				try {
					tripService.save(sample);
					ok++;
				}
				catch (Exception ex) {
					log.warn("Failed to seed sample trip to {}: {}",
							sample.destination(), ex.getMessage());
				}
			}
			log.info("Seeded {}/{} sample trips into the vector store", ok, samples.size());
		};
	}
}
