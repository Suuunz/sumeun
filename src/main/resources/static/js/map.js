// 시군구 SVG 지도 + 지역 패널
// 흐름: 지도 렌더 → 시군구 클릭/검색 선택 → fetch(/api/regions/{sigCd}) → 우측 패널 채우고 슬라이드 인
(function () {
    'use strict';

    const VIEW_W = 800;
    const VIEW_H = 1000;
    const SVG_NS = 'http://www.w3.org/2000/svg';

    let svg, panel, panelBody;

    /* ---------- 유틸 ---------- */
    function el(tag, cls, text) {
        const n = document.createElement(tag);
        if (cls) n.className = cls;
        if (text != null) n.textContent = text;
        return n;
    }
    function icon(name, cls) {
        const s = el('span', 'material-symbols-outlined' + (cls ? ' ' + cls : ''), name);
        return s;
    }
    // 애니메이션 클래스 재적용(재생 리셋)
    function retrigger(node, cls) {
        node.classList.remove(cls);
        void node.offsetWidth;
        node.classList.add(cls);
    }
    function setShow(node, show) {
        node.style.display = show ? '' : 'none';
    }

    /* ---------- 지도 렌더 ---------- */
    async function renderMap() {
        let topo;
        try {
            const res = await fetch('/geo/sig.json');
            if (!res.ok) throw new Error('HTTP ' + res.status);
            topo = await res.json();
        } catch (err) {
            console.error('[map] sig.json 로드 실패:', err);
            return;
        }
        const objectName = Object.keys(topo.objects)[0];
        const fc = topojson.feature(topo, topo.objects[objectName]);
        const projection = d3.geoMercator().fitSize([VIEW_W, VIEW_H], fc);
        const path = d3.geoPath(projection);

        const frag = document.createDocumentFragment();
        let drawn = 0;
        for (const f of fc.features) {
            const d = path(f);
            if (!d) continue;
            const p = document.createElementNS(SVG_NS, 'path');
            p.setAttribute('d', d);
            p.setAttribute('class', 'sig-path');
            p.setAttribute('data-sig-cd', f.properties.SIG_CD);
            p.setAttribute('data-name', f.properties.SIG_KOR_NM);
            const title = document.createElementNS(SVG_NS, 'title');
            title.textContent = f.properties.SIG_KOR_NM;
            p.appendChild(title);
            frag.appendChild(p);
            drawn++;
        }
        svg.appendChild(frag);
        console.log('[map] 시군구 path 렌더 완료:', drawn);

        // 지도 클릭 → 해당 시군구 선택
        svg.addEventListener('click', function (e) {
            const t = e.target.closest ? e.target.closest('.sig-path') : null;
            if (!t) return;
            selectRegion(t.getAttribute('data-sig-cd'));
        });
    }

    /* ---------- 지역 선택 → 패널 ---------- */
    async function selectRegion(sigCd) {
        if (!sigCd) return;
        // 지도 경로 하이라이트
        svg.querySelectorAll('.sig-path.selected').forEach((p) => p.classList.remove('selected'));
        const target = svg.querySelector('.sig-path[data-sig-cd="' + sigCd + '"]');
        if (target) target.classList.add('selected');

        try {
            const res = await fetch('/api/regions/' + encodeURIComponent(sigCd));
            if (!res.ok) throw new Error('HTTP ' + res.status);
            const data = await res.json();
            fillPanel(data, sigCd);
            openPanel();
        } catch (err) {
            console.error('[map] 지역 정보 로드 실패:', err);
        }
    }

    function fillPanel(data, sigCd) {
        document.getElementById('panel-name').textContent = data.name || '';
        document.getElementById('panel-province').textContent = data.province || '';
        document.getElementById('panel-ai').textContent = data.aiSummary || '';

        // 특산물 칩
        const spWrap = document.getElementById('panel-specialties-wrap');
        const sp = document.getElementById('panel-specialties');
        sp.innerHTML = '';
        const specialties = data.specialties || [];
        specialties.forEach((s) => {
            sp.appendChild(el('span',
                'px-4 py-2 bg-surface-alt border border-border rounded font-body-main text-caption text-text-primary', s));
        });
        setShow(spWrap, specialties.length > 0);

        // 착한가격업소 카드
        const shopWrap = document.getElementById('panel-shops-wrap');
        const shops = document.getElementById('panel-shops');
        shops.innerHTML = '';
        (data.shops || []).forEach((shop) => {
            const card = el('div', 'p-4 bg-surface border border-border rounded group hover:-translate-y-0.5 hover:border-primary-container transition-all duration-300');
            const top = el('div', 'flex justify-between items-start mb-2');
            top.appendChild(el('h4', 'font-section-title text-card-title text-text-primary', shop.name));
            top.appendChild(el('span', 'badge-sage', '착한가격업소'));
            const bottom = el('div', 'flex justify-between items-center mt-4');
            bottom.appendChild(el('span', 'font-body-main text-caption text-text-muted', shop.menu));
            bottom.appendChild(el('span', 'font-section-title text-body-main text-primary', shop.price));
            card.appendChild(top);
            card.appendChild(bottom);
            shops.appendChild(card);
        });
        setShow(shopWrap, (data.shops || []).length > 0);

        // 추천 반일 코스 타임라인
        const courseWrap = document.getElementById('panel-course-wrap');
        const course = document.getElementById('panel-course');
        course.innerHTML = '';
        const points = data.briefCourse || [];
        if (points.length > 0) {
            const line = el('div', 'absolute left-[11px] top-2 bottom-6 w-[1px] border-l border-dotted border-outline-variant');
            course.appendChild(line);
            points.forEach((pt) => {
                const item = el('div', 'relative mb-8 last:mb-0 group');
                const badge = el('div', 'absolute -left-6 top-0 w-6 h-6 rounded-full bg-surface border border-primary flex items-center justify-center z-10 font-section-title text-xs text-primary', String(pt.order));
                const head = el('div', 'flex items-center gap-2 mb-1');
                head.appendChild(el('h4', 'font-section-title text-card-title text-text-primary', pt.name));
                head.appendChild(el('span', 'text-[11px] bg-surface-alt px-2 py-0.5 rounded border border-border text-text-muted', pt.type));
                item.appendChild(badge);
                item.appendChild(head);
                item.appendChild(el('p', 'font-body-main text-caption text-text-muted', pt.desc));
                course.appendChild(item);
            });
        }
        setShow(courseWrap, points.length > 0);

        // [이 지역으로 여행가기] → 지역 상세
        document.getElementById('panel-go').setAttribute('href', '/region?sigCd=' + encodeURIComponent(sigCd));
    }

    function openPanel() {
        panel.classList.add('open');
        retrigger(panel, 'slide-in-right');
        retrigger(panelBody, 'stagger-fade-in');
    }
    function closePanel() {
        panel.classList.remove('open');
        svg.querySelectorAll('.sig-path.selected').forEach((p) => p.classList.remove('selected'));
    }

    /* ---------- 검색 자동완성 ---------- */
    function initSearch() {
        const box = document.getElementById('region-search');
        const input = document.getElementById('region-search-input');
        const list = document.getElementById('region-search-list');
        if (!box || !input || !list) return;
        const items = Array.from(list.querySelectorAll('.region-opt'));

        function filter() {
            const q = input.value.trim().toLowerCase();
            items.forEach((li) => {
                const name = (li.getAttribute('data-name') || '').toLowerCase();
                setShow(li, name.includes(q));
            });
        }
        input.addEventListener('focus', () => { list.classList.remove('hidden'); filter(); });
        input.addEventListener('input', () => { list.classList.remove('hidden'); filter(); });
        list.addEventListener('click', (e) => {
            const li = e.target.closest('.region-opt');
            if (!li) return;
            input.value = li.getAttribute('data-name') || '';
            list.classList.add('hidden');
            selectRegion(li.getAttribute('data-sig-cd'));
        });
        document.addEventListener('click', (e) => {
            if (!box.contains(e.target)) list.classList.add('hidden');
        });
        return items;
    }

    /* ---------- 초기화 ---------- */
    async function init() {
        svg = document.getElementById('korea-map');
        panel = document.getElementById('region-panel');
        panelBody = document.getElementById('panel-body');
        if (!svg || !panel) return;

        const options = initSearch();
        await renderMap();

        // 닫기
        const closeBtn = document.getElementById('panel-close');
        if (closeBtn) closeBtn.addEventListener('click', closePanel);

        // 무작위로 한 곳 보기 (자동완성 옵션 중 랜덤)
        const shuffle = document.getElementById('map-shuffle');
        if (shuffle && options && options.length) {
            shuffle.addEventListener('click', () => {
                const idx = Math.floor(Math.random() * options.length);
                selectRegion(options[idx].getAttribute('data-sig-cd'));
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
