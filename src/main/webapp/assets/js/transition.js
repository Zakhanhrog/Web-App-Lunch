document.addEventListener("DOMContentLoaded", function() {
    const preloader = document.getElementById('preloader');
    const transitionOverlay = document.getElementById('page-transition-overlay');
    const body = document.querySelector('body');
    const isHomePage = window.location.pathname === '/';

    function runHomePageAnimation() {
        const counterElement = document.getElementById('preloader-number');
        const progressBar = document.getElementById('preloader-progress');
        if (!preloader || !counterElement || !progressBar) {
            if (transitionOverlay) transitionOverlay.style.display = 'none';
            return;
        }

        body.style.overflow = 'hidden';

        if (transitionOverlay) {
            gsap.to(transitionOverlay, {
                y: '-101%',
                duration: 0.8,
                ease: 'power3.inOut',
            });
        }

        const tl = gsap.timeline({
            delay: 0.8,
            onComplete: () => {
                body.style.overflow = '';
                if (transitionOverlay) transitionOverlay.style.display = 'none';
            }
        });

        tl.to(counterElement, { textContent: 100, duration: 2.5, ease: "power2.out", roundProps: "textContent" })
            .to(progressBar, { width: "100%", duration: 2.5, ease: "power2.out" }, "<")
            .to(preloader, { opacity: 0, duration: 0.75, ease: "power1.inOut", onComplete: () => { if (preloader) preloader.style.display = 'none'; } }, "-=0.5");
    }

    function setupPageExit() {
        document.querySelectorAll('a[data-transition="home"]').forEach(link => {
            link.addEventListener('click', function(event) {
                if (window.location.pathname === '/') {
                    return;
                }
                event.preventDefault();
                const destination = this.href;

                sessionStorage.setItem('isNavigatingHome', 'true');

                if (transitionOverlay) {
                    transitionOverlay.style.display = 'block';
                    gsap.fromTo(transitionOverlay,
                        { y: '-101%' },
                        {
                            y: '0%',
                            duration: 0.8,
                            ease: 'power3.inOut',
                            onComplete: () => {
                                window.location.href = destination;
                            }
                        }
                    );
                } else {
                    window.location.href = destination;
                }
            });
        });
    }

    if (isHomePage) {
        if (sessionStorage.getItem('isNavigatingHome') === 'true') {
            if(transitionOverlay) {
                gsap.set(transitionOverlay, { y: '0%', display: 'block' });
            }
            sessionStorage.removeItem('isNavigatingHome');
        } else {
            if (transitionOverlay) transitionOverlay.style.display = 'none';
        }
        runHomePageAnimation();
    } else {
        if (preloader) preloader.style.display = 'none';
        if (transitionOverlay) transitionOverlay.style.display = 'none';
        body.style.overflow = '';
    }

    setupPageExit();
});