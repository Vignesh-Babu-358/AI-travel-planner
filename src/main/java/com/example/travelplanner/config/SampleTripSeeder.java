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
					new SaveTripRequest("Manali", "Leh", "Jispa, Sarchu, Pang",
							LocalDate.of(2025, 7, 5), LocalDate.of(2025, 7, 8),
							"Royal Enfield Himalayan 450", "experienced", 180, 250,
							"high-altitude Himalayan passes, gravel sections", false, false,
							"glaciers, remote villages, high passes", "moderate",
							"Day 1: Manali -> Jispa via Atal Tunnel & Keylong (~140 km, 5h). "
									+ "Fuel at Tandi (last pump for 365 km). Day 2: Jispa -> Sarchu "
									+ "over Baralacha La 4,890 m (~90 km, 4h), gravel + water "
									+ "crossings. Day 3: Sarchu -> Pang -> Leh via Gata Loops, "
									+ "Nakee La, Tanglang La 5,328 m (~250 km, 9h, long day - start "
									+ "at dawn). Acclimatise in Leh. Carry spare fuel; altitude "
									+ "sickness risk."),
					new SaveTripRequest("San Francisco", "Los Angeles",
							"Monterey, Big Sur, Morro Bay, Santa Barbara",
							LocalDate.of(2025, 9, 12), LocalDate.of(2025, 9, 15),
							"Harley-Davidson Street Glide", "intermediate", 260, 280,
							"scenic coastal cruising on Highway 1", true, false,
							"ocean cliffs, viewpoints, seaside towns", "moderate",
							"Day 1: SF -> Monterey via CA-1 & Santa Cruz (~190 km, 4h), "
									+ "17-Mile Drive. Day 2: Monterey -> Big Sur -> Morro Bay "
									+ "(Bixby Bridge, McWay Falls) (~210 km, 5h, slow twisty "
									+ "cliffs). Day 3: Morro Bay -> Santa Barbara (~210 km). "
									+ "Day 4: Santa Barbara -> LA via Malibu (~150 km)."),
					new SaveTripRequest("Bormio", "Bormio",
							"Stelvio Pass, Santa Maria, Umbrail Pass, Gavia Pass",
							LocalDate.of(2025, 8, 2), LocalDate.of(2025, 8, 3),
							"BMW R 1250 GS", "experienced", 200, 300,
							"twisty alpine mountain passes", true, false,
							"hairpin switchbacks, alpine panoramas", "premium",
							"Day 1: Bormio -> Stelvio Pass 2,758 m (48 hairpins) -> Santa "
									+ "Maria -> Umbrail Pass back to Bormio (~95 km, 4h of pure "
									+ "switchbacks). Day 2: Bormio -> Gavia Pass 2,621 m -> Ponte "
									+ "di Legno loop (~110 km, 4h, narrow with cattle on road)."),
					new SaveTripRequest("Deals Gap", "Deals Gap",
							"Tail of the Dragon US-129, Cherohala Skyway, Tellico Plains",
							LocalDate.of(2025, 6, 14), LocalDate.of(2025, 6, 15),
							"Kawasaki Ninja 650", "intermediate", 180, 200,
							"technical twisty backroads", true, true,
							"318 curves in 11 miles, forest scenery", "budget",
							"Day 1: Deals Gap -> Tail of the Dragon (US-129, 318 curves) -> "
									+ "Robbinsville (~70 km, 3h, ride it twice). Day 2: Robbinsville "
									+ "-> Cherohala Skyway -> Tellico Plains -> back via US-129 "
									+ "(~150 km, 4h)."),
					new SaveTripRequest("Naples", "Sorrento",
							"Amalfi, Positano, Ravello (SS163 Amalfitana)",
							LocalDate.of(2025, 5, 18), LocalDate.of(2025, 5, 20),
							"Ducati Scrambler", "intermediate", 120, 200,
							"narrow scenic coastal cliff roads", true, true,
							"sea-cliff villages, Mediterranean views", "moderate",
							"Day 1: Naples -> Sorrento via SS145 (~55 km, 2h, heavy traffic). "
									+ "Day 2: Sorrento -> Positano -> Amalfi -> Ravello on SS163 "
									+ "(~50 km, 4h, very narrow, busloads - ride early). "
									+ "Day 3: Amalfi coast loop back to Sorrento (~60 km)."));

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
