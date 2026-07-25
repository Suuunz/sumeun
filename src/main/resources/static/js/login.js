// 로그인: 스피너 표시 후 온보딩으로 이동
document.getElementById('loginForm').addEventListener('submit', function (e) {
    e.preventDefault();
    const btn = document.getElementById('loginBtn');
    const text = document.getElementById('btnText');
    const spinner = document.getElementById('btnSpinner');

    btn.disabled = true;
    btn.classList.add('opacity-90');
    text.classList.add('hidden');
    spinner.classList.remove('hidden');

    setTimeout(() => {
        // 실제 페이지 이동 (지침 10: alert/console 대신 라우팅)
        window.location.href = '/onboarding';
    }, 900);
});
