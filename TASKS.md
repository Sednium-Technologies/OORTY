# Oorty Comprehensive On-Device Testing Checklist

Master verification checklist for testing all features, UI revamp items, Live Voice pipelines, Prompt Lab, and system automations on Android device.

---

## 🎙️ Category 1: Universal Live Voice Mode (<300ms TTFW & Personas)
- [x] **1. Empty-State Live Button**: Open a chat. When the input composer is empty, verify the rightmost button displays the orange circular Live Flow button. *(Verified on device)*
- [x] **2. Hands-Free Voice Activation**: Tap the orange live button to launch Live Call Mode. Verify the glowing orb starts listening immediately without requiring repeated taps. *(Verified on device)*
- [ ] **3. Sentence-Streaming Zero-Latency (<300ms TTFW)**: Ask a spoken question (e.g. *"Why is the sky blue?"*). Verify Oorty begins speaking its first sentence almost instantly (~300ms) while still generating the remainder.
- [ ] **4. Human Conversational Persona**: Verify responses in Live Mode are concise (1–3 natural spoken sentences) with no markdown bullet points, tables, or step-by-step reasoning steps.
- [ ] **5. Smart Barge-In (<50ms Interruption)**: While Oorty is speaking aloud, speak a new question (e.g. *"Wait, what about clouds?"*). Verify Oorty cuts off playback immediately and captures your new question.
- [x] **6. Continuous Hot-Mic Listening**: After Oorty finishes speaking its answer, verify the orb immediately transitions back to listening without tapping. *(Verified on device)*
- [x] **7. Live Voice Persona Selector**: Go to **Settings → Live Voice Persona**. Switch between:
  - 🌸 *Warm & Friendly* (Aoede/Serena)
  - 🎙️ *Deep & Confident* (Charon/Onyx)
  - ⚡ *Energetic & Direct* (Puck/Echo)
  - 📱 *System Default*
  Verify each chip plays an instant audio preview when selected. *(Verified on device)*

---

## 🤖 Category 2: Gemini Live & Realtime Voices
- [x] **8. Gemini Live Voice Selector**: In Settings under **Google Gemini**, verify the **Gemini Live Voice** chips (**`Aoede`**, **`Puck`**, **`Charon`**, **`Kore`**, **`Fenrir`**) are displayed and selectable. *(Verified on device)*
- [x] **9. Gemini Live Models Catalog**: In Settings, verify `gemini-2.0-flash-exp`, `gemini-2.0-flash-realtime`, `gemini-3.1-flash-live`, and `gemini-transcribe-3` are available in the model list. *(Verified on device)*
- [x] **10. Phonetic Speech Normalization**: Ask the AI to say *"I bought $45.20 worth of items with 85% discount for our API endpoint"*. Verify TTS pronounces *"forty five dollars and twenty cents"*, *"eighty five percent"*, and *"A P I"*, not raw symbols. *(Verified on device)*

---

## 🧪 Category 3: Prompt Lab Workspace (Drawer → Prompt Lab)
- [x] **11. Open Prompt Lab**: Open the left drawer menu from the chat screen and tap **Prompt Lab**. *(Verified on device)*
- [x] **12. Summarize Tool**: Select **Summarize**, paste a long article/notes, and tap **Run**. Verify real-time streaming output produces a clean, bulleted summary. *(Verified on device)*
- [x] **13. Rewrite Tool with Tones**: Select **Rewrite**, select a tone chip (**`Default`**, **`Professional`**, **`Casual`**, **`Concise`**, **`Persuasive`**), enter text, and tap **Run**. Verify the output adopts the requested tone. *(Verified on device)*
- [x] **14. Code Gen Tool**: Select **Code Gen**, request a code snippet, and tap **Run**. Verify it produces syntax-highlighted code with copy buttons. *(Verified on device)*
- [x] **15. Continue in Chat Handoff**: Once output is generated in Prompt Lab, tap **"Continue in Chat"**. Verify it creates a new chat session with your prompt & output pre-populated. *(Verified on device)*

---

## ⚡ Category 4: Post-Chat Action Bar & 3-Dot Action Sheet
- [x] **16. Quick Action Bar Buttons**: Under any assistant message turn, verify the action bar has working **Speaker (TTS read-aloud)**, **Copy**, and **Retry (Regenerate)** buttons. *(Verified on device)*
- [x] **17. Branch Chat (Fork Conversation)**: Tap the 3-dots (**`...`**) on any past message turn and tap **"Branch chat"**. Verify Oorty forks a new conversation from that exact message turn. *(Verified on device)*
- [x] **18. Cross-Model Comparison ("Send to another model")**: Tap the 3-dots (**`...`**) → tap **"Send to another model"** → pick a different provider/model. Verify the turn is re-evaluated by the alternative model. *(Verified on device)*
- [x] **19. Android System Share**: Tap the 3-dots (**`...`**) → tap **Share**. Verify Android's native share sheet opens to send the response to external apps. *(Verified on device)*

