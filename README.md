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
- A **Google Maps API key** with **Directions API** and **Geocoding API**
  enabled on a GCP project that has billing turned on (the $200/month free
  credit covers ~40k requests — plenty for personal use). Used to fetch the
  real route (real roads, real distances, real town names) that the LLM
  then narrates around. Without it, `POST /api/trips/plan` returns a 503
  with a clear message rather than a hallucinated route.
- Docker (for the bundled `pgvector` database). `spring-boot-docker-compose`
  starts/stops `docker-compose.yml` automatically during `bootRun`.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `OPENAI_API_KEY` | _(required)_ | OpenAI auth; app fails fast at startup if missing |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` | Chat model |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | Embedding model (1536 dims) |
| `GOOGLE_MAPS_API_KEY` | _(required for /plan)_ | Google Maps key (Directions + Geocoding APIs enabled) |
| `GOOGLE_MAPS_REGION` | `in` | ccTLD bias for routing/geocoding (e.g. `in` for India) |
| `GOOGLE_MAPS_COUNTRY` | `in` | ISO-3166 alpha-2 (lowercase) to constrain geocoding to a country |
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
$env:OPENAI_API_KEY      = "sk-..."
$env:GOOGLE_MAPS_API_KEY = "AIza..."
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

## Deployment (Render + Supabase, free tier)

Single-deploy setup: the [Dockerfile](Dockerfile) builds the React app, bakes
the static bundle into the Spring Boot fat jar, and serves both API and UI
from one URL. React Router deep links work via
[SpaFallbackController](src/main/java/com/example/travelplanner/controller/SpaFallbackController.java).

### One-time setup

1. **Supabase** — create a free project at <https://supabase.com>. In the SQL
   editor, run `CREATE EXTENSION IF NOT EXISTS vector;`. From *Project
   Settings → Database*, copy the **Session Pooler** JDBC string
   (port 5432, not the 6543 transaction pooler — Spring AI's `pgvector` store
   needs a full Postgres session).
2. **Render** — at <https://render.com>, create a Web Service from the
   GitHub repo. Runtime: **Docker**. Plan: **Free**. Set environment
   variables:
   - `OPENAI_API_KEY`
   - `GOOGLE_MAPS_API_KEY`
   - `DB_URL` (the Supabase JDBC URL, e.g. `jdbc:postgresql://...sslmode=require`)
   - `DB_USERNAME`, `DB_PASSWORD`
   - `SPRING_DOCKER_COMPOSE_ENABLED=false` (Postgres is external on Render)
3. Click *Create Web Service*. First build is ~10 min (downloads JDK + Gradle
   + npm deps); later builds reuse cached layers. The deploy URL is
   `https://<service-name>.onrender.com`.

### Free-tier caveats

- The service **spins down after ~15 min of inactivity**; first request after
  that takes ~30 s to wake up. Subsequent requests are fast.
- Supabase free pauses after a week of no traffic; resumes automatically on
  the next connection.
- Render free gives 512 MB RAM; the Dockerfile sets `-XX:MaxRAMPercentage=75`
  so the JVM stays inside that limit.

### Local Docker smoke test (before pushing)

```powershell
docker build -t travel-planner .
docker run --rm -p 8080:8080 `
  -e OPENAI_API_KEY=sk-... `
  -e GOOGLE_MAPS_API_KEY=AIza... `
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/travel `
  -e DB_USERNAME=travel -e DB_PASSWORD=travel `
  -e SPRING_DOCKER_COMPOSE_ENABLED=false `
  travel-planner
```
Open <http://localhost:8080> → both UI and `/api/*` come from one origin.

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

## How a plan request flows

1. **Routing (ground truth)** — [RoutingService](src/main/java/com/example/travelplanner/service/routing/RoutingService.java)
   geocodes `origin`/`destination`/waypoints via Google Maps Geocoding,
   calls Google Maps Directions (`mode=driving`, `region=in`, `avoid=…`),
   then chunks the real polyline into daily legs of at most
   `maxDailyDistanceKm`, reverse-geocoding each day boundary to a real
   town name. Result: `RouteSummary { totalDistanceKm, totalDuration, days[] }`.
2. **RAG** — [TripPlanningService](src/main/java/com/example/travelplanner/service/TripPlanningService.java)
   builds a semantic query from the ride parameters and runs
   `vectorStore.similaritySearch(...)` against PGVector for similar past rides.
3. **LLM narrative** — the `RouteSummary` is rendered to Markdown and injected
   into the user prompt as the **source of truth**; the system prompt forbids
   changing distances, durations or town names. The model only writes the
   day-by-day narrative (fuel stops, viewpoints, road notes, rider tips).
4. **Save (`POST /api/trips`)** persists the trip and adds a `Document` to
   PGVector so future plans can retrieve it as context.

If `GOOGLE_MAPS_API_KEY` is unset or Google is unreachable, `/plan` returns
a clear 503 rather than letting the LLM invent a route.
