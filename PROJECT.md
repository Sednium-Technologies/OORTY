# Oorty Android & AI Architecture Specification (`PROJECT.md`)

> **Product**: Oorty by Sednium  
> **Repository Scope**: Multi-Model AI Orchestrator, Local Neural Engine & Obsidian-Compatible Markdown Vault Client  
> **Platforms**: Android (Native Jetpack Compose, Min SDK 26, Target SDK 35/36) & Web Landing Engine  
> **Brand Identity**: Milk White (`#FDFBF7`), Dark Mode Charcoal (`#333333` / `#1E1E1E`), Editorial Orange Accent (`#EC5E27`), Serif Typography (`Source Serif 4`)

---

## 1. Executive Summary & Core Philosophy

**Oorty** is a high-performance, privacy-first AI chat, coding orchestration, and agentic assistant developed by Sednium. It combines the power of multi-provider cloud Large Language Models (Google Gemini, OpenAI, Anthropic Claude, xAI Grok, Groq, OpenRouter, NVIDIA NIM, and Sednium Rosette) with completely offline, on-device neural processing via native GGUF runtime (`llama.cpp`), on-device title generation (`LiteRT`), and local semantic memory recall.

### Fundamental Tenets
1. **Zero-Cloud Memory Lock-in**: All chats and long-term memory are stored directly on the user's device as human-readable, Obsidian-compatible Markdown notes with rich YAML frontmatter (`Documents/Oorty/chats/`).
2. **Autonomous Tool Augmentation (MCP)**: Implements the Model Context Protocol (MCP) enabling models (both remote flagship APIs and local quantized SLMs) to run external functions, fetch resources, inspect schemas, and query local memory.
3. **Hardware Aware & Safe**: Real-time memory profiling dynamically assesses device RAM before loading GGUF weights, providing safety badges (`Recommended`, `May Overheat`, `Not Able to Run`) to prevent OOM panics.
4. **Editorial Aesthetic Craft**: Follows a warm, high-craft editorial design language with smooth animations, customizable system instructions, and unified preset switching.

---

## 2. Complete File & Directory Structure

