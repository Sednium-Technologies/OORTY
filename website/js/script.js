// Sednium Oorty — Modern Interactive Website Controller
(function () {
    'use strict';

    // DOM Utilities
    const $ = (sel, ctx = document) => ctx.querySelector(sel);
    const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

    // Dynamic Copyright Year
    const yearEl = $('#currentYear');
    if (yearEl) yearEl.textContent = new Date().getFullYear();

    // =========================================================================
    // 1. Interactive Demo Showcase Tabs
    // =========================================================================
    function initDemoTabs() {
        const tabs = $$('.demo-tab');
        const views = $$('.demo-view');

        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const target = tab.dataset.tab;

                tabs.forEach(t => {
                    t.classList.toggle('active', t === tab);
                    t.setAttribute('aria-selected', t === tab ? 'true' : 'false');
                });

                views.forEach(v => {
                    v.classList.toggle('active', v.id === `view-${target}`);
                });
            });
        });
    }

    // =========================================================================
    // 2. Hardware RAM Compatibility Calculator
    // =========================================================================
    const ramData = {
        '4': {
            name: 'Qwen 2.5 (0.5B-Instruct)',
            quant: 'Q4_K_M • ~450 MB VRAM',
            fitBadge: 'Comfortable 🟢',
            fitClass: 'fit-comfortable',
            desc: 'Ultra-lightweight model ideal for devices with 4GB RAM. Operates without background app pressure and achieves highest tok/s.',
            speed: '~35 tok/s',
            context: '2048 Tokens',
            agentic: '⚡ Limited (Basic)'
        },
        '6': {
            name: 'Llama 3.2 (1B-Instruct)',
            quant: 'Q4_K_M • ~850 MB VRAM',
            fitBadge: 'Comfortable 🟢',
            fitClass: 'fit-comfortable',
            desc: 'Excellent sweet spot for 6GB devices. Delivers 25–35 tok/s decode speed with fast responses and low battery consumption.',
            speed: '~30 tok/s',
            context: '2048 Tokens',
            agentic: '⚡ Limited (Fast)'
        },
        '8': {
            name: 'Gemma 2 (2B-IT)',
            quant: 'Q4_K_M • ~1.6 GB VRAM',
            fitBadge: 'Comfortable 🟢',
            fitClass: 'fit-comfortable',
            desc: 'High reasoning performance with Google’s Gemma 2 architecture. Capable of moderate multi-step tool calls and code generation.',
            speed: '~20 tok/s',
            context: '4096 Tokens',
            agentic: '✅ Moderate'
        },
        '12': {
            name: 'Phi-3-Mini (3.8B-4k) / Llama 3.1 8B',
            quant: 'Q4_K_M • ~2.4 GB – 4.8 GB VRAM',
            fitBadge: 'Full Power 🚀',
            fitClass: 'fit-comfortable',
            desc: 'Full autonomous capability. Supports complex MCP tool chains, deep reasoning, long context recall, and sophisticated coding.',
            speed: '~14–22 tok/s',
            context: '8192 Tokens',
            agentic: '🚀 Full Autonomous'
        }
    };

    function initRamCalculator() {
        const ramBtns = $$('.ram-btn');
        const nameEl = $('#calc-model-name');
        const quantEl = $('#calc-quant');
        const fitBadgeEl = $('#calc-fit-badge');
        const descEl = $('#calc-desc');
        const speedEl = $('#calc-speed');
        const contextEl = $('#calc-context');
        const agenticEl = $('#calc-agentic');

        if (!ramBtns.length || !nameEl) return;

        ramBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                ramBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');

                const ramKey = btn.dataset.ram;
                const data = ramData[ramKey];
                if (!data) return;

                nameEl.textContent = data.name;
                quantEl.textContent = data.quant;
                fitBadgeEl.textContent = data.fitBadge;
                descEl.textContent = data.desc;
                speedEl.textContent = data.speed;
                contextEl.textContent = data.context;
                agenticEl.textContent = data.agentic;
            });
        });
    }

    // =========================================================================
    // 3. Interactive FAQ Search Filter
    // =========================================================================
    function initFaqSearch() {
        const searchInput = $('#faqSearch');
        const faqItems = $$('.faq-item');
        if (!searchInput || !faqItems.length) return;

        searchInput.addEventListener('input', (e) => {
            const query = e.target.value.toLowerCase().trim();
            faqItems.forEach(item => {
                const text = item.textContent.toLowerCase();
                const match = text.includes(query);
                item.style.display = match ? 'block' : 'none';
                if (query && match) {
                    item.setAttribute('open', '');
                }
            });
        });
    }

    // =========================================================================
    // 4. Navbar & Mobile Menu Controller
    // =========================================================================
    function initNavbar() {
        const navbar = $('#navbar');
        const hamburger = $('#hamburger');
        const navLinks = $('#navLinks');

        window.addEventListener('scroll', () => {
            if (navbar) navbar.classList.toggle('scrolled', window.scrollY > 40);
        }, { passive: true });

        if (hamburger && navLinks) {
            hamburger.addEventListener('click', () => {
                const isOpen = navLinks.classList.toggle('active');
                hamburger.classList.toggle('active');
                hamburger.setAttribute('aria-expanded', isOpen);
            });

            $$('.nav-link', navLinks).forEach(link => {
                link.addEventListener('click', () => {
                    navLinks.classList.remove('active');
                    hamburger.classList.remove('active');
                    hamburger.setAttribute('aria-expanded', 'false');
                });
            });
        }
    }

    // Initialize all interactive controllers on DOM load
    document.addEventListener('DOMContentLoaded', () => {
        initDemoTabs();
        initRamCalculator();
        initFaqSearch();
        initNavbar();
    });
})();
