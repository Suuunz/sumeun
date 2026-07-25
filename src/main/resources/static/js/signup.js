// 회원가입: 간단한 목업 검증 — 3자 초과 입력 시 체크 아이콘 표시
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('input').forEach((input) => {
        input.addEventListener('input', (e) => {
            const val = e.target.value;
            const icon = e.target.parentElement.querySelector('.material-symbols-outlined');
            if (val.length > 3) {
                if (icon) icon.classList.remove('hidden');
            } else {
                if (icon) icon.classList.add('hidden');
            }
        });
    });
});
