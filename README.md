# Origami — Personal AI Assistant

A privacy-first, on-device Android AI assistant powered by **Gemma 4 E2B** running locally via LiteRT-LM. No data ever leaves your device.

## Architecture

```
┌─────────────────────────────────────────────────┐
│  Chat UI  │  Assistants  │  Skills  │  Settings │  ← Compose UI
├───────────┴──────────────┴──────────┴───────────┤
│              Agent Loop (Tool-calling)           │  ← AgentLoop.kt
├──────────┬──────────┬──────────────┬────────────┤
│ LiteRT   │  Memory  │   Terminal   │  Scheduler │  ← Core layers
│ (Gemma4) │  SQLite  │  (proot/sh)  │  WorkMgr   │
├──────────┴──────────┴──────────────┴────────────┤
│   Tools: WebSearch · Composio · Code · Calendar │  ← Agent tools
└─────────────────────────────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Inference | LiteRT-LM + MediaPipe Tasks GenAI (Gemma 4 E2B, 2.6GB) |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Database | Room (SQLite) |
| Memory | Custom embedding engine + cosine similarity retrieval |
| Networking | Retrofit + OkHttp |
| Scheduling | WorkManager + Foreground Service |
| Skills | Shell/Python scripts with SKILL.md manifest |
| Web Search | Tavily Search API |
| App integrations | Composio REST API |

## Features

- **Private inference** — Gemma 4 E2B runs entirely on-device (CPU backend for safety, optional GPU)
- **Tool-calling loop** — One-tool-at-a-time structured JSON calls with automatic result injection
- **Semantic memory** — Automatically extracts and stores facts from conversations, retrieved by cosine similarity
- **Context compression** — Summarizes old turns when the context window fills up
- **Assistants** — Create custom personas with system prompts and daily/weekly schedules
- **Skills** — Drop-in script bundles (SKILL.md + shell/Python) for extensible capabilities
- **Web search** — Tavily API for real-time information
- **250+ app integrations** — Composio for Gmail, Calendar, Slack, GitHub, Notion, etc.
- **Onboarding wizard** — Profiles + skill selection generates a personalized system prompt

## Build

```bash
./gradlew assembleDebug
```

## Model Setup

The app prompts you to download **Gemma 4 E2B** (~2.6 GB) on first launch, or you can provide a path to a pre-downloaded `.task` file.

> **CPU backend is strongly recommended** for devices with less than 12 GB RAM. GPU loading requires ~3 GB VRAM and will SIGSEGV on 8 GB devices.

## Skill Format

```
my-skill/
  SKILL.md      # name, description, version, entry_points
  run.sh        # main entry point
  *.py / *.sh   # supporting scripts
```

Install via Skills tab → folder path.

## API Keys (optional)

- **Tavily** — web search (tavily.com)
- **Composio** — 250+ app integrations (composio.dev)

Both are optional — the assistant works fully offline without them.
