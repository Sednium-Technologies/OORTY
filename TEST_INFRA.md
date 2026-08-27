# E2E Test Infra: Oorty Android Enhancements

## Test Philosophy
- Opaque-box, requirement-driven. Tests verify external observable behavior (e.g. settings parameters, layouts, intent targets, file loading, build setup).
- Methodology: Category-Partition + Boundary Value Analysis (BVA) + Pairwise Combination + Workload Testing.

## Feature Inventory
| # | Feature | Source (requirement) | Tier 1 | Tier 2 | Tier 3 |
|---|---|---|:---:|:---:|:---:|
| 1 | App Icon & Bot Avatar | R1.1, R1.2 | 5 | 5 | ✓ |
| 2 | Termux Setup Guide | R2.1, R2.2, R2.3 | 5 | 5 | ✓ |
| 3 | Local GGUF Model Downloader & RAM Check | R3.1, R3.2, R3.3, R3.4 | 5 | 5 | ✓ |
| 4 | App-Wide Dark Mode | R4.1, R4.2, R4.3 | 5 | 5 | ✓ |
| 5 | Native On-Device GGUF Inference | R5.1, R5.2, R5.3, R5.4 | 5 | 5 | ✓ |
| 6 | LiteRt Title Generator & Packaging | R6.1, R6.2 | 5 | 5 | ✓ |
| 7 | Saved Presets Management & Dialog Roundedness | R7.1, R7.2 | 5 | 5 | ✓ |

## Test Architecture
- **Test Runner**: Instrumented JUnit tests running in target application runtime via `./gradlew connectedAndroidTest` or JVM-based Roborazzi screenshot/Compose tests via `./gradlew test`.
- **Test Case Format**: Compose UI test assertions (e.g., node matching, clipboard content checking, theme attribute verification, resource presence validation).
- **Directory Layout**:
  - `app/src/androidTest/java/oorty/sednium/app/e2e/`: End-to-end tests checking user-facing UI flows.
  - `app/src/test/java/oorty/sednium/app/unit/`: Local unit tests verifying API routing, MemoryInfo calculations, and fallback loaders.

## Real-World Application Scenarios (Tier 4)
| # | Scenario | Features Exercised | Complexity |
|---|---|---|---|
| 1 | Complete offline chat session setup and execute GGUF model | F3, F5, F7 | High |
| 2 | Toggle dark mode, download a recommended model, write message and verify title generation | F3, F4, F6 | High |
| 3 | Termux integration guide check, copy command block, launch app with external intent | F2, F7 | Medium |
| 4 | Preset configuration editing: add, modify parameters, verify list updates and UI dialog rounded corners | F7 | Medium |
| 5 | Resource asset compression verification (manifest parsing, tflite packaging, drawable lookup) | F1, F6 | Medium |

## Coverage Thresholds
- **Tier 1 (Feature Coverage)**: ≥5 test cases per feature (Total: 35) happy-path checks in isolation.
- **Tier 2 (Boundary & Corner Cases)**: ≥5 test cases per feature (Total: 35) testing limits (e.g. zero RAM, no Termux, missing GGUF files, invalid model URIs, extremely long model names).
- **Tier 3 (Cross-Feature Combinations)**: ≥7 test cases testing interactions between features (e.g. downloading while in dark mode, editing preset while chatting).
- **Tier 4 (Real-World Scenarios)**: 5 application-level scenarios mimicking user flows.
