// ==========================================================================
// Sednium Oorty — Comprehensive Interactive Controller & Studio Engine
// ==========================================================================

(function () {
    'use strict';

    // Utility Helpers
    const $ = (sel, ctx = document) => ctx.querySelector(sel);
    const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

    // SVG Icon Map for Toasts and Components
    const SVG_ICONS = {
        check: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>',
        copy: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>',
        moon: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>',
        sun: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/></svg>',
        bolt: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>',
        document: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>',
        clock: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
        warning: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
        wrench: '<svg class="toast-svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>'
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
    // 1. Theme Toggle (Dark / Light Mode)
    // =========================================================================
    function initTheme() {
        const toggleBtn = $('#themeToggle');
        const root = document.documentElement;
        
        // Retrieve saved theme or default to dark
        const savedTheme = localStorage.getItem('oorty-theme') || 'dark';
        root.setAttribute('data-theme', savedTheme);

        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => {
                const current = root.getAttribute('data-theme');
                const next = current === 'dark' ? 'light' : 'dark';
                root.setAttribute('data-theme', next);
                localStorage.setItem('oorty-theme', next);
                showToast(`Switched to ${next.toUpperCase()} theme`, next === 'dark' ? 'moon' : 'sun');
            });
        }
    }

    // =========================================================================
    // 2. Mobile Drawer Navigation
    // =========================================================================
    function initMobileNav() {
        const hamburger = $('#hamburger');
        const drawer = $('#mobileDrawer');
        const closeBtn = $('#drawerClose');
        const drawerLinks = $$('.drawer-link');

        if (!hamburger || !drawer) return;

        const openDrawer = () => {
            drawer.classList.add('open');
            drawer.setAttribute('aria-hidden', 'false');
            hamburger.setAttribute('aria-expanded', 'true');
        };
        const closeDrawer = () => {
            drawer.classList.remove('open');
            drawer.setAttribute('aria-hidden', 'true');
            hamburger.setAttribute('aria-expanded', 'false');
        };

        hamburger.addEventListener('click', openDrawer);
        if (closeBtn) closeBtn.addEventListener('click', closeDrawer);
        drawer.addEventListener('click', (e) => {
            if (e.target === drawer) closeDrawer();
        });
        drawerLinks.forEach(l => l.addEventListener('click', closeDrawer));
    }

    // =========================================================================
    // 3. Interactive Phone Simulator
    // =========================================================================
    function initPhoneSimulator() {
        // Clock updater
        const clockEl = $('#deviceClock');
        if (clockEl) {
            const updateClock = () => {
                const now = new Date();
                clockEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
            };
            updateClock();
            setInterval(updateClock, 30000);
        }

        // Mode Segmented Tabs
        const modeTabs = $$('.mode-tab-btn');
        const views = $$('.phone-view');
        const statusLabel = $('#phoneStatusLabel');
        const ramTag = $('#phoneRamTag');

        const modeData = {
            'gguf': { status: 'LOCAL_GGUF • Quick Mode', ram: '420MB / Safe', ramClass: 'badge-safe' },
            'thinking': { status: 'LOCAL_GGUF • Thinking Mode', ram: '680MB / Safe', ramClass: 'badge-safe' },
            'vault': { status: 'Obsidian Dual-Write Active', ram: '100% Synced', ramClass: 'badge-safe' },
            'mcp': { status: 'MCP Agent • Tool Loop', ram: 'Orchestrator Online', ramClass: 'badge-safe' },
            'cloud': { status: 'Multi-Cloud Router (BYOK)', ram: 'Cloud Zero-RAM', ramClass: 'badge-safe' }
        };

        modeTabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const mode = tab.dataset.mode;

                modeTabs.forEach(t => {
                    t.classList.toggle('active', t === tab);
                    t.setAttribute('aria-selected', t === tab ? 'true' : 'false');
                });

                views.forEach(v => {
                    v.classList.toggle('active', v.id === `view-${mode}`);
                });

                if (statusLabel && modeData[mode]) {
                    statusLabel.textContent = modeData[mode].status;
                }
                if (ramTag && modeData[mode]) {
                    ramTag.innerHTML = `<span class="ram-dot"></span><span>${modeData[mode].ram}</span>`;
                }
            });
        });

        // Copy buttons inside phone
        $$('.btn-copy-code').forEach(btn => {
            btn.addEventListener('click', () => {
                const text = btn.dataset.copy || btn.closest('.code-snippet-box')?.querySelector('pre')?.innerText;
                if (text) {
                    navigator.clipboard.writeText(text);
                    showToast('Code copied to clipboard!', 'copy');
                }
            });
        });
    }

    // =========================================================================
    // 4. Interactive Prompt Lab Studio (PromptLabScreen.kt)
    // =========================================================================
    const promptLabSamples = {
        'summarize': {
            input: "Kotlin Coroutines provide a declarative way to write asynchronous, non-blocking code. By using suspend functions, dispatchers (Dispatchers.IO, Dispatchers.Default, Dispatchers.Main), and structured concurrency with CoroutineScope, developers can prevent memory leaks and coordinate complex parallel tasks seamlessly without callback hell.",
            output: "### Summary Overview
- **Core Mechanism**: Non-blocking asynchronous programming via `suspend` functions.
- **Thread Management**: Clear thread boundaries through Dispatchers (`IO`, `Default`, `Main`).
- **Reliability**: Structured concurrency guarantees automatic child cancellation and zero memory leaks."
        },
        'improve': {
            input: "i made a new feature in my app that lets users chat with local models and it saves chats in markdown files so people can open it in obsidian and not lose data.",
            output: "### Refined Copy
"Engineered a native on-device AI orchestration feature enabling fully offline local inference. All conversational streams are dual-written as open Markdown documents with structured YAML frontmatter—providing seamless, zero-lock-in integration with Obsidian vaults.""
        },
        'explain': {
            input: "val state by viewModel.uiState.collectAsStateWithLifecycle()
Text(text = state.userQuery)",
            output: "### Architectural Breakdown
1. **`collectAsStateWithLifecycle()`**: Safely collects the `StateFlow` only while the Android Activity/Fragment is at least in the `STARTED` lifecycle state, conserving CPU and battery in the background.
2. **`by` Delegate**: Unwraps the Compose `State<T>` object into a direct reference, triggering recomposition only when `state.userQuery` value changes."
        }
    };

    function initPromptLab() {
        const tabs = $$('.lab-tab');
        const inputArea = $('#labInputText');
        const outputArea = $('#labOutputArea');
        const charCounter = $('#labCharCounter');
        const sampleBtn = $('#labSampleBtn');
        const executeBtn = $('#labExecuteBtn');
        const copyOutputBtn = $('#labCopyOutputBtn');

        let activeTool = 'summarize';

        // Set initial sample
        if (inputArea) {
            inputArea.value = promptLabSamples[activeTool].input;
            if (charCounter) charCounter.textContent = `${inputArea.value.length} characters`;

            inputArea.addEventListener('input', () => {
                if (charCounter) charCounter.textContent = `${inputArea.value.length} characters`;
            });
        }

        // Tab Switching
        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                tabs.forEach(t => {
                    t.classList.remove('active');
                    t.setAttribute('aria-selected', 'false');
                });
                tab.classList.add('active');
                tab.setAttribute('aria-selected', 'true');
                activeTool = tab.dataset.tool;

                if (inputArea && promptLabSamples[activeTool]) {
                    inputArea.value = promptLabSamples[activeTool].input;
                    if (charCounter) charCounter.textContent = `${inputArea.value.length} characters`;
                }
            });
        });

        // Load Sample
        if (sampleBtn) {
            sampleBtn.addEventListener('click', () => {
                if (inputArea && promptLabSamples[activeTool]) {
                    inputArea.value = promptLabSamples[activeTool].input;
                    if (charCounter) charCounter.textContent = `${inputArea.value.length} characters`;
                    showToast(`Loaded sample for ${activeTool.toUpperCase()}`, 'document');
                }
            });
        }

        // Run Transformation (Typewriter Simulation)
        if (executeBtn && outputArea) {
            executeBtn.addEventListener('click', () => {
                const sample = promptLabSamples[activeTool];
                if (!sample) return;

                executeBtn.disabled = true;
                outputArea.innerHTML = '<div style="color: var(--brand-orange); font-family: var(--font-mono); font-size: 0.85rem; display: flex; align-items: center; gap: 6px;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> Processing with local LiteRT / GGUF engine...</div>';

                setTimeout(() => {
                    outputArea.innerHTML = '';
                    const fullText = sample.output.replace(/\n/g, '<br>');
                    let index = 0;
                    
                    const interval = setInterval(() => {
                        index += 8;
                        outputArea.innerHTML = fullText.slice(0, index) + '<span style="color: var(--brand-orange);">▌</span>';
                        
                        if (index >= fullText.length) {
                            clearInterval(interval);
                            outputArea.innerHTML = fullText;
                            executeBtn.disabled = false;
                            showToast('Transformation completed in 0.42s!', 'bolt');
                        }
                    }, 25);
                }, 400);
            });
        }

        // Copy Output
        if (copyOutputBtn && outputArea) {
            copyOutputBtn.addEventListener('click', () => {
                const text = outputArea.innerText;
                if (text && !text.includes('Select a tool')) {
                    navigator.clipboard.writeText(text);
                    showToast('Transformed text copied!', 'copy');
                } else {
                    showToast('Run a transformation first!', 'warning');
                }
            });
        }
    }

    // =========================================================================
    // 5. HuggingFace GGUF Download Simulator
    // =========================================================================
    function initHfHubSimulator() {
        const downloadBtns = $$('.btn-download-model');
        const simCard = $('#downloadSimulationCard');
        const simModelName = $('#simModelName');
        const simStatus = $('#simStatus');
        const simSpeed = $('#simSpeed');
        const simProgressBar = $('#simProgressBar');
        const simPercent = $('#simPercent');

        let isDownloading = false;

        downloadBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                if (isDownloading) {
                    showToast('Download currently in progress...', 'clock');
                    return;
                }

                const model = btn.dataset.model || 'model-Q4_K_M.gguf';
                isDownloading = true;

                if (simModelName) simModelName.textContent = model;
                if (simStatus) simStatus.textContent = 'Streaming from HuggingFace...';
                if (simProgressBar) simProgressBar.style.width = '0%';
                if (simPercent) simPercent.textContent = '0%';

                // Scroll to widget smoothly
                simCard?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

                let progress = 0;
                const interval = setInterval(() => {
                    progress += Math.floor(Math.random() * 8) + 4;
                    const speed = (Math.random() * 8 + 14).toFixed(1);

                    if (simProgressBar) simProgressBar.style.width = `${Math.min(progress, 100)}%`;
                    if (simPercent) simPercent.textContent = `${Math.min(progress, 100)}%`;
                    if (simSpeed) simSpeed.textContent = `${speed} MB/s`;

                    if (progress >= 100) {
                        clearInterval(interval);
                        isDownloading = false;
                        if (simStatus) simStatus.textContent = 'Saved to Documents/Oorty/models/';
                        if (simSpeed) simSpeed.textContent = 'Done';
                        showToast(`Successfully saved ${model} to vault!`, 'check');
                    }
                }, 180);
            });
        });
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
            gaugeVal: '~450 MB / 4,096 MB (11%)',
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

    function initRamCalculator() {
        const ramBtns = $$('.ram-btn');
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

        ramBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                ramBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');

                const ram = btn.dataset.ram;
                const profile = ramProfiles[ram];
                if (!profile) return;

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
            });
        });
    }

    // =========================================================================
    // 7. Obsidian Split View (Raw Markdown vs Rendered)
    // =========================================================================
    function initObsidianViewer() {
        const btnRaw = $('#btnRawMd');
        const btnRendered = $('#btnRenderedMd');
        const paneRaw = $('#paneRawMd');
        const paneRendered = $('#paneRenderedMd');
        const copyNoteBtn = $('#btnCopyNote');

        if (btnRaw && btnRendered && paneRaw && paneRendered) {
            btnRaw.addEventListener('click', () => {
                btnRaw.classList.add('active');
                btnRaw.setAttribute('aria-selected', 'true');
                btnRendered.classList.remove('active');
                btnRendered.setAttribute('aria-selected', 'false');
                paneRaw.classList.add('active');
                paneRendered.classList.remove('active');
            });

            btnRendered.addEventListener('click', () => {
                btnRendered.classList.add('active');
                btnRendered.setAttribute('aria-selected', 'true');
                btnRaw.classList.remove('active');
                btnRaw.setAttribute('aria-selected', 'false');
                paneRendered.classList.add('active');
                paneRaw.classList.remove('active');
            });
        }

        if (copyNoteBtn) {
            copyNoteBtn.addEventListener('click', () => {
                const rawCode = paneRaw?.querySelector('code')?.innerText;
                if (rawCode) {
                    navigator.clipboard.writeText(rawCode);
                    showToast('Obsidian Markdown note copied!', 'copy');
                }
            });
        }
    }

    // =========================================================================
    // 8. MCP Agent Simulator
    // =========================================================================
    function initMcpSimulator() {
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

        runBtn.addEventListener('click', () => {
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
            }, 350);
        });
    }

    // =========================================================================
    // 9. Architecture & Code Tab Switcher
    // =========================================================================
    function initCodeTabs() {
        const tabs = $$('.code-tab-btn');
        const contents = $$('.code-tab-content');

        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const codeId = tab.dataset.code;

                tabs.forEach(t => {
                    t.classList.toggle('active', t === tab);
                    t.setAttribute('aria-selected', t === tab ? 'true' : 'false');
                });
                contents.forEach(c => c.classList.toggle('active', c.id === `tab-code-${codeId}`));
            });
        });

        $$('.btn-copy-code').forEach(btn => {
            btn.addEventListener('click', () => {
                const targetSel = btn.dataset.target;
                const codeEl = targetSel ? $(targetSel) : btn.closest('.code-header')?.nextElementSibling;
                if (codeEl) {
                    navigator.clipboard.writeText(codeEl.innerText);
                    showToast('Code snippet copied!', 'copy');
                }
            });
        });
    }

    // =========================================================================
    // 10. FAQ Search & Accordion
    // =========================================================================
    function initFaqSearch() {
        const searchInput = $('#faqInput');
        const faqCards = $$('.faq-card');

        if (!searchInput) return;

        searchInput.addEventListener('input', (e) => {
            const query = e.target.value.toLowerCase().trim();

            faqCards.forEach(card => {
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

    // =========================================================================
    // 11. Scroll Animations (IntersectionObserver)
    // =========================================================================
    function initScrollAnimations() {
        const elements = $$('.fade-in');
        if (!elements.length) return;

        // Reveal all visible elements immediately
        elements.forEach(el => {
            const rect = el.getBoundingClientRect();
            if (rect.top < window.innerHeight) {
                el.classList.add('visible');
            }
        });

        if ('IntersectionObserver' in window) {
            const observer = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('visible');
                    }
                });
            }, { threshold: 0.08 });

            elements.forEach(el => observer.observe(el));
        } else {
            elements.forEach(el => el.classList.add('visible'));
        }
    }

    // =========================================================================
    // Bootstrap / Initialization Safe Check
    // =========================================================================
    function initAll() {
        initTheme();
        initMobileNav();
        initPhoneSimulator();
        initPromptLab();
        initHfHubSimulator();
        initRamCalculator();
        initObsidianViewer();
        initMcpSimulator();
        initCodeTabs();
        initFaqSearch();
        initScrollAnimations();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll);
    } else {
        initAll();
    }

})();
