# AI-Powered Travel Planner API

Spring Boot 3 + Spring AI service that generates trip itineraries with an OpenAI
LLM and uses **RAG over past trips** (PGVector) to ground new plans in what
worked on similar previous trips.

## Stack

- Java 25 (Gradle toolchain; uses the installed JDK 25)
- Spring Boot 3.5.x, Spring AI 1.0.x
- OpenAI chat (`gpt-4o-mini`) + embeddings (`text-embedding-3-small`)
- PostgreSQL + `pgvector` as the vector store; Spring Data JPA for the `trips` table
- springdoc-openapi (Swagger UI)

## Prerequisites

- An OpenAI API key
- Docker (for the bundled `pgvector` database). `spring-boot-docker-compose`
  starts/stops `docker-compose.yml` automatically during `bootRun`.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `OPENAI_API_KEY` | _(required)_ | OpenAI auth; app fails fast at startup if missing |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` | Chat model |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | Embedding model (1536 dims) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/travel` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `travel` / `travel` | DB credentials |
| `SEED_SAMPLE_TRIPS` | `true` | Seed ~5 sample past trips on first start |

## Build (no infra or API key needed)

```powershell
.\gradlew.bat build
```

Unit tests mock the AI/vector beans, so the build is green offline.

## Run

```powershell
$env:OPENAI_API_KEY = "sk-..."
.\gradlew.bat bootRun
```

Docker Compose brings up `pgvector`; Spring AI creates the `vector` extension
and `vector_store` table (`initialize-schema: true`). On first start the sample
trips are embedded so RAG has content.

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/trips/plan` | Generate an itinerary (RAG-grounded with similar past trips) |
| `POST` | `/api/trips` | Persist a trip + embed it into PGVector |
| `GET` | `/api/trips/{id}` | Fetch a saved trip |
| `GET` | `/api/trips/similar?query=&k=` | Semantic similarity search over past trips |

### Example

```bash
curl -s http://localhost:8080/api/trips/plan -H "Content-Type: application/json" -d '{
  "origin": "London",
  "destination": "Kyoto",
  "startDate": "2026-04-01",
  "endDate": "2026-04-05",
  "interests": "temples, food, gardens",
  "budget": "moderate"
}'
```

The response includes the generated `itinerary` and `usedContext` — the past
trips retrieved from the vector store and fed to the model.

## How RAG works here

1. `POST /api/trips` persists a `Trip` (JPA) and adds a `Document`
   (itinerary text + metadata) to PGVector; OpenAI embeddings are generated
   inside `VectorStore.add(...)`.
2. `POST /api/trips/plan` builds a query from the request, runs
   `vectorStore.similaritySearch(...)`, injects the retrieved trips into the
   prompt template, and calls the LLM via `ChatClient`.

See [src/main/java/com/example/travelplanner/service/TripPlanningService.java](src/main/java/com/example/travelplanner/service/TripPlanningService.java).
