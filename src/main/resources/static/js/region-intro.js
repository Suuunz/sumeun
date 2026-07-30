// 지역 상세 "대표적인 랜드마크"의 설명을 채운다.
//
// 관광지 소개글(overview)은 TourAPI 콘텐츠 상세에만 있고 조회에 호출이 든다.
// 페이지 로딩을 그 호출에 묶지 않으려고 렌더 후 비동기로 가져온다.
// (/api/attraction/{id} 가 결과를 DB 에 캐시하므로 두 번째부터는 호출이 없다)
(function () {
    'use strict';

    function init() {
        const items = document.querySelectorAll('#intro-landmarks .landmark');
        if (!items.length) return;

        items.forEach((item) => {
            const desc = item.querySelector('.landmark-desc');
            const id = item.getAttribute('data-id');
            // 서버가 이미 채웠으면 그대로 둔다
            if (!desc || !id || desc.textContent.trim()) return;

            desc.textContent = '소개를 불러오는 중…';
            desc.classList.add('text-text-muted');

            fetch('/api/attraction/' + id)
                .then((res) => (res.ok ? res.json() : null))
                .then((d) => {
                    if (d && d.overview) {
                        desc.textContent = d.overview;
                        desc.classList.remove('text-text-muted');
                        return;
                    }
                    // 소개가 없거나 한도 소진 → 설명 줄을 지운다(빈 자리를 남기지 않는다)
                    desc.remove();
                })
                .catch(() => desc.remove());
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
