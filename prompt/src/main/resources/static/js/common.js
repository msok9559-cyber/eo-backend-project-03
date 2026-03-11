window.addEventListener('DOMContentLoaded', function () {

    //다크모드
    const html       = document.documentElement;
    const darkToggle = document.getElementById('dark-toggle');
    const DARK_KEY   = 'theme';

    if (localStorage.getItem(DARK_KEY) === 'dark') {
        html.setAttribute('data-theme', 'dark');
    }

    if (darkToggle) {
        darkToggle.addEventListener('click', function () {
            const isDark = html.getAttribute('data-theme') === 'dark';
            if (isDark) {
                html.removeAttribute('data-theme');
                localStorage.setItem(DARK_KEY, 'light');
            } else {
                html.setAttribute('data-theme', 'dark');
                localStorage.setItem(DARK_KEY, 'dark');
            }
        });
    }

    // 네비 스크롤 감지
    const nav = document.getElementById('nav');

    if (nav) {
        window.addEventListener('scroll', function () {
            if (window.scrollY > 10) {
                nav.classList.add('scrolled');
            } else {
                nav.classList.remove('scrolled');
            }
        });
    }

});