---

## 📲 Category 5: Device Automations & Android Bridge
- [x] **20. Open WhatsApp**: Send *"open whatsapp"* or say it in Live Mode. Verify WhatsApp launches directly on your device. *(Verified on device)*
- [ ] **21. Open Termux**: Send *"open termux"*. Verify Termux opens without error.
- [x] **22. Open Chrome / Browser**: Send *"open chrome"* or *"open browser"*. Verify Google Chrome opens. *(Verified on device)*
- [x] **23. Battery Status Inspector**: Send *"check my battery status"*. Verify Oorty reports battery percentage, charging state, and health. *(Verified on device)*
- [x] **24. Flashlight Toggle**: Send *"turn on flashlight"* then *"turn off flashlight"*. Verify the camera flash toggles on and off. *(Verified on device)*

---

## 💬 Category 6: Chat Window, Layout & UI Craft
- [x] **25. Left-Aligned Assistant Responses**: Send a prompt that generates code and markdown. Verify the response starts directly at the **left edge of the screen** (no 60dp left avatar indent) so code blocks have full width. *(Verified on device)*
- [x] **26. User Message Right-Alignment**: Verify all user message bubbles are pinned cleanly to the right side with 4 equally rounded corners. *(Verified on device)*
- [x] **27. Long-Press User Message Popup**: Long-press any sent user message bubble. Verify the milk/dark popup menu appears with subtle shadow and offers **Edit** and **Copy** options. *(Verified on device)*
- [x] **28. Compact Tablet Input Bar**: Verify the text composer input bar has pill/tablet rounded corners and proper padding. *(Verified on device)*
- [x] **29. Compact Reasoning / Thought Process Bar**: For reasoning models (e.g. DeepSeek-R1 / Gemini Thinking), verify the thought bar is styled as a compact tablet pill that toggles open and closed. *(Verified on device)*
- [x] **30. Drawer Header Oorty Logo**: Open the navigation drawer from the top-left menu. Verify the header shows the official **Oorty App Logo** instead of the generic robot icon. *(Verified on device)*

---

## 📦 Category 7: On-Device Plugins & Local Models
- [x] **31. Qwen 3 0.6B Ultralight Plugin Card**: Open the Plugin Screen or Settings → On-Device Micro-Models. Verify **Qwen 3 0.6B Ultralight (380 MB)** appears with its HuggingFace repo and description. *(Verified on device)*
- [x] **32. Qwen 3 0.6B in Local GGUF**: Select **Model Provider → Local GGUF**. Verify `Qwen 3 0.6B (0.4 GB) — Low Hardware` is listed at the top. *(Verified on device)*
- [x] **33. Kokoro-82M Neural Voice Plugin Card**: In Settings, verify **Kokoro-82M Neural Voice (82 MB)** is listed under plugins. *(Verified on device)*
- [ ] **34. Silent Background OCR on Images**: Attach an image or screenshot containing text. Verify Oorty silently extracts text from the image and answers questions about it.

---

## 📚 Category 8: Universal Markdown Vault & RAG
- [ ] **35. Universal Markdown Indexing**: Create or place any `.md` file inside `Documents/Oorty/` (with or without frontmatter). Ask a question about its contents. Verify Oorty recalls the information via Vault RAG across all models.

---

## 🏢 Category 9: Sednium Creators & Real-Time Data Intelligence
- [x] **36. Sednium & Creators System Knowledge**: Ask Oorty *"Who made you?"*, *"Who is Sednium?"*, *"Who is Bhoid / Ayush Pal?"*, or *"Who is Loid / Ankush Das?"*. Verify Oorty provides accurate knowledge about Sednium Technologies, Ayush Pal, and Ankush Das.
- [x] **37. About Us Section in Settings**: Go to **Settings** and scroll to the bottom. Verify the **About Sednium & Creators** card displays with clickable portfolio links to `sednium.com`, `bhoid.sednium.com`, and `loid.sednium.com`.
- [x] **38. Real-Time Device Clock & Live Time**: Ask Oorty in chat or Live Mode *"What time is it right now?"* or *"What is today's date?"*. Verify Oorty answers with the exact current time and date.
- [x] **39. Web Search & Link Action Integration**: Ask Oorty *"Search for latest AI news"* or emit `[ACTION: SEARCH query="..."]`. Verify it opens Google Search in the browser.

---

### Quick Installation Command:
```bash
/home/bhoid/Android/Sdk/platform-tools/adb install -r -t /run/media/bhoid/StorageVault/WEbs/Oorty/app/app/build/outputs/apk/debug/app-debug.apk
```
