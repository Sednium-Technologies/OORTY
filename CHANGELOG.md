# Changelog

All notable changes to the **Oorty** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2026-09-04

### 🚀 Added
- **Dual-Engine On-Device Inference**:
  - Direct native bindings to `org.nehuatl.llamacpp.LlamaHelper` for `.gguf` weights, streaming tokens directly via Kotlin Coroutines.
  - Native Google AI Edge LiteRT (`com.google.ai.edge.litert:litert:2.1.0`) runtime engine (`LiteRtHelper.kt`) supporting `.tflite` and `.litertlm` models with multi-threading and XNNPACK delegate acceleration.
  - Provider selection for `LOCAL_LITERT` alongside `LOCAL_GGUF`, requiring zero external API keys.
- **Hugging Face Hub Format Sorter**:
  - Model filter chips: **All Models**, **GGUF (llama.cpp)**, and **LiteRT (Google AI Edge)**.
  - Format badges on model cards with curated LiteRT models (Gemma 2 2B, MobileBERT, GOT-OCR).
- **Hands-Free Live Voice Mode**:
  - Dedicated full-screen voice conversation overlay (`LiveModeOverlay` / `VoiceConversationOverlay`) with real-time waveform visualization.
  - Integrated speech-to-text dictation directly in the message composer.
- **Thinking Mode**:
  - Dedicated `StreamingThoughtParser` that cleanly separates `<thought>` chains from final responses.
  - Collapsible Thought Process view with duration and token counters.
- **Post-Response Action Toolbar**:
  - Quick action bar under assistant responses: Copy, Share, Branch Chat, Send to Another Model, Read Aloud (TTS), and Regenerate.
  - Long-press user message editing with automatic downstream regeneration.
- **Agentic MCP Device Tools & Plugins**:
  - `DeviceTools.kt`: System tools for mobile automation (application launcher, system status, notifications).
  - Plugin architecture with onboarding flow (`PluginOnboardingScreen.kt`).
- **Rich Media Renderers**:
  - Native Jetpack Compose components for AI-generated images (`ImageResultView`), audio (`AudioResultView`), and video (`VideoResultView`).
- **Automated Zero-Mock Test Suite**:
  - `LocalEngineZeroMockTests.kt`: Asserts absence of mock strings and validates initial engine lifecycles.

### 🔄 Changed
- **Iconography**: Migrated to a cohesive, uniform stroke-weight Lucide outline icon family (`LucideIcons.kt`) across TopBar, Settings, Dialogs, and Chat.
- **Message Composer**: Rebuilt with a true pill-radius container, attachment tray trigger (`+`), live STT mic, and send up-arrow.
- **Assistant Message Styling**: Removed card container boxes for assistant messages—text now renders directly on the warm editorial background.
- **Settings Screen**: Cleaned layout with unified pill selectors and real-time hardware memory profiling.
- **Version Bump**: Bumped Android `versionCode` to `2` and `versionName` to `"2.0.0"`.

### 🛡️ Fixed
- **GGUF Mock Fallback**: Removed legacy 70+ line hardcoded canned mock responses caused by outdated reflection classpaths; native llama.cpp bindings now execute directly.
- **ModelLoadingOverlay Lifecycle**: Decoupled overlay dismiss from raw loading state, ensuring the 1.5-second success checkmark delay renders smoothly before unmounting.
- **RAM Watchdog Fallbacks**: Added safe fallback in `HardwareChecker` when `MemoryInfo` returns 0 in sandboxed environments.
- **Test Compilations**: Resolved Kotlin compiler deprecations and test framework compatibility issues.

---

## [1.0.0] - 2026-08-31

### 🚀 Initial Release
- Multi-provider cloud LLM orchestration (Google Gemini, OpenAI, Anthropic Claude, Groq, OpenRouter, NVIDIA NIM, xAI Grok).
- Local GGUF inference prototype using llama.cpp.
- Obsidian-compatible Markdown vault dual-write to `Documents/Oorty/chats/` with YAML frontmatter.
- Hybrid 384-dimensional semantic memory recall via cosine similarity.
- Model Context Protocol (MCP) initial tool-calling orchestrator.
- Warm editorial typography (Source Serif 4, Milk White `#FDFBF7`, Sednium Orange `#EC5E27`).
