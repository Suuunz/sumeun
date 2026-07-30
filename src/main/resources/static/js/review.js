// 후기 작성 화면 — 선택한 사진 미리보기 + 제출 중복 방지
(function () {
    'use strict';

    const MAX_FILES = 5;

    function init() {
        const input = document.getElementById('photos');
        const preview = document.getElementById('photo-preview');
        const form = document.getElementById('review-form');
        const submit = document.getElementById('submit-btn');
        if (!input || !preview) return;

        input.addEventListener('change', () => {
            preview.innerHTML = '';
            const files = Array.from(input.files || []).slice(0, MAX_FILES);
            if (files.length === 0) {
                preview.classList.add('hidden');
                return;
            }
            files.forEach((file) => {
                if (!file.type.startsWith('image/')) return;
                const img = document.createElement('img');
                img.className = 'w-full aspect-square object-cover rounded-lg border border-border';
                img.alt = file.name;
                // ObjectURL 은 로드 후 해제(메모리 누수 방지)
                const url = URL.createObjectURL(file);
                img.src = url;
                img.addEventListener('load', () => URL.revokeObjectURL(url), { once: true });
                preview.appendChild(img);
            });
            preview.classList.remove('hidden');
        });

        // 업로드에 시간이 걸리므로 중복 제출을 막는다
        if (form && submit) {
            form.addEventListener('submit', () => {
                submit.disabled = true;
                submit.classList.add('opacity-90');
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
