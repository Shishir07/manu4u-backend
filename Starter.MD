Create Tool Wrappers (v1) (already done)
1.1 Create a new Spring Boot project (modules you need)

Spring Web

Validation

Jackson

Lombok (optional but speeds you up)

Spring Boot Actuator (optional but nice)

1.2 Add config for API-Football + defaults

Create application.yml:

API-Football base URL

API key

default teamId for Manchester United

timezone = America/Vancouver

Example:

manu4u:
  timezone: America/Vancouver
  defaultTeamId: 33

apifootball:
  baseUrl: https://v3.football.api-sports.io
  apiKey: ${API_FOOTBALL_KEY}

1.3 Create a tiny HTTP client wrapper (one place to call API-Football)

Use Spring’s WebClient (recommended) or RestTemplate (fine too).

ApiFootballClient

GET /fixtures

GET /fixtures/events

GET /fixtures/lineups

This client should:

attach x-apisports-key header

handle non-200 responses cleanly

return raw JSON mapped to DTOs

1.4 Define your normalized domain models (important)

Do NOT leak API-Football JSON everywhere. Normalize immediately.

Create models like:

FixtureSummary { id, utcKickoff, status, homeTeam, awayTeam }

MatchEvent { minute, type, teamId, playerName, assistName, detail }

Lineup { teamId, formation, starters[], substitutes[] }

1.5 Implement the Tools as Java services
TimeTool (local, no external calls)

resolveDate(expression, timezone) supports:

today, yesterday

(later: last match, next match)
Return:

ResolvedDate { date: YYYY-MM-DD, timezone }

FixturesTool

Methods:

getFixturesByDate(teamId, date) → list of FixtureSummary

(later) getFixturesByRange(teamId, from, to)

selectFixture(fixtures, preference) (keep minimal: choose LIVE else nearest kickoff)

EventsTool

getFixtureEvents(fixtureId) → list of MatchEvent (normalized)

LineupsTool

getFixtureLineups(fixtureId) → list of Lineup (normalized)

1.6 Add one “Tool Test Endpoint” (to prove tools work)

Before the agent, add simple endpoints:

GET /tools/time?expr=today

GET /tools/fixtures/today

GET /tools/events/{fixtureId}

GET /tools/lineups/{fixtureId}

This makes debugging insanely easy.

1.7 Add basic unit tests (minimal)

TimeTool resolves today/yesterday in America/Vancouver

FixturesTool selection logic for LIVE vs NOT_STARTED

What “done” looks like for Step 1

You can run locally and do:

GET /tools/time?expr=today → returns date

GET /tools/fixtures/today → returns United fixture(s) for today

Use a fixtureId from (2) and call:

/tools/events/{fixtureId}

/tools/lineups/{fixtureId}

Once this works, Step 2 (agent loop) becomes straightforward.

Your first concrete action (right now)

Create these 4 files first:

ApiFootballProperties.java (baseUrl, apiKey)

Manu4uProperties.java (timezone, defaultTeamId)

ApiFootballClient.java (WebClient wrapper)

TimeTool.java (resolveDate)