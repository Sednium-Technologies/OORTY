# Oorty UI/UX Revamp & Functional Fix Specification (`UI_REVAMP.md`)

> Companion to `PROJECT.md`. This turns the open punch-list into a structured, buildable spec — organized by subsystem, mapped to the actual files in the repo, with clear acceptance criteria per item. Section 10 adds ideas beyond the original list; Section 11 is a flat checklist for tracking.

---

## 1. Reasoning vs. Response Separation (Thinking Mode)

**Bug**: the "thought process" bubble currently renders both the model's chain-of-thought reasoning *and* its final answer inside the same collapsible block. The user only ever sees the collapsed/expandable reasoning bubble — the actual answer is buried inside it instead of appearing as a normal message.

**Required behavior**:
- `StreamingThoughtParser.kt` already extracts `<thought>` tags — confirm it splits output into two distinct fields, e.g. `ChatMessage.reasoning: String?` and `ChatMessage.content: String`, with zero overlap between them.
- `ChatBubble.kt` renders `reasoning` only inside the collapsible "Show thinking" toggle (collapsed by default).
- Only `content` (the final answer) renders as the primary, always-visible chat message.

**Files**: `StreamingThoughtParser.kt`, `ChatBubble.kt`, `Models.kt`

---

## 2. Message Composer Redesign

**Shape**: `MessageComposer.kt` container gets a true pill radius — add a `pill` token to `theme/Shape.kt` (fully rounded ends), rather than reusing the existing `lg: 20dp` card radius.

**Icon order (left → right)**:
1. **Plus icon** — replaces the paperclip. Opens the attachment/tool tray (image, file, camera, etc.). Clean, centered, single-weight stroke.
2. Text input field — expands vertically as needed.
3. **Mic icon** — new outline SVG. Tap starts speech-to-text; transcribed words stream live into the text field as the user speaks, instead of opening a separate screen.
4. **Send button** — up-arrow icon, replacing the paper-plane. Enabled only when there's text or an attachment.

**Live Mode**: today the send button silently swaps into a mic icon while recording, with no clear meaning. Replace that swapped state with an explicit **Live Mode** icon (a waveform/orb glyph, visually distinct from the composer mic) that launches a dedicated hands-free voice conversation: continuous listen → STT → LLM → TTS playback loop, with a visible listening/thinking/speaking state indicator.

**Files**: `MessageComposer.kt`, new `LiveModeScreen.kt` (or `LiveModeOverlay.kt`)

---

## 3. Chat Bubble Layout

- **User messages**: keep the bubble container, but fix the corner radius so all **four corners are equally rounded** — no asymmetric "tail" corner.
- **Assistant messages**: **remove the bubble/card background entirely.** Response text renders directly on the screen background, left-aligned, full width — matching the "no bubble for AI" pattern used by most modern chat UIs.

**Files**: `ChatBubble.kt`, `theme/Shape.kt`

---

## 4. Icon System Modernization

Replace the current mixed icon set with one consistent modern outline family (uniform stroke weight, no mixed filled/outline styles). Minimum replacement list:

| Old | New |
|---|---|
| Paperclip (attach) | Plus, rounded/centered |
| Paper-plane (send) | Up-arrow |
| *(missing)* | Mic, outline waveform |
| Old "recording" mic swap | Live Mode / waveform-orb glyph |
| Mixed default Material icons in Settings/MCP screens | Same outline family throughout |

Apply consistently across `TopBar.kt`, `SettingsScreen.kt`, `McpServersScreen.kt`, `ChatListRow.kt` context menus, and all dialogs.

---

## 5. Post-Response Action Toolbar

Add a small icon row under every **assistant** message:

- **Copy** — copies the full rendered response (plain text/Markdown).
- **Share** — system share sheet with the response text.
- **⋯ (more)** — opens a bottom sheet with:
  - **Branch chat** — forks a new `ChatSession` from this point, copying history up to and including this message.
  - **Send to another model** — re-sends the same user prompt to a different provider/model as a new response, for side-by-side comparison.
  - **Read aloud (TTS)** — plays the response via device/cloud TTS.
  - **Retry** — regenerates in place (same model, same prompt).

**User messages**: long-press opens a small popup with **Edit** and **Copy**. Edit re-opens the message for editing and, on save, truncates/replaces downstream messages (standard "edit and regenerate" behavior).

**Files**: new `MessageActionBar.kt`, new `MessageActionSheet.kt`, `ChatBubble.kt`, `ChatScreen.kt`, `Models.kt` (parent-message linkage for branching)

---

## 6. Settings UI Consistency

- Every settings selector (model dropdown, provider dropdown, etc.) uses the same pill shape token as the composer — one shared token, not per-screen ad hoc radii.
- **GGUF model file row**: the box showing the selected filename is a different height/alignment than the "Select" button beside it. Fix: shared height, filename box uses `Modifier.weight(1f)` with `TextOverflow.Ellipsis`, button stays fixed-width.
- **Session Config Dialog**: same height/shape mismatch between the input field(s) and the **Save** button — apply the same fixed shared-height fix.
- **HuggingFace Hub dialog** (`HuggingFaceHubDialog.kt`): remove the filled background chips behind file-type/quantization icons — they currently overlap adjacent badges. Use outline-only or icon-only chips with `Arrangement.spacedBy` instead.

**Files**: `SettingsControls.kt`, `SettingsScreen.kt`, `SessionConfigDialog.kt`, `HuggingFaceHubDialog.kt`, `theme/Shape.kt`

---

## 7. Functional Fixes (Not Cosmetic)

