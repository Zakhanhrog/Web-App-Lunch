document.addEventListener("DOMContentLoaded", function() {
    const preloader = document.getElementById('preloader');
    const transitionOverlay = document.getElementById('page-transition-overlay');
    const body = document.querySelector('body');
    const isHomePage = window.location.pathname === '/';

    function runHomePageAnimation() {
        if (!preloader) {
            if (transitionOverlay) transitionOverlay.style.display = 'none';
            return;
        }

        gsap.registerPlugin(SplitText);

        const timeline = gsap.timeline({
            onComplete: () => {
                body.classList.remove('body-no-scroll');
                if (preloader) preloader.style.display = 'none';
            }
        });

        let split = SplitText.create(".preloader-letters", {
            type: "chars",
            charsClass: "char"
        });

        if (transitionOverlay) {
            transitionOverlay.style.display = 'none';
        }

        body.classList.add('body-no-scroll');

        timeline
            .to(".preloader-container", {
                y: "0",
                duration: 1,
                opacity: 1,
                ease: "elastic.out(1,0.3)",
                delay: 0.5
            })
            .to(".preloader-loader-bar", {
                width: "100%",
                duration: 6,
                ease: "power1.inOut"
            }, "-=0.5")
            .to(preloader, {
                y: "-100vh",
                duration: 1.2,
                ease: "power3.inOut"
            }, "-=1.0");

        split.chars.forEach((char) => {
            timeline.fromTo(
                char, {
                    y: "0px",
                    opacity: 0,
                    rotation: 0
                }, {
                    y: "-85px",
                    opacity: 1,
                    rotation: () => gsap.utils.random(-30, 30),
                    yoyo: true,
                    repeat: -1,
                    duration: 0.5,
                    ease: "power2.out"
                },
                0.6 + Math.random() * 0.5
            );
        });

        setTimeout(() => {
            body.classList.remove('body-no-scroll');
        }, 8000);
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
                    body.classList.add('body-no-scroll');
                    transitionOverlay.style.display = 'block';
                    gsap.fromTo(transitionOverlay, {
                        y: '-101%'
                    }, {
                        y: '0%',
                        duration: 0.8,
                        ease: 'power3.inOut',
                        onComplete: () => {
                            window.location.href = destination;
                        }
                    });
                } else {
                    window.location.href = destination;
                }
            });
        });
    }

    if (isHomePage) {
        if (sessionStorage.getItem('isNavigatingHome') === 'true') {
            if (transitionOverlay) {
                gsap.set(transitionOverlay, { y: '0%', display: 'block' });
            }
            if (preloader) {
                preloader.style.display = 'none';
            }
            sessionStorage.removeItem('isNavigatingHome');
            body.classList.add('body-no-scroll');
            gsap.to(transitionOverlay, {
                y: '-101%',
                duration: 1.2,
                ease: 'power3.inOut',
                delay: 0.3,
                onComplete: () => {
                    body.classList.remove('body-no-scroll');
                    if (transitionOverlay) transitionOverlay.style.display = 'none';
                }
            });
        } else {
            if (transitionOverlay) transitionOverlay.style.display = 'none';
            runHomePageAnimation();
        }
    } else {
        if (preloader) preloader.style.display = 'none';
        if (transitionOverlay) transitionOverlay.style.display = 'none';
    }

    setupPageExit();
});