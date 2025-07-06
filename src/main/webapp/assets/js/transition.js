document.addEventListener("DOMContentLoaded", function() {
    const preloader = document.getElementById('preloader');
    const transitionOverlay = document.getElementById('page-transition-overlay');
    const body = document.querySelector('body');

    // Hàm chạy animation vào trang chủ
    function runHomePageAnimation() {
        const counterElement = document.getElementById('preloader-number');
        const progressBar = document.getElementById('preloader-progress');
        if (!preloader || !counterElement || !progressBar) {
            if (transitionOverlay) transitionOverlay.style.display = 'none';
            return;
        }

        body.style.overflow = 'hidden';

        // Luôn trượt màn lên để lộ preloader
        gsap.to(transitionOverlay, {
            y: '-100%',
            duration: 0.8,
            ease: 'power3.inOut',
            onStart: () => {
                preloader.style.opacity = 1;
                preloader.style.display = 'flex';
            }
        });

        const tl = gsap.timeline({
            delay: 0.7, // Đợi màn trượt lên gần xong
            onComplete: () => {
                body.style.overflow = '';
            }
        });

        tl.to(counterElement, { textContent: 100, duration: 2.5, ease: "power2.out", roundProps: "textContent" })
            .to(progressBar, { width: "100%", duration: 2.5, ease: "power2.out" }, "<")
            .to(preloader, { opacity: 0, duration: 0.75, ease: "power1.inOut", onComplete: () => { preloader.style.display = 'none'; } }, "-=0.5");
    }

    // Hàm xử lý khi click link để về trang chủ
    function setupPageExit() {
        // Chỉ tìm các link được đánh dấu đặc biệt
        document.querySelectorAll('a[data-transition="home"]').forEach(link => {
            link.addEventListener('click', function(event) {
                // Nếu đang ở trang chủ rồi thì không làm gì cả
                if (window.location.pathname === '/') {
                    return;
                }
                event.preventDefault();
                const destination = this.href;

                body.style.overflow = 'hidden';
                transitionOverlay.style.display = 'block';
                // Reset vị trí của lớp phủ về phía trên trước khi chạy animation
                gsap.set(transitionOverlay, { y: '-100%' });

                // Chạy animation trượt xuống
                gsap.to(transitionOverlay, {
                    y: '0%',
                    duration: 0.8,
                    ease: 'power3.inOut',
                    onComplete: () => {
                        window.location.href = destination;
                    }
                });
            });
        });
    }

    // Logic chính: Kiểm tra URL để quyết định chạy animation nào
    if (window.location.pathname === '/') {
        runHomePageAnimation();
    } else {
        // Nếu không phải trang chủ, ẩn hết các lớp phủ ngay lập tức
        if (preloader) preloader.style.display = 'none';
        if (transitionOverlay) transitionOverlay.style.display = 'none';
        body.style.overflow = '';
    }

    // Luôn gắn sự kiện click cho các link đặc biệt trên mọi trang
    setupPageExit();
});