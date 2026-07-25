// 시군구 SVG 지도 + 지역 패널
// 흐름: 로딩 → 지도 렌더 → 호버/클릭/키보드/검색/무작위 → fetch(/api/regions) → 패널 슬라이드 인
(function () {
    'use strict';

    const VIEW_W = 800;
    const VIEW_H = 1000;
    const FULL_VIEWBOX = [0, 0, VIEW_W, VIEW_H];
    const SVG_NS = 'http://www.w3.org/2000/svg';
    const ZOOM_MS = 350;

    let svg, panel, panelBody;
    const boundsByCode = {};   // SIG_CD → [[x0,y0],[x1,y1]]
    let vbAnim = null;         // 진행 중인 viewBox 애니메이션 취소용

    /* ---------- 유틸 ---------- */
    function el(tag, cls, text) {
        const n = document.createElement(tag);
        if (cls) n.className = cls;
        if (text != null) n.textContent = text;
        return n;
    }
    function retrigger(node, cls) {
        node.classList.remove(cls);
        void node.offsetWidth;
        node.classList.add(cls);
    }
    function setShow(node, show) {
        node.style.display = show ? '' : 'none';
    }
    const easeInOut = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

    /* ---------- 지도 렌더 ---------- */
    async function renderMap() {
        const loading = document.getElementById('map-loading');
        let topo;
        try {
            const res = await fetch('/geo/sig.json');
            if (!res.ok) throw new Error('HTTP ' + res.status);
            topo = await res.json();
        } catch (err) {
            console.error('[map] sig.json 로드 실패:', err);
            if (loading) loading.querySelector('span').textContent = '지도를 불러오지 못했어요.';
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
            const sigCd = f.properties.SIG_CD;
            const name = f.properties.SIG_KOR_NM;
            boundsByCode[sigCd] = path.bounds(f);

            const p = document.createElementNS(SVG_NS, 'path');
            p.setAttribute('d', d);
            p.setAttribute('class', 'sig-path');
            p.setAttribute('data-sig-cd', sigCd);
            p.setAttribute('data-name', name);
            // 접근성
            p.setAttribute('role', 'button');
            p.setAttribute('tabindex', '0');
            p.setAttribute('aria-label', name);
            const title = document.createElementNS(SVG_NS, 'title');
            title.textContent = name;
            p.appendChild(title);
            frag.appendChild(p);
            drawn++;
        }
        svg.appendChild(frag);
        console.log('[map] 시군구 path 렌더 완료:', drawn);
        if (loading) setShow(loading, false);

        // 마우스 클릭
        svg.addEventListener('click', (e) => {
            const t = e.target.closest ? e.target.closest('.sig-path') : null;
            if (t) selectRegion(t.getAttribute('data-sig-cd'));
        });
        // 키보드(엔터/스페이스)로 선택
        svg.addEventListener('keydown', (e) => {
            if (e.key !== 'Enter' && e.key !== ' ' && e.key !== 'Spacebar') return;
            const t = e.target.closest ? e.target.closest('.sig-path') : null;
            if (t) {
                e.preventDefault();
                selectRegion(t.getAttribute('data-sig-cd'));
            }
        });
    }

    /* ---------- viewBox 줌/팬 (부드럽게) ---------- */
    function animateViewBox(target) {
        const start = svg.getAttribute('viewBox').split(/\s+/).map(Number);
        if (vbAnim) cancelAnimationFrame(vbAnim);
        const t0 = performance.now();
        function step(now) {
            const p = Math.min(1, (now - t0) / ZOOM_MS);
            const k = easeInOut(p);
            const cur = start.map((s, i) => s + (target[i] - s) * k);
            svg.setAttribute('viewBox', cur.join(' '));
            if (p < 1) vbAnim = requestAnimationFrame(step);
        }
        vbAnim = requestAnimationFrame(step);
    }
    function zoomToRegion(sigCd) {
        const b = boundsByCode[sigCd];
        if (!b) return;
        const cx = (b[0][0] + b[1][0]) / 2;
        const cy = (b[0][1] + b[1][1]) / 2;
        const w = b[1][0] - b[0][0];
        const h = b[1][1] - b[0][1];
        // 지역을 살짝 여유있게 담되 과도한 확대는 제한
        let size = Math.max(w, h) * 2.4;
        size = Math.max(140, Math.min(size, VIEW_H));
        const boxH = size;
        const boxW = size * (VIEW_W / VIEW_H);
        animateViewBox([cx - boxW / 2, cy - boxH / 2, boxW, boxH]);
    }
    function resetZoom() {
        animateViewBox(FULL_VIEWBOX.slice());
    }

    /* ---------- 지역 선택 → 패널 ---------- */
    async function selectRegion(sigCd) {
        if (!sigCd) return;
        svg.querySelectorAll('.sig-path.selected').forEach((p) => p.classList.remove('selected'));
        const target = svg.querySelector('.sig-path[data-sig-cd="' + sigCd + '"]');
        if (target) target.classList.add('selected');

        zoomToRegion(sigCd); // 해당 지역으로 살짝 이동/강조

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

        const spWrap = document.getElementById('panel-specialties-wrap');
        const sp = document.getElementById('panel-specialties');
        sp.innerHTML = '';
        const specialties = data.specialties || [];
        specialties.forEach((s) => sp.appendChild(el('span',
            'px-4 py-2 bg-surface-alt border border-border rounded font-body-main text-caption text-text-primary', s)));
        setShow(spWrap, specialties.length > 0);

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

        const courseWrap = document.getElementById('panel-course-wrap');
        const course = document.getElementById('panel-course');
        course.innerHTML = '';
        const points = data.briefCourse || [];
        if (points.length > 0) {
            course.appendChild(el('div', 'absolute left-[11px] top-2 bottom-6 w-[1px] border-l border-dotted border-outline-variant'));
            points.forEach((pt) => {
                const item = el('div', 'relative mb-8 last:mb-0 group');
                item.appendChild(el('div', 'absolute -left-6 top-0 w-6 h-6 rounded-full bg-surface border border-primary flex items-center justify-center z-10 font-section-title text-xs text-primary', String(pt.order)));
                const head = el('div', 'flex items-center gap-2 mb-1');
                head.appendChild(el('h4', 'font-section-title text-card-title text-text-primary', pt.name));
                head.appendChild(el('span', 'text-[11px] bg-surface-alt px-2 py-0.5 rounded border border-border text-text-muted', pt.type));
                item.appendChild(head);
                item.appendChild(el('p', 'font-body-main text-caption text-text-muted', pt.desc));
                course.appendChild(item);
            });
        }
        setShow(courseWrap, points.length > 0);

        document.getElementById('panel-go').setAttribute('href', '/region?sigCd=' + encodeURIComponent(sigCd));
    }

    function openPanel() {
        const wasOpen = panel.classList.contains('open');
        panel.classList.add('open');
        if (wasOpen) retrigger(panel, 'open'); // 이미 열려 있으면 진입 애니메이션 재생
        retrigger(panelBody, 'stagger-fade-in');
    }
    function closePanel() {
        panel.classList.remove('open');
        svg.querySelectorAll('.sig-path.selected').forEach((p) => p.classList.remove('selected'));
        resetZoom();
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
            items.forEach((li) => setShow(li, (li.getAttribute('data-name') || '').toLowerCase().includes(q)));
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
    }

    /* ---------- 무작위로 한 곳 보기 ----------
       지금은 저평가 지수가 없어 단순 무작위. (지수 도입 시 가중 샘플링으로 교체) */
    function pickRandom() {
        const paths = svg.querySelectorAll('.sig-path');
        if (!paths.length) return;
        const p = paths[Math.floor(Math.random() * paths.length)];
        selectRegion(p.getAttribute('data-sig-cd'));
    }

    /* ---------- 초기화 ---------- */
    async function init() {
        svg = document.getElementById('korea-map');
        panel = document.getElementById('region-panel');
        panelBody = document.getElementById('panel-body');
        if (!svg || !panel) return;

        initSearch();
        await renderMap();

        const closeBtn = document.getElementById('panel-close');
        if (closeBtn) closeBtn.addEventListener('click', closePanel);

        const shuffle = document.getElementById('map-shuffle');
        if (shuffle) shuffle.addEventListener('click', pickRandom);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
