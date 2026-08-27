# Project: Oorty Android Application Enhancements

## Architecture
Oorty is a multi-model orchestration client. The Android application is built with Jetpack Compose, targeting Android SDK 35 (min SDK 26).
Key architectural areas involved:
- **UI Screens (`ui/screens/`)**: Includes `SettingsScreen`, `ChatScreen`, and `ChatListScreen` implementing Jetpack Compose screens.
- **API Services (`api/`)**: `UniversalApi` handles model request generation and streaming. `LiteRtTitleGen` performs local title generation.
- **Model / Storage (`model/`, `StorageHelper.kt`)**: Defines data classes for presets, models, and settings state.

## Code Layout
- `app/app/src/main/AndroidManifest.xml`: Application manifest defining app launcher icon.
- `app/app/src/main/java/oorty/sednium/app/ui/theme/Theme.kt`: Color themes and Compose Theme setup.
- `app/app/src/main/java/oorty/sednium/app/ui/screens/`: Screen composables.
- `app/app/src/main/java/oorty/sednium/app/ui/components/`: Component composables (e.g. `ChatBubble.kt`).
- `app/app/src/main/java/oorty/sednium/app/api/`: API implementations (`UniversalApi.kt`, `LiteRtTitleGen.kt`).
- `app/app/build.gradle.kts`: Android build file defining dependencies and compilation settings.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|---|---|---|---|
| 1 | E2E Testing Track | Implement E2E test cases (Tier 1-4) | None | IN_PROGRESS |
| 2 | Milestone 1: App Identity & UI Polish | R1 (App icon, ChatBubble avatar), R2 (Termux setup guide), R7.2 (Rounded dialogs & theme colors) | None | PLANNED |
| 3 | Milestone 2: Dark Mode & Theming | R4 (Dark Mode Color(0xFF333333), replace hardcoded Milk backgrounds) | M1 | PLANNED |
| 4 | Milestone 3: Title Generator & Packaging | R6 (noCompress tflite, LiteRtTitleGen robust asset reader fallback) | M1 | PLANNED |
| 5 | Milestone 4: Models Downloader & Presets | R3 (popular GGUF list, RAM check, Recommended badge, save to presets), R7.1 (savedPresets edit/delete) | M2, M3 | PLANNED |
| 6 | Milestone 5: Native GGUF Inference | R5 (llamacpp-kotlin dependency, takePersistableUriPermission, LlamaHelper inference stream, endpoint fix) | M4 | PLANNED |
| 7 | Milestone 6: Final Verification & Hardening | Run all E2E tests, execute adversarial coverage testing (Tier 5) | M5 | PLANNED |

## Interface Contracts
### Presets Storage & Structure
Saved presets (`settings.savedPresets`) must use a unified configuration data class:
- Data type: `ModelConfig` / `SavedPreset`
- Properties:
  - `name`: String (identifying preset label)
  - `systemInstruction`: String (custom system prompt)
  - `model`: String (model identifier/name or local URI/path)
  - `provider`: String ("LOCAL_GGUF", "LOCAL", "CUSTOM", etc.)
  - `mode`: String (Quick, Thinking, Coding)

### Local Endpoint Routing
When API client makes calls:
- If provider is `LOCAL`, `LOCAL_GGUF`, or `CUSTOM`, it must use `settings.localBaseUrl` instead of hardcoded default endpoints.

### GGUF Model Execution
`UniversalApi.generateContentStream` GGUF branch details:
- Runs natively on-device.
- If provider is `LOCAL_GGUF`, instantiate llama.cpp engine via `LlamaHelper` using the persistable Uri retrieved.
- Stream chunks of responses incrementally back to `onChunkReceived` handler.