- **GGUF loading must be real, not a stub.** Audit `LlamaHelper.kt` end-to-end: confirm it actually calls the native `llama.cpp` bindings, loads real weights from the selected `.gguf` file, and streams genuine tokens. Add a visible smoke test (log first-token latency and tokens/sec) so it's provable, not assumed.
- **API key → model fetch is broken.** In `SettingsScreen.kt`, whatever calls the provider's "list models" endpoint after a key is entered needs to actually populate the dropdown — check auth header formatting, error handling, and that the response is parsed into backing state.
- **Model search doesn't work.** The filter field over the fetched model list needs an actual predicate wired to the text input — likely just a missing `.filter {}` on the list state, or an `onValueChange` that isn't connected to anything.

**Files**: `LlamaHelper.kt`, `SettingsScreen.kt`, `UniversalApi.kt`

---

## 8. System Prompt Overhaul

- Remove any "explain your reasoning" / "show your thinking" instruction from the Thinking Mode system prompt. Per Section 1, reasoning is captured via `<thought>` tags and hidden by default — instructing the model to narrate reasoning *to the user* directly conflicts with that and is likely part of why reasoning and answer are currently bleeding together.
- Audit every built-in system prompt (Thinking Mode, default assistant prompt, `PromptLabScreen.kt` defaults, MCP tool-use prompt). Replace vague boilerplate ("be helpful and thoughtful...") with concrete, scoped instructions: the assistant's role, the exact tags/format it must emit (`<thought>`, tool-call JSON, etc.), and explicit dos/don'ts.

**Files**: wherever prompt templates live (likely defaults in `Models.kt` or a `prompts/` resource), `PromptLabScreen.kt`

---

## 9. Multimodal Output Support (New)

`ChatMessage` currently assumes text-only. Extend it to carry typed generative outputs:

- **Image generation**: shimmer/skeleton placeholder while generating, then swaps to the final image (tap opens `ImageViewerOverlay.kt`).
- **Voice/audio generation** (e.g. Lyria-class models): inline audio player — waveform scrubber, play/pause, duration.
- **Video generation**: inline video player with a poster-frame placeholder while generating.

Give each a generation-state enum (`Queued`, `Generating`, `Complete`, `Failed`), mirroring the state pattern `ToolCallEvent` already uses for MCP calls, so placeholders and error states stay consistent app-wide.

**Files**: `Models.kt` (ChatMessage variants), new `ImageResultView.kt`, `AudioResultView.kt`, `VideoResultView.kt`, `UniversalApi.kt` (routing to image/audio/video-capable providers)

---

## 10. Additional Ideas Worth Considering

Beyond the original list:

- **Streaming skeleton** — shimmer placeholder for the assistant bubble before the first token arrives, using the TTFT telemetry `LlamaHelper.kt` already tracks.
- **Inline tool-call chip** — mirror the reasoning-bubble pattern for MCP calls: a small collapsible "Used tool: web_search" chip inline in the response, instead of only a toast.
- **In-chat search** — a find-in-conversation bar on `ChatScreen.kt`, separate from vault-wide semantic search.
- **Model capability badges** — small tags (Vision / Tools / 128k) next to each model in the selector, since providers vary widely.
- **Regenerate with different settings** — from Retry, a small popover to nudge temperature/top-p before regenerating, instead of only an exact repeat.
- **Response rating** — a lightweight thumbs up/down per response, useful as a future signal for the vault's memory ranking.
- **Offline/fallback badge** — small indicator when the app silently falls back to the local GGUF model due to no network, so a quality/personality shift isn't confusing.
- **Per-chat export** — share sheet option to export a single conversation as Markdown or PDF, separate from a full vault export.
- **Accent color options** — a couple of alternate accents beyond burnt-orange, for users who want to visually separate work/personal sessions.
- **Accessibility pass** — content-description labels on every new icon button (mic, live mode, plus, action-bar icons), plus a font-scale setting independent of system text size, since Serif/Sans switching already exists.

---

## 11. Consolidated Checklist

| # | Item | Type | Primary file(s) |
|---|---|---|---|
| 1 | Split reasoning vs. response | Bug | StreamingThoughtParser.kt, ChatBubble.kt |
| 2 | Pill-shaped composer + icon rework | UI | MessageComposer.kt |
| 3 | Mic button + live transcription | Feature | MessageComposer.kt |
| 4 | Live Mode voice conversation | Feature | LiveModeScreen.kt (new) |
| 5 | User bubble: 4 rounded corners | UI | ChatBubble.kt |
| 6 | AI response: no bubble background | UI | ChatBubble.kt |
| 7 | Icon set modernization | UI | app-wide |
| 8 | Post-response action toolbar | Feature | MessageActionBar.kt (new) |
| 9 | Branch / resend-to-model / TTS / retry | Feature | ChatScreen.kt, Models.kt |
| 10 | Long-press user message → edit/copy | Feature | ChatBubble.kt |
| 11 | Pill-shaped settings selectors | UI | SettingsControls.kt |
| 12 | GGUF filename box vs. Select button sizing | Bug | SettingsScreen.kt |
| 13 | Session Config input vs. Save button sizing | Bug | SessionConfigDialog.kt |
| 14 | HuggingFace icon background overlap | Bug | HuggingFaceHubDialog.kt |
| 15 | Verify GGUF loading is real, not stubbed | Bug/Verify | LlamaHelper.kt |
| 16 | Fix API key → model fetch | Bug | SettingsScreen.kt |
| 17 | Fix model search filter | Bug | SettingsScreen.kt |
| 18 | Remove "show reasoning" from Thinking prompt | Prompt | prompt templates |
| 19 | Rewrite vague system prompts | Prompt | prompt templates |
| 20 | Multimodal output rendering (image/audio/video) | Feature | Models.kt + new result views |
