# AI-Powered Motorcycle Road-Trip Planner API

Spring Boot 3 + Spring AI service that generates **day-by-day motorcycle ride
itineraries** with an OpenAI LLM — route legs with distances and ride time, fuel
stops spaced to the bike's range, passes/twisties, road-surface and weather
cautions, and rider gear/checklist tips. It uses **RAG over past rides**
(PGVector) to ground new plans in what worked on similar previous motorcycle
trips.

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

## Frontend (React + Vite)

A web UI lives in [frontend/](frontend/) (React 19 + Vite + Tailwind). It talks
to the backend through a Vite dev proxy (`/api` → `:8080`), so no CORS config is
needed and you only open one URL.

```powershell
# backend must be running on :8080 first
cd frontend
npm install   # first time only
npm run dev
```

Open <http://localhost:5173>. Pages: **Plan** (generate a ride plan + see RAG
context, save it), **Trips** (saved rides list + detail), **Save** (manual),
**Similar** (semantic search). Production build: `npm run build` (outputs
`frontend/dist/`).

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/trips/plan` | Generate an itinerary (RAG-grounded with similar past trips) |
| `POST` | `/api/trips` | Persist a trip + embed it into PGVector |
| `GET` | `/api/trips` | List all saved trips (newest first) |
| `GET` | `/api/trips/{id}` | Fetch a saved trip |
| `GET` | `/api/trips/similar?query=&k=` | Semantic similarity search over past trips |

### Request fields

`origin` (ride start) and `destination` are required; everything else is
optional and falls back to sensible defaults.

| Field | Type | Notes |
|---|---|---|
| `origin` / `destination` | string | **required** — ride start / end (loop allowed) |
| `waypoints` | string | optional "via" stops |
| `startDate` / `endDate` | date | `YYYY-MM-DD` |
| `motorcycleModel` | string | e.g. `Royal Enfield Himalayan 450` |
| `ridingExperience` | string | `beginner` / `intermediate` / `experienced` |
| `maxDailyDistanceKm` | int | daily riding cap (paces the legs) |
| `fuelRangeKm` | int | tank/charge range → fuel-stop spacing |
| `routePreference` | string | e.g. `twisty mountain passes`, `scenic coastal` |
| `avoidHighways` / `avoidTolls` | bool | prefer backroads / skip tolls |
| `interests` | string | scenery & points of interest |
| `budget` | string | accommodation/fuel budget level |
| `notes` | string | free-form (two-up, panniers, cold mornings…) |

### Example

```bash
curl -s http://localhost:8080/api/trips/plan -H "Content-Type: application/json" -d '{
  "origin": "Manali",
  "destination": "Leh",
  "waypoints": "Jispa, Sarchu, Pang",
  "startDate": "2026-07-05",
  "endDate": "2026-07-08",
  "motorcycleModel": "Royal Enfield Himalayan 450",
  "ridingExperience": "experienced",
  "maxDailyDistanceKm": 200,
  "fuelRangeKm": 250,
  "routePreference": "high-altitude Himalayan passes",
  "avoidHighways": false
}'
```

The response includes the generated `itinerary` and `usedContext` — the past
rides retrieved from the vector store and fed to the model.

### Re-seeding the sample rides

The seeder loads 5 classic motorcycle routes **only when the `trips` table is
empty**. If you previously ran the app it still holds old data, so reset the DB
volume for a clean reseed:

```powershell
docker compose down -v   # drops the pgdata volume (trips + vector_store)
.\gradlew.bat bootRun    # seeder repopulates with motorcycle routes
```

## How RAG works here

1. `POST /api/trips` persists a `Trip` (JPA) and adds a `Document`
   (itinerary text + metadata) to PGVector; OpenAI embeddings are generated
   inside `VectorStore.add(...)`.
2. `POST /api/trips/plan` builds a query from the request, runs
   `vectorStore.similaritySearch(...)`, injects the retrieved trips into the
   prompt template, and calls the LLM via `ChatClient`.

See [src/main/java/com/example/travelplanner/service/TripPlanningService.java](src/main/java/com/example/travelplanner/service/TripPlanningService.java).
