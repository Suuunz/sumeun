// 시군구 SVG 지도 초기화
// 흐름: fetch(sig.json) → topojson→GeoJSON → geoMercator.fitSize → <path> 렌더 → 이벤트 바인딩
(function () {
    'use strict';

    const VIEW_W = 800;
    const VIEW_H = 1000;
    const SVG_NS = 'http://www.w3.org/2000/svg';

    async function initKoreaMap() {
        const svg = document.getElementById('korea-map');
        if (!svg) return;

        // 1) TopoJSON 로드
        let topo;
        try {
            const res = await fetch('/geo/sig.json');
            if (!res.ok) throw new Error('HTTP ' + res.status);
            topo = await res.json();
        } catch (err) {
            console.error('[map] sig.json 로드 실패:', err);
            return;
        }

        // 2) TopoJSON → GeoJSON FeatureCollection
        const objectName = Object.keys(topo.objects)[0];
        const fc = topojson.feature(topo, topo.objects[objectName]);

        // 3) 투영: 한국 전체(제주·울릉 포함)를 뷰포트에 맞춤
        const projection = d3.geoMercator().fitSize([VIEW_W, VIEW_H], fc);
        const path = d3.geoPath(projection);

        // 4) 시군구별 <path> 렌더 (한 번만 그림 · 이후 리렌더 없음)
        const frag = document.createDocumentFragment();
        let drawn = 0;
        for (const f of fc.features) {
            const d = path(f);
            if (!d) continue;
            const el = document.createElementNS(SVG_NS, 'path');
            el.setAttribute('d', d);
            el.setAttribute('class', 'sig-path');
            el.setAttribute('data-sig-cd', f.properties.SIG_CD);
            el.setAttribute('data-name', f.properties.SIG_KOR_NM);
            const title = document.createElementNS(SVG_NS, 'title');
            title.textContent = f.properties.SIG_KOR_NM;
            el.appendChild(title);
            frag.appendChild(el);
            drawn++;
        }
        svg.appendChild(frag);
        console.log('[map] 시군구 path 렌더 완료:', drawn);

        // 5) 이벤트: 클릭 시 선택 표시 후 지역 패널로 이동 (호버는 CSS에서 처리)
        svg.addEventListener('click', function (e) {
            const target = e.target.closest ? e.target.closest('.sig-path') : null;
            if (!target) return;
            svg.querySelectorAll('.sig-path.selected').forEach((p) => p.classList.remove('selected'));
            target.classList.add('selected');
            // 선택 색이 잠깐 보이도록 살짝 지연 후 이동 (지도 지역 선택 → /region/panel)
            const sigCd = target.getAttribute('data-sig-cd');
            setTimeout(() => {
                window.location.href = '/region/panel?sig=' + encodeURIComponent(sigCd);
            }, 250);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initKoreaMap);
    } else {
        initKoreaMap();
    }
})();
