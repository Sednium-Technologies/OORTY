// Oorty — Next-Level Animated Landing Page
// Powered by Anime.js + vanilla JS

(function () {
    'use strict';

    // ============ Utility: touch device detection ============
    const isTouchDevice = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);

    // ============ Dynamic Copyright Year ============
    const yearEl = document.getElementById('currentYear');
    if (yearEl) yearEl.textContent = new Date().getFullYear();

    // ============ Anime.js CDN Loader ============
    const ANIME_CDN = 'https://cdnjs.cloudflare.com/ajax/libs/animejs/3.2.2/anime.min.js';

    function loadScript(src) {
        return new Promise((resolve, reject) => {
            const s = document.createElement('script');
            s.src = src;
            s.onload = resolve;
            s.onerror = reject;
            document.head.appendChild(s);
        });
    }

    // ============ DOM Cache ============
    const $ = (sel, ctx = document) => ctx.querySelector(sel);
    const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

    // ============ Navbar Scroll Effect ============
    function handleScroll() {
        const navbar = $('#navbar');
        if (navbar) navbar.classList.toggle('scrolled', window.scrollY > 50);
    }

    window.addEventListener('scroll', handleScroll, { passive: true });

    // ============ Mobile Menu ============
    function initMobileMenu() {
        const hamburger = $('#hamburger');
        const navLinks = $('#navLinks');
        if (!hamburger || !navLinks) return;

        hamburger.addEventListener('click', () => {
            const isActive = navLinks.classList.toggle('active');
            hamburger.classList.toggle('active');
            hamburger.setAttribute('aria-expanded', isActive);
            document.body.style.overflow = isActive ? 'hidden' : '';
        });

        $$('.nav-links a').forEach(link => {
            link.addEventListener('click', () => {
                navLinks.classList.remove('active');
                hamburger.classList.remove('active');
                hamburger.setAttribute('aria-expanded', 'false');
                document.body.style.overflow = '';
            });
        });
    }

    // ============ Smooth Scroll ============
    function initSmoothScroll() {
        $$('a[href^="#"]').forEach(anchor => {
            anchor.addEventListener('click', function (e) {
                const href = this.getAttribute('href');
                if (href === '#') return;
                const target = $(href);
                if (target) {
                    e.preventDefault();
                    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            });
        });
    }

    // ============ Contact Form ============
    function initContactForm() {
        const contactForm = $('#contactForm');
        if (contactForm) {
            contactForm.addEventListener('submit', function (e) {
                e.preventDefault();

                // Basic client-side validation
                const name = this.querySelector('#name');
                const email = this.querySelector('#email');
                const message = this.querySelector('#message');

                if (!name.value.trim() || !email.value.trim() || !message.value.trim()) {
                    showToast('Please fill in all required fields.');
                    return;
                }

                const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailRegex.test(email.value.trim())) {
                    showToast('Please enter a valid email address.');
                    return;
                }

                showToast('Thank you! Your message has been received. We will get back to you soon.');
                this.reset();
            });
        }
    }

    function showToast(message) {
        const toast = $('#toast');
        if (!toast) return;
        toast.textContent = message;
        toast.classList.add('show');
        setTimeout(() => toast.classList.remove('show'), 4000);
    }

    // ============ Modal Functions ============
    window.showTerms = e => { e.preventDefault(); openModal('termsModal'); };
    window.showPrivacy = e => { e.preventDefault(); openModal('privacyModal'); };
    window.closeModal = id => closeModal(id);

    function openModal(id) {
        const modal = document.getElementById(id);
        if (!modal) return;
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
        // Focus first close button for accessibility
        const closeBtn = modal.querySelector('.modal-close');
        if (closeBtn) closeBtn.focus();

        if (window.anime) {
            anime({
                targets: modal.querySelector('.modal-content'),
                scale: [0.9, 1],
                opacity: [0, 1],
                duration: 350,
                easing: 'easeOutCubic'
            });
        }
    }

    function closeModal(id) {
        const modal = document.getElementById(id);
        if (!modal) return;
        const content = modal.querySelector('.modal-content');

        if (window.anime) {
            anime({
                targets: content,
                scale: [1, 0.9],
                opacity: [1, 0],
                duration: 250,
                easing: 'easeInCubic',
                complete: () => {
                    modal.classList.remove('active');
                    document.body.style.overflow = '';
                    content.style.opacity = '';
                    content.style.transform = '';
                }
            });
        } else {
            modal.classList.remove('active');
            document.body.style.overflow = '';
        }
    }

    function initModals() {
        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') {
                $$('.modal.active').forEach(m => closeModal(m.id));
            }
        });

        $$('.modal').forEach(modal => {
            modal.addEventListener('click', function (e) {
                if (e.target === this) closeModal(this.id);
            });
        });
    }

    // ============ Boot ============
    function boot() {
        loadScript(ANIME_CDN).then(() => {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', init);
            } else {
                init();
            }
        }).catch(() => {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', initBasic);
            } else {
                initBasic();
            }
        });
    }

    // Start as soon as DOM is ready (script is at end of body)
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }

    // ============================================
    //  BASIC FALLBACK (no anime.js)
    // ============================================
    function initBasic() {
        document.body.classList.add('no-anime');
        handleScroll();
        initMobileMenu();
        initSmoothScroll();
        initContactForm();
        initModals();
        $$('.hero .fade-in').forEach(el => el.classList.add('visible'));
        $$('.fade-in').forEach(el => {
            const obs = new IntersectionObserver(entries => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('visible');
                        obs.unobserve(entry.target);
                    }
                });
            }, { threshold: 0.1 });
            obs.observe(el);
        });
    }

    // ============================================
    //  FULL ANIME.JS INITIALIZATION
    // ============================================
    function init() {
        handleScroll();
        initMobileMenu();
        initSmoothScroll();
        initContactForm();
        initModals();

        initHeroEntrance();
        initChatTyping();
        initScrollReveal();
        initProviderMarquee();

        // Skip 3D tilt and magnetic buttons on touch devices
        if (!isTouchDevice) {
            initTiltCards();
            initMagneticButtons();
        }

        initParallax();
        initFaqAnime();
        initParticles();
        initStatsCounter();
        initModeCardsEntrance();
    }

    // ============ 1. HERO CINEMATIC ENTRANCE ============
    function initHeroEntrance() {
        // Prepare title for split-text animation
        const heroTitle = $('.hero-title');
        if (!heroTitle) return;

        // Set parent to visible so children stagger correctly
        heroTitle.style.opacity = '1';
        heroTitle.style.transform = 'none';

        // Statically structured split text for the specific title
        heroTitle.innerHTML = `
            <span class="hero-word highlight">Sednium</span>
            <span class="hero-word highlight">Oorty.</span><br>
            <span class="hero-word">One</span>
            <span class="hero-word">Interface.</span>
            <span class="hero-word">Every</span>
            <span class="hero-word">AI</span>
            <span class="hero-word">Model.</span>
        `;

        // Set initial states
        $$('.hero-word').forEach(w => { w.style.opacity = '0'; w.style.transform = 'translateY(40px)'; });
        const subtitle = $('.hero-subtitle');
        const actions = $('.hero-actions');
        const stats = $$('.stat');
        const heroVisual = $('.hero-visual');

        if (subtitle) { subtitle.style.opacity = '0'; subtitle.style.transform = 'translateY(30px)'; }
        if (actions) { actions.style.opacity = '0'; actions.style.transform = 'translateY(30px)'; }
        const heroStats = $('.hero-stats');
        if (heroStats) { heroStats.style.opacity = '1'; heroStats.style.transform = 'none'; }
        stats.forEach(s => { s.style.opacity = '0'; s.style.transform = 'translateY(20px)'; });
        if (heroVisual) { heroVisual.style.opacity = '0'; heroVisual.style.transform = 'translateY(60px) scale(0.95)'; }

        // Build the timeline
        const tl = anime.timeline({
            easing: 'easeOutExpo',
            duration: 1000
        });

        tl
            .add({
                targets: '.hero-word',
                opacity: [0, 1],
                translateY: [40, 0],
                delay: anime.stagger(80),
                duration: 1200
            })
            .add({
                targets: subtitle,
                opacity: [0, 1],
                translateY: [30, 0],
                duration: 900
            }, '-=600')
            .add({
                targets: actions,
                opacity: [0, 1],
                translateY: [30, 0],
                duration: 800
            }, '-=500')
            .add({
                targets: stats,
                opacity: [0, 1],
                translateY: [20, 0],
                delay: anime.stagger(100),
                duration: 700
            }, '-=400')
            .add({
                targets: heroVisual,
                opacity: [0, 1],
                translateY: [60, 0],
                scale: [0.95, 1],
                duration: 1200,
                easing: 'easeOutBack'
            }, '-=700');

        // After entrance: perpetual float on chat demo (skip on touch to save perf)
        if (!isTouchDevice) {
            tl.finished.then(() => {
                anime({
                    targets: '.chat-demo',
                    translateY: [-8, 8],
                    duration: 3000,
                    easing: 'easeInOutSine',
                    direction: 'alternate',
                    loop: true
                });
            });
        }
    }

    // ============ 2. CHAT TYPING ANIMATION ============
    function initChatTyping() {
        const chatMessages = $$('.chat-message');
        chatMessages.forEach(m => { m.style.opacity = '0'; m.style.transform = 'translateY(20px)'; });

        const chatBubbles = $$('.chat-bubble');
        // Store original text
        chatBubbles.forEach(b => {
            b.dataset.fullText = b.textContent;
            if (!b.classList.contains('code')) {
                b.textContent = '';
            }
        });

        // Start after hero entrance
        setTimeout(() => {
            chatMessages.forEach((msg, i) => {
                setTimeout(() => {
                    // Animate message in
                    anime({
                        targets: msg,
                        opacity: [0, 1],
                        translateY: [20, 0],
                        duration: 500,
                        easing: 'easeOutCubic',
                        complete: () => {
                            // Typewriter for non-code bubbles
                            const bubble = msg.querySelector('.chat-bubble:not(.code)');
                            if (bubble && bubble.dataset.fullText) {
                                typeText(bubble, bubble.dataset.fullText, 20);
                            }
                        }
                    });
                }, i * 1200);
            });
        }, 2400);
    }

    function typeText(el, text, speed) {
        let i = 0;
        el.textContent = '';
        const interval = setInterval(() => {
            el.textContent += text[i];
            i++;
            if (i >= text.length) clearInterval(interval);
        }, speed);
    }

    // ============ 3. SCROLL-REVEAL SECTIONS ============
    function initScrollReveal() {
        const targets = $$('.fade-in');
        const sectionTargets = targets.filter(el => !el.closest('.hero'));

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const el = entry.target;
                    observer.unobserve(el);

                    // Check if it's a grid parent or single element
                    const isGrid = el.classList.contains('features-grid') ||
                        el.classList.contains('modes-grid') ||
                        el.classList.contains('local-ai-grid') ||
                        el.classList.contains('testimonials-grid') ||
                        el.classList.contains('steps') ||
                        el.classList.contains('faq-list');

                    if (isGrid) {
                        // Animate parent container to visible state
                        anime({
                            targets: el,
                            opacity: [0, 1],
                            translateY: [30, 0],
                            duration: 400,
                            easing: 'easeOutCubic'
                        });

                        // Set children invisible first
                        Array.from(el.children).forEach(child => {
                            child.style.opacity = '0';
                            child.style.transform = 'translateY(40px)';
                        });
                        anime({
                            targets: el.children,
                            opacity: [0, 1],
                            translateY: [40, 0],
                            delay: anime.stagger(80),
                            duration: 800,
                            easing: 'easeOutCubic'
                        });
                    } else if (el.classList.contains('section-header')) {
                        el.style.opacity = '0';
                        anime({
                            targets: el,
                            opacity: [0, 1],
                            translateY: [30, 0],
                            duration: 900,
                            easing: 'easeOutCubic'
                        });
                    } else {
                        el.style.opacity = '0';
                        anime({
                            targets: el,
                            opacity: [0, 1],
                            translateY: [40, 0],
                            duration: 800,
                            easing: 'easeOutCubic',
                            begin: () => { el.style.opacity = '0'; }
                        });
                    }
                }
            });
        }, { threshold: 0.05, rootMargin: '0px 0px -30px 0px' });

        sectionTargets.forEach(el => observer.observe(el));
    }

    // ============ 4. PROVIDER PILLS MARQUEE ============
    function initProviderMarquee() {
        const grid = $('.providers-grid');
        if (!grid) return;

        const items = $$('.provider-item', grid);
        if (items.length === 0) return;

        // Duplicate items for seamless loop
        const clone = grid.innerHTML;
        grid.innerHTML += clone;

        // Hide duplicated items from screen readers
        const allItems = $$('.provider-item', grid);
        allItems.forEach((item, i) => {
            if (i >= items.length) item.setAttribute('aria-hidden', 'true');
        });

        // Set up horizontal scrolling
        grid.style.display = 'flex';
        grid.style.flexWrap = 'nowrap';
        grid.style.gap = '20px';
        grid.style.width = 'max-content';

        const totalWidth = grid.scrollWidth / 2;

        const marqueeAnim = anime({
            targets: grid,
            translateX: [0, -totalWidth],
            duration: 30000,
            easing: 'linear',
            loop: true
        });

        // Hover: pause animation smoothly + highlight hovered item
        grid.addEventListener('mouseenter', () => {
            marqueeAnim.pause();
        });

        grid.addEventListener('mouseleave', () => {
            marqueeAnim.play();
            // Remove any active highlight on leave
            $$('.provider-item.highlighted', grid).forEach(el => el.classList.remove('highlighted'));
        });

        // Highlight provider under the cursor
        $$('.provider-item', grid).forEach(item => {
            item.addEventListener('mouseenter', () => {
                item.classList.add('highlighted');
            });
            item.addEventListener('mouseleave', () => {
                item.classList.remove('highlighted');
            });
        });
    }

    // ============ 5. 3D TILT EFFECT ON CARDS ============
    function initTiltCards() {
        if (isTouchDevice) return;

        const cards = $$('.feature-card, .local-ai-card, .testimonial-card, .mode-card');

        cards.forEach(card => {
            card.style.transformStyle = 'preserve-3d';
            card.style.willChange = 'transform';

            card.addEventListener('mousemove', (e) => {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                const centerX = rect.width / 2;
                const centerY = rect.height / 2;

                const rotateX = ((y - centerY) / centerY) * -8;
                const rotateY = ((x - centerX) / centerX) * 8;

                anime({
                    targets: card,
                    rotateX: rotateX,
                    rotateY: rotateY,
                    duration: 400,
                    easing: 'easeOutQuad'
                });
            });

            card.addEventListener('mouseleave', () => {
                anime({
                    targets: card,
                    rotateX: 0,
                    rotateY: 0,
                    duration: 600,
                    easing: 'easeOutElastic(1, .6)'
                });
            });
        });
    }

    // ============ 6. MAGNETIC BUTTONS ============
    function initMagneticButtons() {
        if (isTouchDevice) return;

        const btns = $$('.btn-primary, .btn-outline');

        btns.forEach(btn => {
            btn.addEventListener('mousemove', (e) => {
                const rect = btn.getBoundingClientRect();
                const x = e.clientX - rect.left - rect.width / 2;
                const y = e.clientY - rect.top - rect.height / 2;

                anime({
                    targets: btn,
                    translateX: x * 0.3,
                    translateY: y * 0.3,
                    duration: 300,
                    easing: 'easeOutQuad'
                });
            });

            btn.addEventListener('mouseleave', () => {
                anime({
                    targets: btn,
                    translateX: 0,
                    translateY: 0,
                    duration: 600,
                    easing: 'easeOutElastic(1, .6)'
                });
            });
        });
    }

    // ============ 7. PARALLAX HERO VISUAL ============
    function initParallax() {
        if (isTouchDevice) return;

        const heroVisual = $('.hero-visual');
        const heroContent = $('.hero-content');
        if (!heroVisual || !heroContent) return;

        let ticking = false;

        window.addEventListener('scroll', () => {
            if (!ticking) {
                requestAnimationFrame(() => {
                    const scrollY = window.scrollY;
                    if (scrollY < window.innerHeight) {
                        anime.set(heroVisual, {
                            translateY: scrollY * 0.15,
                            scale: 1 - scrollY * 0.0002
                        });
                        anime.set(heroContent, {
                            translateY: scrollY * 0.08,
                            opacity: 1 - scrollY * 0.001
                        });
                    }
                    ticking = false;
                });
                ticking = true;
            }
        }, { passive: true });
    }

    // ============ 8. FAQ ANIME EXPAND ============
    function initFaqAnime() {
        $$('.faq-question').forEach(button => {
            button.addEventListener('click', function () {
                const item = this.parentElement;
                const answer = item.querySelector('.faq-answer');
                const wasActive = item.classList.contains('active');

                // Close all
                $$('.faq-item.active').forEach(faq => {
                    faq.classList.remove('active');
                    const btn = faq.querySelector('.faq-question');
                    if (btn) btn.setAttribute('aria-expanded', 'false');
                    const ans = faq.querySelector('.faq-answer');
                    anime({
                        targets: ans,
                        maxHeight: [ans.scrollHeight + 'px', '0px'],
                        paddingBottom: ['22px', '0px'],
                        duration: 400,
                        easing: 'easeInCubic',
                        complete: () => {
                            ans.style.maxHeight = '';
                            ans.style.paddingBottom = '';
                        }
                    });
                    anime({
                        targets: faq.querySelector('.faq-icon'),
                        rotate: [45, 0],
                        duration: 400,
                        easing: 'easeInCubic'
                    });
                });

                if (!wasActive) {
                    item.classList.add('active');
                    this.setAttribute('aria-expanded', 'true');
                    anime({
                        targets: answer,
                        maxHeight: ['0px', answer.scrollHeight + 40 + 'px'],
                        paddingBottom: ['0px', '22px'],
                        opacity: [0, 1],
                        duration: 500,
                        easing: 'easeOutCubic',
                        complete: () => {
                            answer.style.maxHeight = '';
                            answer.style.paddingBottom = '';
                        }
                    });
                    anime({
                        targets: item.querySelector('.faq-icon'),
                        rotate: [0, 45],
                        duration: 400,
                        easing: 'easeOutCubic'
                    });
                }
            });
        });
    }

    // ============ 9. PARTICLES (Enhanced with Anime.js) ============
    function initParticles() {
        const container = $('#particles');
        if (!container) return;

        const count = window.innerWidth > 768 ? 25 : 8;

        for (let i = 0; i < count; i++) {
            const p = document.createElement('div');
            p.className = 'particle';
            const size = Math.random() * 4 + 2;
            Object.assign(p.style, {
                position: 'absolute',
                width: size + 'px',
                height: size + 'px',
                background: `rgba(236, 94, 39, ${Math.random() * 0.12 + 0.04})`,
                borderRadius: '50%',
                left: Math.random() * 100 + '%',
                top: Math.random() * 100 + '%',
                pointerEvents: 'none'
            });
            container.appendChild(p);

            // Animate each particle with random path
            anime({
                targets: p,
                translateY: () => anime.random(-300, 300),
                translateX: () => anime.random(-200, 200),
                scale: () => [1, anime.random(5, 15) / 10],
                opacity: [
                    { value: 0.7, duration: anime.random(2000, 4000) },
                    { value: 0, duration: anime.random(2000, 4000) }
                ],
                duration: () => anime.random(8000, 16000),
                delay: () => anime.random(0, 4000),
                easing: 'easeInOutSine',
                loop: true,
                direction: 'alternate'
            });
        }
    }

    // ============ 10. STATS COUNTER ANIMATION ============
    function initStatsCounter() {
        const statNumbers = $$('.stat-number');
        const observed = new Set();

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting && !observed.has(entry.target)) {
                    observed.add(entry.target);
                    const el = entry.target;
                    const text = el.textContent;
                    const match = text.match(/^(\d+)/);
                    if (match) {
                        const target = parseInt(match[1]);
                        const suffix = text.replace(/^\d+/, '');
                        const obj = { val: 0 };

                        anime({
                            targets: obj,
                            val: target,
                            round: 1,
                            duration: 2000,
                            easing: 'easeOutExpo',
                            update: () => {
                                el.textContent = obj.val + suffix;
                            }
                        });
                    }
                }
            });
        }, { threshold: 0.5 });

        statNumbers.forEach(s => observer.observe(s));
    }

    // ============ 11. MODE CARDS ENTRANCE ============
    function initModeCardsEntrance() {
        const modeCards = $$('.mode-card');
        modeCards.forEach(card => {
            const icon = card.querySelector('.mode-icon');
            if (!icon) return;

            // Subtle pulse on the icon (skip on touch to save battery)
            if (!isTouchDevice) {
                anime({
                    targets: icon,
                    scale: [1, 1.06, 1],
                    duration: 3000,
                    easing: 'easeInOutSine',
                    loop: true,
                    delay: anime.random(0, 1500)
                });
            }
        });
    }

})();