```
Oorty/
├── .agents/                                # Agentic configurations and shared agent rules
├── .claude/                                # Claude Code workflow configurations
├── .git/                                   # Git version control metadata
├── .gitignore                              # Git exclusion rules
├── ai.md                                   # Specification for the .ai encrypted state format
├── config.yaml                             # LiteLLM / Rosette routing & fallback config
├── Oorty.md                                # High-level web & brand product specification
├── PROJECT.md                              # Comprehensive architectural blueprint (this document)
├── README.md                               # Project documentation & user quickstart
├── rosette.md                              # Rosette gateway integration instructions
├── run-proxy.ps1                           # Local proxy script for dev routing
├── TEST_INFRA.md                           # End-to-End and unit test hierarchy specification
│
├── website/                                # Landing page & hosted web client
│   ├── index.html                          # Landing page markup (Tailwind + Vanilla JS)
│   ├── assets/                             # Brand assets, logos, and previews
│   ├── css/                                # Custom styles and animations
│   ├── js/                                 # Client interaction scripts
│   ├── public/                             # Public static web assets
│   └── oorty-app.apk                       # Published standalone release APK binary
│
└── app/                                    # Native Android Project (Gradle Root)
    ├── build.gradle.kts                    # Root build script & plugins configuration
    ├── settings.gradle.kts                 # Project dependency repositories & modules
    ├── gradle.properties                   # JVM memory flags & AndroidX enable flags
    ├── gradlew / gradlew.bat               # Gradle wrapper executable binaries
    ├── gradle/
    │   ├── wrapper/                        # Gradle wrapper jar and properties
    │   └── libs.versions.toml              # Version Catalog (AGP, Compose, Kotlin, OkHttp, etc.)
    │
    └── app/                                # Main Application Module (`oorty.sednium.app`)
        ├── build.gradle.kts                # Application dependencies, build features, ABI filters
        ├── proguard-rules.pro              # Obfuscation and optimization rules
        └── src/
            ├── main/
            │   ├── AndroidManifest.xml     # Permissions, Query intents (Termux), Activity setup
            │   ├── res/                    # Drawables, mipmaps, string resources, themes
            │   │   ├── drawable/           # Vector icons and graphical assets
            │   │   ├── mipmap-*/           # Launcher icons (ic_launcher, ic_launcher_round)
            │   │   ├── values/             # strings.xml, colors.xml, themes.xml
            │   │   └── xml/                # data_extraction_rules, backup_rules
            │   │
            │   └── java/oorty/sednium/app/ # Kotlin Source Tree
            │       │
            │       ├── MainActivity.kt     # Single-Activity entry point, state holder & agent loop
            │       ├── StorageHelper.kt    # SharedPreferences serializer & Markdown Vault dual-writer
            │       │
            │       ├── model/              # Domain Data Classes & State Enums
            │       │   └── Models.kt       # AppSettings, ChatSession, ChatMessage, MCPConfig, Providers
            │       │
            │       ├── navigation/         # App Scaffold & Navigation Composition
            │       │   └── SedniumApp.kt   # Drawer, BottomSheets, TopBar orchestration & dialog router
            │       │
            │       ├── api/                # Network APIs & Local Inference Engines
            │       │   ├── UniversalApi.kt         # Retrofit router for Gemini, Claude, OpenAI, Groq, GGUF
            │       │   ├── LlamaHelper.kt          # Native llama.cpp GGUF runner & fallback engine
            │       │   ├── LiteRtTitleGen.kt       # On-device TFLite/LiteRT chat title summarizer
            │       │   ├── HuggingFaceApi.kt       # HuggingFace Hub repository search & metadata API
            │       │   └── StreamingThoughtParser.kt # Real-time <thought> tag extractor for reasoning models
            │       │
            │       ├── vault/              # On-Device Markdown Vault & Semantic RAG
            │       │   ├── ChatVault.kt            # Obsidian file writer (`Documents/Oorty/chats/`)
            │       │   ├── ChatVaultEntry.kt       # Vault metadata, tag parser & note container
            │       │   ├── EmbeddingEngine.kt      # 384-dimensional dense semantic vector generator
            │       │   └── VaultIndexer.kt         # In-memory cosine-similarity RAG search & memory injector
            │       │
            │       ├── mcp/                # Model Context Protocol (MCP) Autonomous Tool Calling
            │       │   ├── McpProtocol.kt          # JSON-RPC 2.0 message schemas & constants
            │       │   ├── McpProtocolTypes.kt     # Tool, CallToolResult, ClientCapabilities models
            │       │   ├── McpClient.kt            # Client transport coordinator & session lifecycle
            │       │   ├── McpServerManager.kt     # Multi-server connection manager & tool registry
            │       │   ├── StreamableHttpTransport.kt # SSE + HTTP POST client for remote MCP servers
            │       │   ├── ToolCallOrchestrator.kt # Multi-turn LLM <-> MCP execution loop
            │       │   ├── ToolCallPolicy.kt       # Security rules & destructive call safeguards
            │       │   ├── ToolCallEvent.kt        # UI telemetry events (Started, Retrying, Succeeded, Failed)
            │       │   ├── ProviderToolChatClients.kt # Gemini, Claude, OpenAI tool call converters
            │       │   ├── LocalGgufToolChatClient.kt # Local GGUF tool invocation parser
            │       │   └── VaultRecallTool.kt      # Built-in tool for LLMs to query past vault notes
            │       │
            │       ├── ui/                 # Jetpack Compose UI Layer
            │       │   ├── screens/
            │       │   │   ├── ChatScreen.kt       # Primary conversation screen with stream renderer
            │       │   │   ├── ChatListScreen.kt   # Drawer list of chat sessions, search, batch delete
            │       │   │   ├── SettingsScreen.kt   # 3-tab settings (API/Models, General, Local AI/GGUF)
            │       │   │   ├── McpServersScreen.kt # MCP server configuration & tool toggle list
            │       │   │   └── PromptLabScreen.kt  # Isolated playground for testing system instructions
            │       │   │
            │       │   ├── components/
            │       │   │   ├── TopBar.kt               # App header, model badge, preset selector, gear
            │       │   │   ├── ChatBubble.kt           # User/Assistant message bubble, copy, thought toggle
            │       │   │   ├── MessageComposer.kt      # Multi-line input bar, attachment tray, send button
            │       │   │   ├── ChatListRow.kt          # Chat session row item with pin and context menu
            │       │   │   ├── HuggingFaceHubDialog.kt # Live Hugging Face GGUF repository explorer modal
            │       │   │   ├── HardwareWarningDialog.kt # Insufficient RAM safety confirmation modal
            │       │   │   ├── ModelLoadingOverlay.kt  # Full-screen GGUF model load progress overlay
            │       │   │   ├── SessionConfigDialog.kt  # Per-chat hyperparameter override dialog
            │       │   │   ├── SettingsControls.kt     # Reusable settings text fields, sliders, switches
            │       │   │   ├── AddMcpServerDialog.kt   # MCP endpoint registration dialog
            │       │   │   ├── McpDisclaimerDialog.kt  # Tool execution security warning dialog
            │       │   │   ├── ImageViewerOverlay.kt   # Full-screen lightbox for attached images
            │       │   │   └── BufferedFadingMarkdown.kt # Smooth animated markdown chunk renderer
            │       │   │
            │       │   └── theme/
            │       │       ├── Color.kt            # Palette definitions (SedYellow, Orange, Dark Charcoal)
            │       │       ├── Theme.kt            # Compose MaterialTheme provider with Dark Mode support
            │       │       ├── Type.kt             # Source Serif 4 & Roboto typography scale
            │       │       ├── Shape.kt            # Rounded corner standard tokens (sm: 8dp, md: 14dp, lg: 20dp)
            │       │       └── Animations.kt       # Spring specs and transition animation curves
            │       │
            │       ├── code/               # Code Syntax Highlighting & Copy UI
            │       │   ├── CodeBlockView.kt        # Card container with copy button & language header
            │       │   └── SyntaxHighlighter.kt    # Regex-based syntax colorizer (Kotlin, Python, JS, etc.)
            │       │
            │       ├── markdown/           # Custom In-App Markdown AST & Parser
            │       │   ├── Block.kt                # Heading, Paragraph, List, BlockQuote, Code AST nodes
            │       │   ├── MarkdownView.kt         # Compose renderer for parsed Markdown blocks
            │       │   ├── parseMarkdown.kt        # Block-level parser
            │       │   └── parseInline.kt          # Inline bold, italic, strikethrough, link, code parser
            │       │
            │       └── util/               # System & Hardware Utilities
            │           └── HardwareChecker.kt      # RAM inspection, GGUF parameter sizing, compatibility checks
            │
            ├── test/                               # JVM Local Unit & Robolectric Tests
            │   └── java/oorty/sednium/app/         # Unit test suite (API routing, memory limits, embeddings)
            │
            └── androidTest/                        # Instrumented Device & UI Tests
                └── java/oorty/sednium/app/         # Compose UI test cases & activity integration tests
```

