// 후기 공유 — 웹 공유 시트가 있으면 사용하고, 없으면 링크를 클립보드에 복사
(function () {
    'use strict';

    function init() {
        const btn = document.getElementById('share-btn');
        const label = document.getElementById('share-label');
        if (!btn || !label) return;

        const original = label.textContent;

        function done(message) {
            label.textContent = message;
            setTimeout(() => { label.textContent = original; }, 2000);
        }

        btn.addEventListener('click', async () => {
            const url = window.location.href;
            const title = btn.getAttribute('data-share-title') || '여행 후기';

            // 1) 모바일 등 공유 시트 지원 환경
            if (navigator.share) {
                try {
                    await navigator.share({ title: title, url: url });
                    return;
                } catch (e) {
                    // 사용자가 취소한 경우 → 아무것도 하지 않는다
                    return;
                }
            }

            // 2) 클립보드 복사
            try {
                await navigator.clipboard.writeText(url);
                done('링크를 복사했어요');
            } catch (e) {
                // 3) 클립보드 권한이 없으면 주소를 선택 상태로 만들어 직접 복사하게 한다
                done('주소창의 링크를 복사해 주세요');
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
