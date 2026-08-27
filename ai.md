# The `.ai` File Format (V3 Upgrade)

This document provides instructions for developers working with the **`.ai` file format**, a highly secure, block-based, temporally-aware local memory system for autonomous AI agents built by Sednium.

## What is the `.ai` Format?

The `.ai` format resolves the limits of standard conversational context windows by implementing encrypted, block-partitioned, and attention-decayed state retrieval. It allows agents to load large histories instantly, bypassing long input-prefill precomputation phases.

## Key Features

1.  **Partial Envelope Unpacking (Lazy Loading):** The engine reads an unencrypted, HMAC-verified structural header index first, and decrypts *only* the specific context blocks requested (metadata, variables, dialog turns).
2.  **Importance-Based Decay:** Shifts from pure time-based memory decay to utility-based context retention. High reference usage (matching active workspace keywords) preserves raw accuracy (`FP16_RAW`), while low usage triggers instant compression (`INT2_DEEP_SUMMARY`).
3.  **Neural Graph Connectivity:** Crypto hash pointers link sequential contexts and related skill blocks across sessions into a directed acyclic graph (DAG) of cognitive states.
4.  **Autonomous Self-Correction Loop:** Background processes scan archives, decay inactive blocks, update graph paths, and silently migrate legacy formats to V3.
5.  **AEAD Encryption:** Leverages an Encrypt-then-MAC (EtM) standard to ensure context files cannot be read or tampered with without correct keys.

## Integrating Skills into your Agent

To enable your autonomous agent to read/write `.ai` checkpoints, configure your skills and backends:

### 1. Copy Agent Configuration Skills
Copy the `agentic-skill` configurations from the repository into your workspace:
```bash
# Workspace-scoped rules:
mkdir -p .agents/skills/ai-format
cp agentic-skill/SKILL.md .agents/skills/ai-format/SKILL.md
cat agentic-skill/AGENTS.md >> .agents/AGENTS.md
```
For Global-scoped rules, append `agentic-skill/AGENTS.md` to `~/.gemini/config/AGENTS.md`.

### 2. Deploy Python Backend Engines
Ensure the core Python modules are in your active execution path:
*   `ai_format_production.py` (AEAD Context Engine)
*   `ai_format_temporal.py` (Ebbinghaus forgetting curves)
*   `autonomous_optimizer.py` (Self-correcting optimizer)

### 3. Usage via CLI Wrapper (`ai.bat`)
A CLI wrapper is available for manual checks. Update `SCRATCH_DIR` inside `ai.bat` to point to your agent's scratch workspace.
*   `.\ai save backup.ai`: Serialize and encrypt your active session.
*   `.\ai load backup.ai`: Authenticate and preview history.

## Development & Testing
To run the automated validation tests (verifying block encryption, lazy loading, and decay logic):
```bash
python test_v3_features.py
```