---

## 3. Core Subsystems & How Oorty Works

### A. UI & Theming System
- **Single Activity Shell**: `MainActivity.kt` hosts `SedniumApp.kt` inside a Compose `Surface`.
- **Theme Palette**:
  - **Light Mode (`SedYellow`)**: Background `#FDFBF7` (Milk White), Surfaces `#FFFFFF`, Accents `#EC5E27` (Burnt Orange).
  - **Dark Mode (`DarkGray`)**: Background `#333333`, Card Surfaces `#1E1E1E` / `#262626`, Borders `#444444`, Accents `#FF7A45`.
- **Dynamic Font Switching**: User toggle between Serif (`Source Serif 4`) and Clean Sans (`Roboto`).

### B. Dual-Tier State & Persistence Architecture
1. **In-Memory Reactive State**: Compose `mutableStateOf` holds active chat messages, streaming tokens, active preset, and transient tool executions.
2. **Local SharedPreferences**: `StorageHelper.kt` encodes `AppSettings` and `List<ChatSession>` to JSON via `kotlinx.serialization`.
3. **Obsidian-Native Markdown Vault (`ChatVault.kt`)**:
   - Whenever chats are saved, the most recent 15 active sessions are dual-written to `Documents/Oorty/chats/{chat_id}.md`.
   - Notes include YAML frontmatter:
     ```yaml
     ---
     title: "Fixing Vector Dimensions"
     date: 2026-08-28T20:27:00
     tags: [kotlin, compose, vectors]
     model: "gemini-3.5-flash"
     tokens_est: 340
     ---
     ```

