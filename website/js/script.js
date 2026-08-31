// ==========================================================================
// Sednium Oorty — Comprehensive Interactive Controller & Event Engine
// ==========================================================================

(function () {
    'use strict';

    // Utility Helpers
    const $ = (sel, ctx = document) => ctx.querySelector(sel);
    const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

    // SVG Icon Map for Toasts and Interactive Feedback
    const SVG_ICONS = {
        check: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>',
        copy: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>',
        moon: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>',
        sun: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/></svg>',
        bolt: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>',
        document: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>',
        clock: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
        warning: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
        wrench: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>',
        download: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>'
    };

    // Toast Notification Dispatcher
    function showToast(message, iconKey = 'check') {
        const container = $('#toast-container');
        if (!container) return;
        
        const toast = document.createElement('div');
        toast.className = 'toast';
        const iconSvg = SVG_ICONS[iconKey] || SVG_ICONS.check;
        toast.innerHTML = `${iconSvg} <span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3200);
    }

    // Dynamic Year
    const yearEl = $('#currentYear');
    if (yearEl) yearEl.textContent = new Date().getFullYear();

    // =========================================================================
    // 1. Theme Management
    // =========================================================================
    function toggleTheme() {
        const root = document.documentElement;
        const current = root.getAttribute('data-theme') || 'dark';
        const next = current === 'dark' ? 'light' : 'dark';
        root.setAttribute('data-theme', next);
        localStorage.setItem('oorty-theme', next);
        showToast(`Switched to ${next.toUpperCase()} theme`, next === 'dark' ? 'moon' : 'sun');
    }

    function initTheme() {
        const savedTheme = localStorage.getItem('oorty-theme') || 'dark';
        document.documentElement.setAttribute('data-theme', savedTheme);
    }

    // =========================================================================
    // 2. Mobile Drawer Navigation
    // =========================================================================
    function openDrawer() {
        const drawer = $('#mobileDrawer');
        const hamburger = $('#hamburger');
        if (drawer) {
            drawer.classList.add('open');
            drawer.setAttribute('aria-hidden', 'false');
        }
        if (hamburger) hamburger.setAttribute('aria-expanded', 'true');
    }

    function closeDrawer() {
        const drawer = $('#mobileDrawer');
        const hamburger = $('#hamburger');
        if (drawer) {
            drawer.classList.remove('open');
            drawer.setAttribute('aria-hidden', 'true');
        }
        if (hamburger) hamburger.setAttribute('aria-expanded', 'false');
    }

    // =========================================================================
    // 3. Interactive Phone Simulator
    // =========================================================================
    const phoneModes = {
        'gguf': { status: 'LOCAL_GGUF • Quick Mode', ram: '420MB / Safe' },
        'thinking': { status: 'LOCAL_GGUF • Thinking Mode', ram: '680MB / Safe' },
        'vault': { status: 'Obsidian Dual-Write Active', ram: '100% Synced' },
        'mcp': { status: 'MCP Agent • Tool Loop', ram: 'Orchestrator Online' },
        'cloud': { status: 'Multi-Cloud Router (BYOK)', ram: 'Cloud Zero-RAM' }
    };

    function setPhoneMode(mode) {
        if (!phoneModes[mode]) return;
        
        $$('.mode-tab-btn').forEach(btn => {
            const isActive = btn.dataset.mode === mode;
            btn.classList.toggle('active', isActive);
            btn.setAttribute('aria-selected', isActive ? 'true' : 'false');
        });

        $$('.phone-view').forEach(v => {
            v.classList.toggle('active', v.id === `view-${mode}`);
        });

        const statusLabel = $('#phoneStatusLabel');
        const ramTag = $('#phoneRamTag');
        if (statusLabel) statusLabel.textContent = phoneModes[mode].status;
        if (ramTag) {
            ramTag.innerHTML = `<span class="ram-dot"></span><span>${phoneModes[mode].ram}</span>`;
        }
    }

    function updateClock() {
        const clockEl = $('#deviceClock');
        if (clockEl) {
            const now = new Date();
            clockEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
        }
    }

    // =========================================================================
    // 4. Interactive Prompt Lab Studio (PromptLabScreen.kt)
    // =========================================================================
    const promptLabSamples = {
        'summarize': {
            input: "Kotlin Coroutines provide a declarative way to write asynchronous, non-blocking code. By using suspend functions, dispatchers (Dispatchers.IO, Dispatchers.Default, Dispatchers.Main), and structured concurrency with CoroutineScope, developers can prevent memory leaks and coordinate complex parallel tasks seamlessly without callback hell.",
            output: "### Summary Overview\n- **Core Mechanism**: Non-blocking asynchronous programming via `suspend` functions.\n- **Thread Management**: Clear thread boundaries through Dispatchers (`IO`, `Default`, `Main`).\n- **Reliability**: Structured concurrency guarantees automatic child cancellation and zero memory leaks.",
            telemetry: "Mode: LOCAL_GGUF • Temperature: 0.7 • Max Tokens: 512"
        },
        'improve': {
            input: "i made a new feature in my app that lets users chat with local models and it saves chats in markdown files so people can open it in obsidian and not lose data.",
            output: "### Refined Copy\n"Engineered a native on-device AI orchestration feature enabling fully offline local inference. All conversational streams are dual-written as open Markdown documents with structured YAML frontmatter—providing seamless, zero-lock-in integration with Obsidian vaults."",
            telemetry: "Mode: LOCAL_GGUF • Temperature: 0.3 • Max Tokens: 512"
        },
        'explain': {
            input: "val state by viewModel.uiState.collectAsStateWithLifecycle()\nText(text = state.userQuery)",
            output: "### Architectural Breakdown\n1. **`collectAsStateWithLifecycle()`**: Safely collects the `StateFlow` only while the Android Activity/Fragment is at least in the `STARTED` lifecycle state, conserving CPU and battery in the background.\n2. **`by` Delegate**: Unwraps the Compose `State<T>` object into a direct reference, triggering recomposition only when `state.userQuery` value changes.",
            telemetry: "Mode: LOCAL_GGUF • Temperature: 0.2 • Max Tokens: 512"
        }
    };

    let activePromptTool = 'summarize';

    function setPromptLabTool(tool) {
        if (!promptLabSamples[tool]) return;
        activePromptTool = tool;

        $$('.lab-tab').forEach(tab => {
            const isActive = tab.dataset.tool === tool;
            tab.classList.toggle('active', isActive);
            tab.setAttribute('aria-selected', isActive ? 'true' : 'false');
        });

        const inputArea = $('#labInputText');
        const charCounter = $('#labCharCounter');
        const telemetryFooter = $('.lab-telemetry-footer');

        if (inputArea) {
            inputArea.value = promptLabSamples[tool].input;
            if (charCounter) charCounter.textContent = `${inputArea.value.length} characters`;
        }

        if (telemetryFooter) {
            telemetryFooter.innerHTML = `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" style="display:inline; vertical-align:middle; margin-right:4px;"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg><span>${promptLabSamples[tool].telemetry}</span>`;
        }
    }

    function runPromptTransformation() {
        const sample = promptLabSamples[activePromptTool];
        const executeBtn = $('#labExecuteBtn');
        const outputArea = $('#labOutputArea');
        if (!sample || !outputArea) return;

        if (executeBtn) executeBtn.disabled = true;
        outputArea.innerHTML = '<div style="color: var(--brand-orange); font-family: var(--font-mono); font-size: 0.85rem; display: flex; align-items: center; gap: 6px;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> Processing with local LiteRT / GGUF engine...</div>';

        setTimeout(() => {
            outputArea.innerHTML = '';
            const fullText = sample.output.replace(/\\n/g, '<br>').replace(/\n/g, '<br>');
            let index = 0;
            
            const interval = setInterval(() => {
                index += 8;
                outputArea.innerHTML = fullText.slice(0, index) + '<span style="color: var(--brand-orange); animation: blinkDot 0.8s infinite;">▌</span>';
                
                if (index >= fullText.length) {
                    clearInterval(interval);
                    outputArea.innerHTML = fullText;
                    if (executeBtn) executeBtn.disabled = false;
                    showToast('Transformation completed in 0.42s!', 'bolt');
                }
            }, 25);
        }, 350);
    }

    // =========================================================================
    // 5. HuggingFace GGUF Download Simulator (HuggingFaceApi.kt)
    // =========================================================================
    let isDownloadingModel = false;

    function startModelDownload(modelName) {
        if (isDownloadingModel) {
            showToast('Download currently in progress...', 'clock');
            return;
        }

        const simCard = $('#downloadSimulationCard');
        const simModelName = $('#simModelName');
        const simStatus = $('#simStatus');
        const simSpeed = $('#simSpeed');
        const simProgressBar = $('#simProgressBar');
        const simPercent = $('#simPercent');

        isDownloadingModel = true;

        if (simModelName) simModelName.textContent = modelName;
        if (simStatus) simStatus.textContent = 'Streaming from HuggingFace...';
        if (simProgressBar) simProgressBar.style.width = '0%';
        if (simPercent) simPercent.textContent = '0%';

        if (simCard) {
            simCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }

        let progress = 0;
        const interval = setInterval(() => {
            progress += Math.floor(Math.random() * 9) + 5;
            const speed = (Math.random() * 8 + 14).toFixed(1);

            if (simProgressBar) simProgressBar.style.width = `${Math.min(progress, 100)}%`;
            if (simPercent) simPercent.textContent = `${Math.min(progress, 100)}%`;
            if (simSpeed) simSpeed.textContent = `${speed} MB/s`;

            if (progress >= 100) {
                clearInterval(interval);
                isDownloadingModel = false;
                if (simStatus) simStatus.textContent = 'Saved to Documents/Oorty/models/';
                if (simSpeed) simSpeed.textContent = 'Done';
                showToast(`Successfully saved ${modelName} to vault!`, 'check');
            }
        }, 160);
    }

    // =========================================================================
    // 6. Dynamic RAM Watchdog Calculator (HardwareChecker.kt)
    // =========================================================================
    const ramProfiles = {
        '4': {
            title: 'Qwen 2.5 (0.5B-Instruct)',
            quant: 'Quant: Q4_K_M • ~450 MB VRAM',
            badgeClass: 'badge-safe',
            badgeText: 'Comfortable Fit',
            explanation: 'Perfect for 4GB RAM devices. Operates with near-zero background pressure and achieves the highest decode speeds (35+ tok/s).',
            gaugeVal: '~450 MB / 4,096 MB (11.0%)',
            gaugePercent: '11%',
            gaugeColor: 'fill-green',
            speed: '~35 tok/s',
            context: '2,048 Tokens',
            agentic: 'Limited (Basic)',
            risk: 'Zero (Safe)',
            riskClass: 'text-green'
        },
        '6': {
            title: 'Llama 3.2 (1B-Instruct)',
            quant: 'Quant: Q4_K_M • ~850 MB VRAM',
            badgeClass: 'badge-safe',
            badgeText: 'Comfortable Fit',
            explanation: 'Optimal choice for 6GB devices. Delivers 25–35 tok/s decode speed with fast responses and low battery consumption. Leaves ample RAM for Android OS.',
            gaugeVal: '~850 MB / 6,144 MB (13.8%)',
            gaugePercent: '14%',
            gaugeColor: 'fill-green',
            speed: '~30 tok/s',
            context: '4,096 Tokens',
            agentic: 'Limited (Fast)',
            risk: 'Zero (Safe)',
            riskClass: 'text-green'
        },
        '8': {
            title: 'Gemma 2 (2B-IT)',
            quant: 'Quant: Q4_K_M • ~1.6 GB VRAM',
            badgeClass: 'badge-safe',
            badgeText: 'Comfortable Fit',
            explanation: 'High reasoning performance with Google’s Gemma 2 architecture. Supports multi-step tool calls and code generation on 8GB RAM devices.',
            gaugeVal: '~1,600 MB / 8,192 MB (19.5%)',
            gaugePercent: '20%',
            gaugeColor: 'fill-green',
            speed: '~20 tok/s',
            context: '4,096 Tokens',
            agentic: 'Moderate',
            risk: 'Zero (Safe)',
            riskClass: 'text-green'
        },
        '12': {
            title: 'Phi-3-Mini (3.8B-4k) / Llama 3.1 8B',
            quant: 'Quant: Q4_K_M • ~2.4 – 4.8 GB VRAM',
            badgeClass: 'badge-safe',
            badgeText: 'Full Power Mode',
            explanation: 'Full autonomous capability. Supports complex MCP tool chains, deep reasoning, long context recall, and sophisticated coding tasks without slowdown.',
            gaugeVal: '~2,400 MB / 12,288 MB (19.5%)',
            gaugePercent: '22%',
            gaugeColor: 'fill-green',
            speed: '~14–22 tok/s',
            context: '8,192 Tokens',
            agentic: 'Full Autonomous',
            risk: 'Zero (Safe)',
            riskClass: 'text-green'
        }
    };

    function setRamProfile(ramKey) {
        const profile = ramProfiles[ramKey];
        if (!profile) return;

        $$('.ram-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.ram === ramKey);
        });

        const titleEl = $('#calcModelTitle');
        const quantEl = $('#calcQuantTag');
        const badgeEl = $('#calcSafetyBadge');
        const expEl = $('#calcExplanation');
        const gaugeValEl = $('#calcGaugeVal');
        const gaugeFillEl = $('#calcGaugeFill');
        const speedEl = $('#calcSpeedVal');
        const contextEl = $('#calcContextVal');
        const agenticEl = $('#calcAgenticVal');
        const riskEl = $('#calcRiskVal');

        if (titleEl) titleEl.textContent = profile.title;
        if (quantEl) quantEl.textContent = profile.quant;
        if (expEl) expEl.textContent = profile.explanation;
        if (gaugeValEl) gaugeValEl.textContent = profile.gaugeVal;
        if (gaugeFillEl) {
            gaugeFillEl.style.width = profile.gaugePercent;
            gaugeFillEl.className = `gauge-bar-fill ${profile.gaugeColor}`;
        }
        if (badgeEl) {
            badgeEl.className = `ram-safety-badge ${profile.badgeClass}`;
            badgeEl.innerHTML = `<span class="badge-dot-svg"></span><span>${profile.badgeText}</span>`;
        }
        if (speedEl) speedEl.textContent = profile.speed;
        if (contextEl) contextEl.textContent = profile.context;
        if (agenticEl) agenticEl.textContent = profile.agentic;
        if (riskEl) {
            riskEl.textContent = profile.risk;
            riskEl.className = `metric-value ${profile.riskClass}`;
        }
    }

    // =========================================================================
    // 7. Obsidian Split View (Raw Markdown vs Rendered)
    // =========================================================================
    function setObsidianTab(viewMode) {
        const isRaw = viewMode === 'raw';
        const btnRaw = $('#btnRawMd');
        const btnRendered = $('#btnRenderedMd');
        const paneRaw = $('#paneRawMd');
        const paneRendered = $('#paneRenderedMd');

        if (btnRaw) {
            btnRaw.classList.toggle('active', isRaw);
            btnRaw.setAttribute('aria-selected', isRaw ? 'true' : 'false');
        }
        if (btnRendered) {
            btnRendered.classList.toggle('active', !isRaw);
            btnRendered.setAttribute('aria-selected', !isRaw ? 'true' : 'false');
        }
        if (paneRaw) paneRaw.classList.toggle('active', isRaw);
        if (paneRendered) paneRendered.classList.toggle('active', !isRaw);
    }

    // =========================================================================
    // 8. MCP Agent Simulator (ToolCallOrchestrator.kt)
    // =========================================================================
    function runMcpAgentSimulation() {
        const runBtn = $('#btnSimulateMcpRun');
        const logsBox = $('#traceLogsBox');
        const badge = $('#traceBadge');
        if (!runBtn || !logsBox) return;

        const traceSteps = [
            '[ORCHESTRATOR] Received goal: "Check latest SQLite note entries and summarize performance benchmarks."',
            '[STEP 1] Inspecting tool policies: Tool `sqlite_query` authorized.',
            '[EXECUTE] `sqlite_query(query="SELECT * FROM benchmarks ORDER BY date DESC LIMIT 3")`',
            '[RESULT] Found 3 benchmark rows (Qwen 2.5: 35.2 tok/s, Llama 3.2: 29.8 tok/s).',
            '[STEP 2] Inspecting tool `recall_from_vault` for matching topic notes.',
            '[EXECUTE] Vector cosine similarity match score: 0.942 on `jetpack-compose-perf.md`.',
            '[SYNTHESIS] Combining structured data with vault context into final response.',
            '[DONE] Agent loop finished in 2 iterations (0.88s total runtime).'
        ];

        runBtn.disabled = true;
        if (badge) {
            badge.className = 'trace-badge-running';
            badge.textContent = 'Running...';
        }
        logsBox.innerHTML = '';

        let stepIndex = 0;
        const interval = setInterval(() => {
            if (stepIndex < traceSteps.length) {
                const row = document.createElement('div');
                row.className = 'trace-item';
                row.style.color = stepIndex === traceSteps.length - 1 ? '#22C55E' : '#D4D4D8';
                row.textContent = traceSteps[stepIndex];
                logsBox.appendChild(row);
                logsBox.scrollTop = logsBox.scrollHeight;
                stepIndex++;
            } else {
                clearInterval(interval);
                runBtn.disabled = false;
                if (badge) {
                    badge.className = 'trace-badge-idle';
                    badge.textContent = 'Completed';
                }
                showToast('MCP Agent simulation finished!', 'wrench');
            }
        }, 300);
    }

    // =========================================================================
    // 9. Architecture & Code Tab Switcher
    // =========================================================================
    function setCodeTab(codeKey) {
        $$('.code-tab-btn').forEach(btn => {
            const isActive = btn.dataset.code === codeKey;
            btn.classList.toggle('active', isActive);
            btn.setAttribute('aria-selected', isActive ? 'true' : 'false');
        });

        $$('.code-tab-content').forEach(content => {
            content.classList.toggle('active', content.id === `tab-code-${codeKey}`);
        });
    }

    // =========================================================================
    // 10. Global Document Event Delegation (Never Misses Clicks)
    // =========================================================================
    function setupEventDelegation() {
        document.addEventListener('click', (e) => {
            // Theme Toggle
            const themeBtn = e.target.closest('#themeToggle');
            if (themeBtn) {
                e.preventDefault();
                toggleTheme();
                return;
            }

            // Mobile Drawer Toggle / Close
            const hamburger = e.target.closest('#hamburger');
            if (hamburger) {
                e.preventDefault();
                openDrawer();
                return;
            }

            const closeDrawerBtn = e.target.closest('#drawerClose') || e.target.closest('.drawer-link');
            if (closeDrawerBtn) {
                closeDrawer();
                return;
            }

            // RAM Matrix Buttons (4GB, 6GB, 8GB, 12GB+)
            const ramBtn = e.target.closest('.ram-btn');
            if (ramBtn) {
                e.preventDefault();
                const ram = ramBtn.dataset.ram;
                if (ram) setRamProfile(ram);
                return;
            }

            // Prompt Lab Tool Tabs (Summarize, Improve, Explain)
            const labTab = e.target.closest('.lab-tab');
            if (labTab) {
                e.preventDefault();
                const tool = labTab.dataset.tool;
                if (tool) setPromptLabTool(tool);
                return;
            }

            // Prompt Lab Load Sample
            const sampleBtn = e.target.closest('#labSampleBtn');
            if (sampleBtn) {
                e.preventDefault();
                const sample = promptLabSamples[activePromptTool];
                const inputArea = $('#labInputText');
                const charCounter = $('#labCharCounter');
                if (sample && inputArea) {
                    inputArea.value = sample.input;
                    if (charCounter) charCounter.textContent = `${inputArea.value.length} characters`;
                    showToast(`Loaded sample for ${activePromptTool.toUpperCase()}`, 'document');
                }
                return;
            }

            // Prompt Lab Run Transformation
            const runTransformBtn = e.target.closest('#labExecuteBtn');
            if (runTransformBtn) {
                e.preventDefault();
                runPromptTransformation();
                return;
            }

            // Prompt Lab Copy Output
            const copyOutputBtn = e.target.closest('#labCopyOutputBtn');
            if (copyOutputBtn) {
                e.preventDefault();
                const outputArea = $('#labOutputArea');
                const text = outputArea ? outputArea.innerText : '';
                if (text && !text.includes('Select a tool')) {
                    navigator.clipboard.writeText(text);
                    showToast('Transformed text copied to clipboard!', 'copy');
                } else {
                    showToast('Run a transformation first!', 'warning');
                }
                return;
            }

            // HuggingFace Model Card Download Buttons
            const downloadModelBtn = e.target.closest('.btn-download-model');
            if (downloadModelBtn) {
                e.preventDefault();
                const model = downloadModelBtn.dataset.model || 'model-Q4_K_M.gguf';
                startModelDownload(model);
                return;
            }

            // In-Phone Simulator Tabs
            const phoneTab = e.target.closest('.mode-tab-btn');
            if (phoneTab) {
                e.preventDefault();
                const mode = phoneTab.dataset.mode;
                if (mode) setPhoneMode(mode);
                return;
            }

            // Obsidian Raw / Rendered Tabs
            const rawTab = e.target.closest('#btnRawMd');
            if (rawTab) {
                e.preventDefault();
                setObsidianTab('raw');
                return;
            }
            const renderedTab = e.target.closest('#btnRenderedMd');
            if (renderedTab) {
                e.preventDefault();
                setObsidianTab('rendered');
                return;
            }

            // Copy Obsidian Note Button
            const copyNoteBtn = e.target.closest('#btnCopyNote');
            if (copyNoteBtn) {
                e.preventDefault();
                const rawCode = $('#paneRawMd')?.querySelector('code')?.innerText;
                if (rawCode) {
                    navigator.clipboard.writeText(rawCode);
                    showToast('Obsidian Markdown note copied!', 'copy');
                }
                return;
            }

            // Code Architecture Tabs
            const codeTab = e.target.closest('.code-tab-btn');
            if (codeTab) {
                e.preventDefault();
                const codeKey = codeTab.dataset.code;
                if (codeKey) setCodeTab(codeKey);
                return;
            }

            // Generic Snippet Copy Buttons
            const copySnippetBtn = e.target.closest('.btn-copy-code');
            if (copySnippetBtn) {
                e.preventDefault();
                const text = copySnippetBtn.dataset.copy || 
                             (copySnippetBtn.dataset.target ? $(copySnippetBtn.dataset.target)?.innerText : '') ||
                             copySnippetBtn.closest('.code-snippet-box, .code-tab-content')?.querySelector('pre, code')?.innerText;
                if (text) {
                    navigator.clipboard.writeText(text);
                    showToast('Snippet copied to clipboard!', 'copy');
                }
                return;
            }

            // Run MCP Agent Simulation Button
            const mcpRunBtn = e.target.closest('#btnSimulateMcpRun');
            if (mcpRunBtn) {
                e.preventDefault();
                runMcpAgentSimulation();
                return;
            }

            // Smooth Scroll for CTA Buttons & Internal Anchor Links
            const anchor = e.target.closest('a[href^="#"]');
            if (anchor) {
                const targetId = anchor.getAttribute('href');
                if (targetId && targetId !== '#') {
                    const targetEl = $(targetId);
                    if (targetEl) {
                        e.preventDefault();
                        targetEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    }
                }
            }
        });

        // Textarea live character counter
        const inputArea = $('#labInputText');
        const charCounter = $('#labCharCounter');
        if (inputArea && charCounter) {
            inputArea.addEventListener('input', () => {
                charCounter.textContent = `${inputArea.value.length} characters`;
            });
        }

        // FAQ real-time search filter
        const searchInput = $('#faqInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const query = e.target.value.toLowerCase().trim();
                $$('.faq-card').forEach(card => {
                    const text = card.textContent.toLowerCase();
                    const matches = text.includes(query);
                    card.style.display = matches ? 'block' : 'none';
                    if (query && matches) {
                        card.setAttribute('open', '');
                    } else if (!query) {
                        card.removeAttribute('open');
                    }
                });
            });
        }
    }

    // =========================================================================
    // Initialization Bootstrap
    // =========================================================================
    function initApp() {
        initTheme();
        updateClock();
        setInterval(updateClock, 30000);
        setupEventDelegation();

        // Initial setup for Prompt Lab
        const inputArea = $('#labInputText');
        const charCounter = $('#labCharCounter');
        if (inputArea && promptLabSamples[activePromptTool]) {
            inputArea.value = promptLabSamples[activePromptTool].input;
            if (charCounter) charCounter.textContent = `${inputArea.value.length} characters`;
        }

        // Initial RAM Matrix setup (default 6GB)
        setRamProfile('6');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initApp);
    } else {
        initApp();
    }

})();
