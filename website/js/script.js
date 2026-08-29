// ==========================================================================
// Sednium Oorty — Comprehensive Interactive Controller & Studio Engine
// ==========================================================================

(function () {
    'use strict';

    // Utility Helpers
    const $ = (sel, ctx = document) => ctx.querySelector(sel);
    const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

    // Toast Notification Dispatcher
    function showToast(message, icon = '✓') {
        const container = $('#toast-container');
        if (!container) return;
        
        const toast = document.createElement('div');
        toast.className = 'toast';
        toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
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
        
        // Retrieve saved theme or system preference
        const savedTheme = localStorage.getItem('oorty-theme') || 
            (window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');
            
        root.setAttribute('data-theme', savedTheme);

        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => {
                const current = root.getAttribute('data-theme');
                const next = current === 'dark' ? 'light' : 'dark';
                root.setAttribute('data-theme', next);
                localStorage.setItem('oorty-theme', next);
                showToast(`Switched to ${next.toUpperCase()} theme`, next === 'dark' ? '🌙' : '☀️');
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

        const openDrawer = () => drawer.classList.add('open');
        const closeDrawer = () => drawer.classList.remove('open');

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
                    showToast('Code copied to clipboard!', '📋');
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
            output: "### 📝 Summary Overview\n- **Core Mechanism**: Non-blocking asynchronous programming via `suspend` functions.\n- **Thread Management**: Clear thread boundaries through Dispatchers (`IO`, `Default`, `Main`).\n- **Reliability**: Structured concurrency guarantees automatic child cancellation and zero memory leaks."
        },
        'improve': {
            input: "i made a new feature in my app that lets users chat with local models and it saves chats in markdown files so people can open it in obsidian and not lose data.",
            output: "### ✍️ Refined Copy\n\"Engineered a native on-device AI orchestration feature enabling fully offline local inference. All conversational streams are dual-written as open Markdown documents with structured YAML frontmatter—providing seamless, zero-lock-in integration with Obsidian vaults.\""
        },
        'explain': {
            input: "val state by viewModel.uiState.collectAsStateWithLifecycle()\nText(text = state.userQuery)",
            output: "### 💻 Architectural Breakdown\n1. **`collectAsStateWithLifecycle()`**: Safely collects the `StateFlow` only while the Android Activity/Fragment is at least in the `STARTED` lifecycle state, conserving CPU and battery in the background.\n2. **`by` Delegate**: Unwraps the Compose `State<T>` object into a direct reference, triggering recomposition only when `state.userQuery` value changes."
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
                tabs.forEach(t => t.classList.remove('active'));
                tab.classList.add('active');
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
                    showToast(`Loaded sample for ${activeTool.toUpperCase()}`, '📝');
                }
            });
        }

        // Run Transformation (Typewriter Simulation)
        if (executeBtn && outputArea) {
            executeBtn.addEventListener('click', () => {
                const sample = promptLabSamples[activeTool];
                if (!sample) return;

                executeBtn.disabled = true;
                outputArea.innerHTML = '<div style="color: var(--brand-orange); font-family: var(--font-mono); font-size: 0.85rem;"><span class="pulse-icon">⚡</span> Processing with local LiteRT / GGUF engine...</div>';

                setTimeout(() => {
                    outputArea.innerHTML = '';
                    const fullText = sample.output.replace(/\\n/g, '<br>');
                    let index = 0;
                    
                    const interval = setInterval(() => {
                        index += 8;
                        outputArea.innerHTML = fullText.slice(0, index) + '<span style="color: var(--brand-orange);">▌</span>';
                        
                        if (index >= fullText.length) {
                            clearInterval(interval);
                            outputArea.innerHTML = fullText;
                            executeBtn.disabled = false;
                            showToast('Transformation completed in 0.42s!', '⚡');
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
                    showToast('Transformed text copied!', '📋');
                } else {
                    showToast('Run a transformation first!', '⚠️');
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
                    showToast('Download currently in progress...', '⏳');
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
                        if (simStatus) simStatus.textContent = '✓ Saved to Documents/Oorty/models/';
                        if (simSpeed) simSpeed.textContent = 'Done';
                        showToast(`Successfully saved ${model} to vault!`, '🎉');
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
            badgeIcon: '🟢',
            explanation: 'Perfect for 4GB RAM devices. Operates with near-zero background pressure and achieves the highest decode speeds (35+ tok/s).',
            gaugeVal: '~450 MB / 4,096 MB (11%)',
            gaugePercent: '11%',
            gaugeColor: 'fill-green',
            speed: '~35 tok/s',
            context: '2,048 Tokens',
            agentic: '⚡ Limited (Basic)',
            risk: 'Zero (Safe)',
            riskClass: 'text-green'
        },
        '6': {
            title: 'Llama 3.2 (1B-Instruct)',
            quant: 'Quant: Q4_K_M • ~850 MB VRAM',
            badgeClass: 'badge-safe',
            badgeText: 'Comfortable Fit',
            badgeIcon: '🟢',
            explanation: 'Optimal choice for 6GB devices. Delivers 25–35 tok/s decode speed with fast responses and low battery consumption. Leaves ample RAM for Android OS.',
            gaugeVal: '~850 MB / 6,144 MB (13.8%)',
            gaugePercent: '14%',
            gaugeColor: 'fill-green',
            speed: '~30 tok/s',
            context: '4,096 Tokens',
            agentic: '⚡ Limited (Fast)',
            risk: 'Zero (Safe)',
            riskClass: 'text-green'
        },
        '8': {
            title: 'Gemma 2 (2B-IT)',
            quant: 'Quant: Q4_K_M • ~1.6 GB VRAM',
            badgeClass: 'badge-safe',
            badgeText: 'Comfortable Fit',
            badgeIcon: '🟢',
            explanation: 'High reasoning performance with Google’s Gemma 2 architecture. Supports multi-step tool calls and code generation on 8GB RAM devices.',
            gaugeVal: '~1,600 MB / 8,192 MB (19.5%)',
            gaugePercent: '20%',
            gaugeColor: 'fill-green',
            speed: '~20 tok/s',
            context: '4,096 Tokens',
            agentic: '✅ Moderate',
            risk: 'Zero (Safe)',
            riskClass: 'text-green'
        },
        '12': {
            title: 'Phi-3-Mini (3.8B-4k) / Llama 3.1 8B',
            quant: 'Quant: Q4_K_M • ~2.4 – 4.8 GB VRAM',
            badgeClass: 'badge-safe',
            badgeText: 'Full Power Mode',
            badgeIcon: '🚀',
            explanation: 'Full autonomous capability. Supports complex MCP tool chains, deep reasoning, long context recall, and sophisticated coding tasks without slowdown.',
            gaugeVal: '~2,400 MB / 12,288 MB (19.5%)',
            gaugePercent: '22%',
            gaugeColor: 'fill-green',
            speed: '~14–22 tok/s',
            context: '8,192 Tokens',
            agentic: '🚀 Full Autonomous',
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
                    badgeEl.innerHTML = `<span class="badge-icon">${profile.badgeIcon}</span><span>${profile.badgeText}</span>`;
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
                btnRendered.classList.remove('active');
                paneRaw.classList.add('active');
                paneRendered.classList.remove('active');
            });

            btnRendered.addEventListener('click', () => {
                btnRendered.classList.add('active');
                btnRaw.classList.remove('active');
                paneRendered.classList.add('active');
                paneRaw.classList.remove('active');
            });
        }

        if (copyNoteBtn) {
            copyNoteBtn.addEventListener('click', () => {
                const rawCode = paneRaw?.querySelector('code')?.innerText;
                if (rawCode) {
                    navigator.clipboard.writeText(rawCode);
                    showToast('Obsidian Markdown note copied!', '📓');
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
                    showToast('MCP Agent simulation finished!', '🛠️');
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

                tabs.forEach(t => t.classList.toggle('active', t === tab));
                contents.forEach(c => c.classList.toggle('active', c.id === `tab-code-${codeId}`));
            });
        });

        $$('.btn-copy-code').forEach(btn => {
            btn.addEventListener('click', () => {
                const targetSel = btn.dataset.target;
                const codeEl = targetSel ? $(targetSel) : btn.closest('.code-header')?.nextElementSibling;
                if (codeEl) {
                    navigator.clipboard.writeText(codeEl.innerText);
                    showToast('Code snippet copied!', '📋');
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

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('visible');
                }
            });
        }, { threshold: 0.12 });

        elements.forEach(el => observer.observe(el));
    }

    // =========================================================================
    // Initialization
    // =========================================================================
    document.addEventListener('DOMContentLoaded', () => {
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
    });

})();
