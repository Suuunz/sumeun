// 로그인: 제출 중 스피너 표시.
// 폼 자체는 막지 않는다 — Spring Security(/login)로 실제 인증 요청이 나가야 한다.
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('loginForm');
    if (!form) return;

    form.addEventListener('submit', () => {
        const btn = document.getElementById('loginBtn');
        const text = document.getElementById('btnText');
        const spinner = document.getElementById('btnSpinner');
        // 버튼을 disabled 로 만들면 제출이 취소되는 브라우저가 있어 스타일만 바꾼다
        if (btn) btn.classList.add('opacity-90');
        if (text) text.classList.add('hidden');
        if (spinner) spinner.classList.remove('hidden');
    });
});
