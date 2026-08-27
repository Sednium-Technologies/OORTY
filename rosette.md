# Integrating with Rosette Gateway

This guide explains how to connect external applications (like Sednium Oorty, autonomous coding agents, or custom LLM clients) to the **Rosette Gateway**. By configuring your app to interface with Rosette, you gain access to decentralized credentials management, automatic provider failover, true round-robin key rotation, CVE payload shielding, and multi-agent collaborative orchestration.

## 1. Architectural Overview

Rosette operates as a **zero-server key storage** system. The gateway endpoints are stateless, meaning they do not persist your API keys in a centralized database.

### Authentication Methods
1. **Stateless Cloud Sync (Recommended):** Best for user-facing apps. Pass a Google OAuth Token in the `Authorization: Bearer` header. Keys are retrieved client-side from the user's private Google Drive `appDataFolder`.
2. **Scoped HTTP Headers:** Best for CLI/Scripts/CI. Pass `x-rocky-keys` and `x-rocky-waterfall` in HTTP Headers.
3. **Direct Configuration:** Best for backend integrations. Pass configuration directly in the JSON body: `{"config": {"providerKeys": [...], ...}}`.

## 2. API Endpoints

Both endpoints accept standard JSON payloads and return OpenAI-compatible JSON responses wrapped with additional Rosette metadata.

*   **Unified Chat Completions:** `POST /api/chat`
    *   *Features:* Fallback cascading, key rotation, CVE security check, outbound domain whitelisting.
*   **Multi-Agent Orchestration:** `POST /api/orchestrate`
    *   *Features:* Automatically elects a Leader model based on performance benchmarks, coordinates sub-agents to solve complex tasks, and returns a synthesized result.

## 3. Step-by-Step Integration

### Step 1: Generate a Scoped Key
1. Open the Rosette Dashboard -> **Key Vault**.
2. Under **Scoped API Keys**, toggle RBAC permissions (e.g., `read_files`, `write_files`, `execute_commands`, `network_access`).
3. Click **Generate Scoped Key** and copy it.

### Step 2: Configure Client Code

#### Node.js / TypeScript (OpenAI SDK)

```typescript
import OpenAI from 'openai';

const providerKeys = [
  {
    provider: 'openai',
    key: 'sk-proj-YourOpenAiKeyHere...',
    name: 'Primary OpenAI',
    model: 'gpt-4o',
    endpoint: 'https://api.openai.com/v1'
  },
  // Add Anthropic, Deepseek, etc. here
];
const waterfallOrder = ['openai', 'anthropic'];

const openai = new OpenAI({
  apiKey: 'sk-sednium-dummy-key', // Dummy key to pass validation
  baseURL: 'https://rosette.sednium.com/api', // Point to Rosette gateway
  defaultHeaders: {
    'x-rocky-keys': JSON.stringify(providerKeys),
    'x-rocky-waterfall': JSON.stringify(waterfallOrder)
  }
});

async function main() {
  const completion = await openai.chat.completions.create({
    model: 'gpt-4o', // The model specified here will be intercepted and failover-routed
    messages: [{ role: 'user', content: 'Ping' }]
  });
  console.log('Response:', completion.choices[0].message.content);
}
```

#### Python (Direct REST API Calls)

```python
import requests
import json

url = "https://rosette.sednium.com/api/chat"
payload = {
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Ping"}],
    "config": {
        "providerKeys": [
            { "provider": "openai", "key": "sk-proj-YourKey", "model": "gpt-4o" }
        ],
        "waterfallOrder": ["openai"],
        "cveThreshold": 85  # Block messages scoring below 85% safety score
    }
}
headers = {"Content-Type": "application/json"}
response = requests.post(url, headers=headers, data=json.dumps(payload))
```

## 4. Security & Guardrails

When your app connects to Rosette, it is protected by:
*   **Vulnerability CVE Shield:** Scans prompt payloads for high-risk strings (like SQL injections, XSS, or `rm -rf`). Fails with `400 Bad Request`.
*   **Outbound Sandbox Whitelisting:** Prevents agents from executing callbacks to non-whitelisted third-party APIs.
*   **Cascading Failover & Rotation Logs:** Automatically falls back on rate limit (HTTP 429) or internal errors (HTTP 500). Logs are appended in `_rocky_logs` metadata.
