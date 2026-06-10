# ManU4U Intelligence — LLM-First Football Intelligence Agent

## Overview

**ManU4U Intelligence** is an LLM-first, tool-using AI agent focused exclusively on **Manchester United**.  
The system answers natural-language questions about matches, fixtures, goals, assists, lineups, and (later) news and history by **grounding all factual answers in real data**.

The project is intentionally designed as:
- **Conversational** (ChatGPT-like UX)
- **Grounded** (no hallucinated match facts)
- **Composable** (outputs can later power content creation: tweets, videos, threads)
- **Agent-driven** (LLM decides what tools to use, not hardcoded rules)

This project forms **Phase 1 (Intelligence Layer)** of a larger ManU4U ecosystem.

---

## Core Philosophy

1. **LLM-first agent**
    - The LLM decides *intent*, *tool selection*, and *reasoning*
    - No regex-based or rules-first routing in backend code

2. **Tools provide truth**
    - All match facts come from API-Football
    - Backend code executes tools safely and deterministically

3. **Separation of concerns**
    - Project 1: Intelligence & Q&A (this repo)
    - Project 2 (future): Content generation (tweets, videos, scripts)

4. **Production-minded**
    - Structured outputs
    - JSON schema enforcement
    - Observability and safety

---

## What This Project Does (Phase 1)

The system can answer questions like:
- “Who assisted United’s first goal today?”
- “What was the score in the last match?”
- “Who started at right-back?”
- “When do we play next?”

Later phases will support:
- injuries
- news summaries
- historical questions
- tactical explanations

---

## High-Level Architecture

User
↓
POST /ask
↓
Agent Orchestrator (Spring Boot)
↓
LLM (Tool-Calling Enabled)
↔
Tool Registry
↔
Tool Executor
↔
API-Football / Time / (Later: News, Vector DB)
↓
Structured Response (JSON)

yaml
Copy code

---

## Tech Stack

### Backend
- Java 17
- Spring Boot 3
- WebClient (HTTP)
- Jackson (JSON)
- Bean Validation

### AI
- LLM with tool-calling support (OpenAI / equivalent)
- No LangChain required
- ReAct-style agent loop

### Data
- API-Football (live match data)
- Later: Postgres + pgvector (memory & RAG)

---

## Phase 1 Tools (MVP)

Only **high-signal, fact-based tools** are included initially.

### 1. TimeTool (local)
Resolves relative time expressions.

**Responsibilities**
- Resolve “today”, “yesterday”
- Apply timezone: `America/Vancouver`

---

### 2. FixturesTool
Backbone of all match resolution.

**API-Football Endpoint**
GET /fixtures

yaml
Copy code

**Responsibilities**
- Find matches for a team on a given date
- Resolve “today’s match”, “last match”, “next match”
- Provide fixtureId for downstream tools

---

### 3. EventsTool
Provides match events.

**API-Football Endpoint**
GET /fixtures/events

yaml
Copy code

**Responsibilities**
- Goals (scorer, assist, minute)
- Cards
- Substitutions

All events are normalized into internal domain models.

---

### 4. LineupsTool
Provides starting XI and formations.

**API-Football Endpoint**
GET /fixtures/lineups

yaml
Copy code

**Responsibilities**
- Starting XI
- Bench
- Formation

---

## What Is Explicitly NOT in Phase 1

Deferred intentionally:
- Standings
- Player season stats
- Injuries
- Advanced team statistics
- Betting / predictions

These will be added incrementally.

---

## Agent Design (LLM-First)

### Core Loop (ReAct Style)

User Question
↓
LLM receives:

system prompt

tool catalog

conversation history
↓
LLM chooses:

Tool Call (name + args)
OR

Final Answer
↓
If tool call:

Backend executes tool

Tool result sent back to LLM

Loop continues
If final answer:

Validate + return

yaml
Copy code

---

## Role of Spring Boot

Spring Boot acts as the **agent runtime**, not the brain.

It is responsible for:
- Hosting `/ask`
- Providing tool implementations
- Executing tool calls safely
- Validating inputs/outputs
- Enforcing grounding rules
- Logging and observability

**Spring Boot does NOT**
- interpret intent via regex
- decide which tool to call
- generate football knowledge

---

## Grounding & Safety (Step 3)

### Grounding Rules
- Match facts **must** come from tools
- LLM is forbidden from guessing
- If no data is found, the agent must say so explicitly

### Output Schema (Strict)
Every response must conform to:


{
  "answer": "string",
  "evidence": { },
  "sources": [
    { "tool": "string", "key": "string" }
  ],
  "confidence": 0.0
}
Responses failing schema validation are rejected and retried.

Memory (Future Phase)
Conversation Memory
Store recent Q&A per session

Knowledge Memory (RAG)
Vectorize:

news articles

club history

Allow agent to cite retrieved context

Build Plan (Execution Roadmap)
✅ Step 1 — Tool Wrappers (Completed / In Progress)
API-Football client

TimeTool

FixturesTool

EventsTool

LineupsTool

/tools/* test endpoints

🔜 Step 2 — Agent Loop
LLM client

Tool registry + schemas

Tool executor

Agent orchestrator

/ask endpoint

🔜 Step 3 — Grounding & Schema Enforcement
System prompt rules

JSON schema validation

Retry on ungrounded answers

Confidence scoring

🔜 Step 4 — Memory & Expansion
Add news tool

Add vector DB

Historical knowledge ingestion

Mini Launch Milestones
Mini Launch 1
/ask answers match fact questions correctly

Mini Launch 2
Grounded, structured responses

Safe to share publicly

Mini Launch 3
News + memory support

Long-Term Vision (Phase 2)
Outputs from ManU4U Intelligence will power:

Tweet generation

Threads

Matchday live content

Short video scripts

Tactical explainers

This keeps intelligence separate from creativity.

Why This Project Matters (Resume Value)
Real agent architecture (not demos)

Tool-calling LLMs

Grounded AI (anti-hallucination)

Production-grade backend

Clear evolution path

Status
Current focus: Step 2 — Agent loop implementation
Next milestone: First /ask mini launch

yaml
Copy code

---

If you want, next we can:
- **trim this into a public GitHub README**
- or split it into `README.md` + `ARCHITECTURE.md` + `ROADMAP.md`

Just tell me 👍
::contentReference[oaicite:0]{index=0}