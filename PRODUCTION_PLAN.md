# ManU4U — Production & Hosting Plan

> Living document. Update as decisions are made. Sections marked `[DECIDE]` need a final call before that phase begins.

---

## 1. Application Components to Host

| Component | Current (dev) | Production target |
|---|---|---|
| Spring Boot app | `localhost:8080` | Container (Docker) |
| PostgreSQL | `docker-compose` local | Managed DB service |
| Qdrant | `docker-compose` local | Qdrant Cloud or managed container |
| Prometheus | `docker-compose` local | Grafana Cloud (managed) or self-hosted |
| Grafana | `docker-compose` local | Grafana Cloud or self-hosted |
| Tempo (traces) | `docker-compose` local | Grafana Cloud Traces or self-hosted |

---

## 2. Hosting Options

### Option A — PaaS (Recommended for MVP / low ops overhead)

| Service | What it hosts | Notes |
|---|---|---|
| [Railway](https://railway.app) | App + Postgres + Qdrant | One-click deploys, managed Postgres, env vars UI, free tier available |
| [Render](https://render.com) | App + Postgres | Similar to Railway; Postgres add-on; no native Qdrant |
| [Fly.io](https://fly.io) | App container | More control, persistent volumes for Qdrant, cheap |
| [Qdrant Cloud](https://cloud.qdrant.io) | Qdrant only | Free 1GB cluster; connect from any host |
| [Grafana Cloud](https://grafana.com/products/cloud/) | Prometheus + Tempo + Grafana | Free tier: 10k metrics, 50GB traces/logs |

**Best combo for MVP:** Railway (app + Postgres) + Qdrant Cloud (free cluster) + Grafana Cloud (observability)

### Option B — Cloud Container (More control)

| Service | Notes |
|---|---|
| AWS ECS Fargate | Serverless containers; RDS Postgres; no cold starts |
| Google Cloud Run | Per-request billing; Cloud SQL for Postgres; cheapest for low traffic |
| Azure Container Apps | If you're already Azure-aligned |

**Best combo:** Cloud Run (app) + Cloud SQL Postgres + Qdrant Cloud + Grafana Cloud

### Option C — Kubernetes (Future / high scale)
Not recommended until the app proves it needs it. Adds significant ops overhead for a fan-facing tool.

---

## 3. Pre-Production Checklist

### 3.1 Database

- [ ] Switch to Flyway for schema migrations (replace `ddl-auto: update` — already `validate` in staging/prod profiles)
- [ ] Create `src/main/resources/db/migration/V1__initial_schema.sql` from current entity definitions
- [ ] Add `flyway-core` dependency to `pom.xml`
- [ ] Test migration on clean DB before deploying
- [ ] Managed DB: enable automated backups (daily minimum), point-in-time recovery

**Tables to include in V1 migration:**
`countries`, `leagues`, `seasons`, `teams`, `players`, `player_statistics`, `fixture_cache`

### 3.2 Secrets / Environment Variables

Never commit API keys. All sensitive config must come from environment variables:

| Env var | Maps to | Where set |
|---|---|---|
| `SPRING_AI_OPENAI_API_KEY` | OpenAI key | Railway/Cloud secret |
| `API_FOOTBALL_KEY` | API-Football key | Railway/Cloud secret |
| `SPRING_DATASOURCE_URL` | DB connection string | Managed DB provides this |
| `SPRING_DATASOURCE_USERNAME` | DB user | Railway/Cloud secret |
| `SPRING_DATASOURCE_PASSWORD` | DB password | Railway/Cloud secret |
| `QDRANT_HOST` | Qdrant cluster host | Qdrant Cloud provides this |
| `QDRANT_API_KEY` | Qdrant API key | Qdrant Cloud provides this |

- [ ] Rotate all keys before first public deployment
- [ ] Never log env vars (check `application.yml` for any `${...}` values that get logged at startup)
- [ ] Confirm `application-prod.yml` has no hardcoded fallbacks for secrets

### 3.3 Admin Endpoint Security

Currently `/admin/*` and `/admin/sync/*` are completely open. **Must be secured before going public.**

Options (pick one):
- **Simple:** HTTP Basic Auth via Spring Security — one admin user, credentials in env vars
- **Better:** Bearer token (`Authorization: Bearer <token>`) checked by a request filter
- **Best:** IP allowlist + Basic Auth (only callable from your IP or a trusted CI job)

- [ ] `[DECIDE]` Security model for admin endpoints
- [ ] Add Spring Security dependency + config
- [ ] Protect: `POST /admin/**`, `POST /admin/sync/**`
- [ ] Leave public: `GET /health`, `POST /ask` (if public-facing)

### 3.4 Rate Limiting on `/ask`

Each `/ask` call hits OpenAI (token cost) and potentially API-Football (credit cost). Without rate limiting, a crawler can drain both quotas.

- [ ] `[DECIDE]` Rate limit strategy:
  - **Token bucket per IP** — Resilience4j `RateLimiter` (already on classpath via Spring Cloud Circuit Breaker, or add standalone)
  - **Per session/user** — if auth is added later
- [ ] Suggested limit: 10 requests / minute / IP for MVP
- [ ] Return `429 Too Many Requests` with `Retry-After` header

### 3.5 Health Checks

Required by all container orchestrators for liveness/readiness probes.

- [ ] Expose `/actuator/health` (already available once actuator is added for observability)
- [ ] Add custom health indicator for API-Football reachability (optional)
- [ ] Liveness probe: `GET /actuator/health/liveness`
- [ ] Readiness probe: `GET /actuator/health/readiness` (waits for DB + Qdrant connections)

```yaml
# application-prod.yml addition
management:
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

### 3.6 Container Image

- [ ] Write `Dockerfile` — multi-stage build (Maven builder → JRE runtime)
- [ ] Use `eclipse-temurin:21-jre-alpine` base (small, LTS)
- [ ] Add `.dockerignore` (exclude `target/`, `.env`, IDE files)
- [ ] Set `JAVA_OPTS` for memory: `-XX:MaxRAMPercentage=75.0` (lets JVM tune to container limits)
- [ ] Tag strategy: `manu4u:<git-sha>` for traceability

```dockerfile
# Suggested Dockerfile skeleton
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 3.7 Logging

- [ ] Structured JSON logging for production (Logstash encoder or Spring Boot 3 JSON format)
- [ ] Ship logs to: Grafana Loki (if Grafana Cloud) or cloud provider's log service
- [ ] Never log: API keys, full request bodies (by default), player PII
- [ ] Add `application-prod.yml` entry:

```yaml
logging:
  structured:
    format:
      console: ecs    # Spring Boot 3.4+ native ECS JSON logging
```

---

## 4. Observability Stack (Grafana + Prometheus + Tempo)

### 4.1 Self-Hosted (docker-compose → server)

Keep the same `docker-compose.yml` stack, deploy to a small VPS or cloud VM alongside the app.

```
App container → Prometheus (scrapes /actuator/prometheus every 15s)
App container → Tempo (receives OTel traces via OTLP gRPC :4317)
Prometheus → Grafana (data source)
Tempo → Grafana (data source)
```

Config files needed:
- `observability/prometheus.yml` — scrape config
- `observability/tempo.yml` — storage + receiver config
- `observability/grafana/provisioning/datasources/` — auto-provision Prometheus + Tempo
- `observability/grafana/provisioning/dashboards/` — auto-provision ManU4U dashboard

### 4.2 Grafana Cloud (Recommended for MVP)

- Push metrics via Grafana Agent (or OTel Collector) → Grafana Cloud Prometheus endpoint
- Push traces via OTLP → Grafana Cloud Tempo endpoint
- Free tier: 10,000 active series, 50GB traces/logs/month
- Zero ops: no Prometheus/Tempo/Grafana servers to maintain

```yaml
# application-prod.yml
management:
  otlp:
    metrics:
      export:
        url: https://otlp-gateway-prod-us-east-0.grafana.net/otlp/v1/metrics
        headers:
          Authorization: "Basic <grafana-cloud-token>"
    tracing:
      endpoint: https://otlp-gateway-prod-us-east-0.grafana.net/otlp/v1/traces
```

- [ ] `[DECIDE]` Self-hosted vs Grafana Cloud for observability
- [ ] Create Grafana Cloud account + get OTLP push URL + token
- [ ] Set up ManU4U dashboard (token usage, tool calls, latency, cache hit rate)

### 4.3 Key Dashboards to Build

1. **Agent Health** — request rate, error rate, p95 latency on `/ask`
2. **Token Burn** — input + output tokens/hour → daily OpenAI cost estimate
3. **Tool Usage** — which tools fire per hour, tool call latency p95
4. **API-Football Quota** — requests/day by endpoint vs plan limit
5. **Cache Efficiency** — fixture cache hit% by status (completed/live/upcoming)
6. **VectorStore** — Qdrant query latency, top_k distribution (post-RAG)

---

## 5. CI/CD

- [ ] `[DECIDE]` CI platform: GitHub Actions (recommended — free for public repos, cheap for private)
- [ ] Pipeline stages:
  1. `mvn test` — unit + integration tests (exclude `eval` tag)
  2. `mvn package -DskipTests` — build JAR
  3. `docker build + push` — push to container registry (GHCR or Docker Hub)
  4. Deploy — Railway/Fly/Cloud Run re-deploys on new image tag

```yaml
# .github/workflows/deploy.yml sketch
on:
  push:
    branches: [main]
jobs:
  build-and-deploy:
    steps:
      - mvn test -DexcludedGroups=eval
      - docker build -t ghcr.io/<user>/manu4u:${{ github.sha }} .
      - docker push ...
      - railway up   # or: gcloud run deploy ...
```

- [ ] Store all secrets in GitHub Actions secrets (never in workflow YAML)
- [ ] Eval tests (`-Peval`) run separately against staging, not on every push

---

## 6. Scaling Considerations

ManU4U is **I/O bound** (OpenAI API + API-Football API + Postgres). A single instance handles significant traffic:
- OpenAI: ~2-5s per `/ask` → ~12 concurrent requests/min on one instance
- API-Football: free tier = 100 req/day → the fixture cache is critical before scaling

| Traffic level | Recommendation |
|---|---|
| Personal / demo (< 100 req/day) | 1 instance, 512MB RAM |
| Small public (100-1000 req/day) | 1-2 instances, 1GB RAM, add rate limiting |
| Production (> 1000 req/day) | Upgrade API-Football plan, horizontal scale, Redis for session store |

**Session state note:** `AgentOrchestrator` currently holds session memory in-memory (Map). This means **horizontal scaling breaks sessions** — two requests for the same session can land on different instances. Before scaling to >1 instance:
- [ ] Move session store to Redis or PostgreSQL (see `Future.MD`)

---

## 7. Cost Estimate (MVP / Personal scale)

| Service | Tier | Est. monthly cost |
|---|---|---|
| Railway (app) | Starter | ~$5-10 |
| Railway Postgres | Starter | ~$5 |
| Qdrant Cloud | Free (1GB) | $0 |
| Grafana Cloud | Free tier | $0 |
| OpenAI (gpt-4o) | Pay-per-token | ~$5-20 (depends on usage) |
| API-Football | Free (100 req/day) | $0 |
| **Total** | | **~$15-35/month** |

Upgrade triggers:
- API-Football → paid plan when fixture cache misses exceed free quota
- OpenAI → monitor token burn dashboard; switch to `gpt-4o-mini` for simple queries
- Railway → bump plan when RAM > 512MB or CPU throttles

---

## 8. Pre-Launch Checklist Summary

```
Infrastructure
  [ ] Dockerfile + .dockerignore
  [ ] docker-compose.prod.yml (or cloud-specific deploy config)
  [ ] Flyway V1 migration from current entity state

Security
  [ ] Admin endpoint auth (Spring Security)
  [ ] Rate limiting on /ask
  [ ] All secrets in env vars — no hardcoded fallbacks in prod profile
  [ ] Rotate API keys before first deploy

Database
  [ ] Managed Postgres provisioned
  [ ] Flyway migration runs clean on empty DB
  [ ] Automated backups enabled

Observability
  [ ] Actuator + Prometheus + Tempo wired
  [ ] Grafana dashboards provisioned
  [ ] Alert: error rate > 5% on /ask
  [ ] Alert: token usage > $X/day

Operations
  [ ] Health probes configured (liveness + readiness)
  [ ] Structured JSON logging
  [ ] Session store externalised (before multi-instance)
  [ ] Eval suite runs clean against staging
```