### C. Hybrid Semantic Memory Retrieval (RAG)
- `EmbeddingEngine.kt`: Computes 384-dimensional dense semantic vectors using subword n-grams, word positional weighting, and $L_2$ unit normalization.
- `VaultIndexer.kt`:
  - Scans all vault files on startup into memory.
  - On user query: Computes cosine similarity against past chat embeddings + title keyword boosts.
  - Formats top matching snippets and automatically prepends context into the model's system prompt:
    ```
    [Relevant memory from your past chats on this device:]
    • Chat: "Fixing Vector Dimensions" (tags: kotlin, compose)
      Snippet: We updated VECTOR_DIM from 128 to 384...
    [Use the above context if relevant to the query, or proceed naturally.]
    ```

### D. Model Context Protocol (MCP) Autonomous Agent Loop
- `McpServerManager`: Connects via Server-Sent Events (`StreamableHttpTransport`) to external tools (e.g. filesystem, web scrapers, database connectors).
- `ToolCallOrchestrator`:
  1. Sends user message and tool definitions to active provider (Gemini, Claude, OpenAI, or Local GGUF).
  2. Parses function calls from response payload.
  3. Executes tool calls via MCP servers.
  4. Emits real-time state events (`Started`, `Succeeded`, `Failed`) to the UI `ToolActivityView`.
  5. Recursively feeds tool execution outputs back to the LLM until the final natural language answer is synthesized.

### E. Inference & Provider Execution Subsystem
1. **Cloud Providers (`UniversalApi.kt`)**: Direct streaming endpoints using Retrofit & OkHttp SSE for Google Gemini, OpenAI, Anthropic, xAI, Groq, OpenRouter, NVIDIA NIM, and Sednium Rosette.
2. **On-Device GGUF Inference (`LlamaHelper.kt`)**:
   - Uses `llamacpp-kotlin` native bindings to load quantized weights directly into mobile RAM.
   - Streams incremental tokens directly to Compose UI with live TTFT (Time-to-First-Token) and tokens/sec telemetry.
3. **On-Device Title Generation (`LiteRtTitleGen.kt`)**:
   - Uses TensorFlow Lite / LiteRT model (`title_generator.tflite`) with TF-IDF keyword extraction fallback to generate titles for new conversations with 0 latency.

---

## 4. Hardware Safety & RAM Recommendation Matrix

`HardwareChecker.kt` calculates available RAM via `ActivityManager.MemoryInfo` and evaluates GGUF model suitability:

| Estimated Model Size | Min RAM Required | Recommended RAM | Fit Classification | UI Warning Action |
|---|---|---|---|---|
| **0.5B - 1.0B (0.3 - 0.7 GB)** | ≥ 3.0 GB | 4.0 GB | `PERFECT_FIT` | Direct Launch |
| **1.5B - 3.0B (1.0 - 2.2 GB)** | ≥ 5.5 GB | 8.0 GB | `MODERATE_LOAD` | Recommended Badge |
| **7.0B - 8.0B (3.8 - 4.5 GB)** | ≥ 9.0 GB | 12.0 GB | `HEAVY_LOAD` | `HardwareWarningDialog` (May Overheat) |
| **14B+ (8.0+ GB)** | ≥ 16.0 GB | 16.0 GB+ | `UNSUPPORTED` | Prevent Execution / Warning |

---

## 5. End-to-End Test Matrix & Verification

As detailed in `TEST_INFRA.md`:
- **Tier 1 (Feature Isolation)**: Tests App Icon, Termux setup guide, GGUF downloader, dark mode colors, native GGUF stream, title generator, and preset management.
- **Tier 2 (Boundaries & Edge Cases)**: OOM limits, malformed GGUF headers, network disconnections during SSE streaming, special character sanitization in Markdown notes.
- **Tier 3 (Cross-Feature Integrations)**: Dual-write to vault while downloading GGUF model in Dark Mode.
- **Tier 4 (Real-World Scenarios)**: Complete offline RAG session with tool execution and Markdown export.
