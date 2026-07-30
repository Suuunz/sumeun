// 내 코스 만들기 — 왼쪽 후보에서 담기/삭제, 순서 갱신, 저장.
// 코스 상태는 오른쪽 타임라인 DOM을 단일 진실원(source of truth)으로 삼는다.
(function () {
    'use strict';

    let timeline, emptyState, candidateSide;

    function el(tag, cls, text) {
        const n = document.createElement(tag);
        if (cls) n.className = cls;
        if (text != null) n.textContent = text;
        return n;
    }

    function courseItems() {
        return Array.from(timeline.querySelectorAll('.course-item'));
    }
    function currentNames() {
        return courseItems().map((i) => i.getAttribute('data-name'));
    }
    function inCourse(name) {
        return currentNames().includes(name);
    }

    /* ---------- 코스 항목 DOM 생성 ----------
       o: {name, type, category, sage, id, image, addr}
       id 가 있는 관광지는 후보 카드와 동일하게 사진과 [자세히]를 제공한다. */
    function makeCourseItem(o) {
        const row = el('div', 'course-item bg-surface border border-border rounded-lg p-3 mb-3');
        row.setAttribute('data-name', o.name);
        row.setAttribute('data-type', o.type);
        row.setAttribute('data-category', o.category || '');
        row.setAttribute('data-sage', o.sage ? 'true' : 'false');
        if (o.id) row.setAttribute('data-id', o.id);
        if (o.image) row.setAttribute('data-image', o.image);
        if (o.addr) row.setAttribute('data-addr', o.addr);

        const top = el('div', 'flex items-center gap-3');

        const handle = el('span', 'material-symbols-outlined text-text-muted cursor-grab drag-handle text-[20px]', 'drag_indicator');
        const badge = el('div', 'w-7 h-7 rounded-full bg-accent-soft text-primary flex items-center justify-center font-bold text-sm shrink-0 order-num', '0');

        // 썸네일 (없으면 자리를 차지하지 않는다)
        let thumb = null;
        if (o.image) {
            thumb = el('img', 'w-11 h-11 rounded-lg object-cover border border-border shrink-0 bg-surface-alt');
            thumb.src = o.image;
            thumb.alt = o.name;
            thumb.loading = 'lazy';
        }

        const body = el('div', 'flex-1 min-w-0');
        const head = el('div', 'flex items-center gap-2 flex-wrap');
        head.appendChild(el('span', 'font-semibold text-text-primary truncate', o.name));
        head.appendChild(el('span', 'bg-surface-alt text-text-muted font-caption text-caption px-2 py-0.5 rounded text-[11px]', o.category || o.type));
        if (o.sage) head.appendChild(el('span', 'badge-sage', '착한가격업소'));
        body.appendChild(head);

        // 상세는 TourAPI 관광지(id 보유)만 제공
        if (o.type === 'attraction' && o.id) {
            const detailBtn = el('button', 'detail-btn mt-1 inline-flex items-center gap-1 font-caption text-caption text-primary hover:underline');
            detailBtn.type = 'button';
            detailBtn.appendChild(el('span', 'material-symbols-outlined text-[16px]', 'expand_more'));
            detailBtn.appendChild(el('span', 'detail-btn__label', '자세히'));
            body.appendChild(detailBtn);
        }

        const del = el('button', 'del-btn text-text-muted hover:text-error transition-colors p-1');
        del.type = 'button';
        del.setAttribute('aria-label', '삭제');
        del.appendChild(el('span', 'material-symbols-outlined', 'close'));

        top.appendChild(handle);
        top.appendChild(badge);
        if (thumb) top.appendChild(thumb);
        top.appendChild(body);
        top.appendChild(del);
        row.appendChild(top);

        row.appendChild(el('div', 'item-detail hidden mt-3 pt-3 border-t border-border'));

        makeDraggable(row);
        return row;
    }

    /* ---------- 추가 / 삭제 ---------- */
    function addFromCard(card) {
        const name = card.getAttribute('data-name');
        if (!name || inCourse(name)) return;
        timeline.insertBefore(makeCourseItem({
            name: name,
            type: card.getAttribute('data-type'),
            category: card.getAttribute('data-category'),
            sage: card.getAttribute('data-sage') === 'true',
            id: card.getAttribute('data-id'),
            image: card.getAttribute('data-image'),
            addr: card.getAttribute('data-addr')
        }), emptyState);
        refresh();
    }
    function removeItem(name) {
        courseItems().forEach((it) => {
            if (it.getAttribute('data-name') === name) it.remove();
        });
        refresh();
    }

    /* ---------- 공통 갱신 ---------- */
    function refresh() {
        renumber();
        updateSummary();
        syncAdded();
        toggleEmpty();
        saveDraft();
    }

    /* ---------- 임시 보관 (비로그인 → 로그인 왕복 시 코스 유지) ----------
       비로그인 상태로 코스를 담다가 로그인하러 가면 페이지를 떠나게 된다.
       담은 내용을 sessionStorage 에 두었다가 돌아왔을 때 복원한다. */
    const DRAFT_KEY = 'courseDraft';

    function currentSigCd() {
        const input = document.querySelector('#save-form input[name="sigCd"]');
        return input ? input.value : '';
    }

    function saveDraft() {
        try {
            const items = courseItems().map((i) => ({
                name: i.getAttribute('data-name'),
                type: i.getAttribute('data-type'),
                category: i.getAttribute('data-category'),
                sage: i.getAttribute('data-sage') === 'true',
                id: i.getAttribute('data-id'),
                image: i.getAttribute('data-image'),
                addr: i.getAttribute('data-addr')
            }));
            if (items.length === 0) {
                sessionStorage.removeItem(DRAFT_KEY);
                return;
            }
            sessionStorage.setItem(DRAFT_KEY, JSON.stringify({ sigCd: currentSigCd(), items: items }));
        } catch (e) { /* 저장 실패는 무시 — 기능에 영향 없음 */ }
    }

    function restoreDraft() {
        try {
            const raw = sessionStorage.getItem(DRAFT_KEY);
            if (!raw) return;
            const draft = JSON.parse(raw);
            // 다른 지역의 코스는 복원하지 않는다
            if (!draft || draft.sigCd !== currentSigCd() || !Array.isArray(draft.items)) return;
            draft.items.forEach((it) => {
                if (!it.name || inCourse(it.name)) return; // 서버가 이미 렌더한 항목과 중복 방지
                timeline.insertBefore(makeCourseItem(it), emptyState);
            });
        } catch (e) { /* 손상된 값이면 무시 */ }
    }

    function clearDraft() {
        try {
            sessionStorage.removeItem(DRAFT_KEY);
        } catch (e) { /* 무시 */ }
    }
    function renumber() {
        courseItems().forEach((it, i) => {
            const n = it.querySelector('.order-num');
            if (n) n.textContent = String(i + 1);
        });
    }
    function updateSummary() {
        const count = courseItems().length;
        const t = document.getElementById('total-places');
        if (t) t.textContent = String(count);
        // 담긴 곳이 없으면 저장 버튼을 비활성 — 눌러도 아무 일 없는 상태를 만들지 않는다
        const btn = document.getElementById('save-btn');
        if (btn) {
            btn.disabled = count === 0;
            btn.title = count === 0 ? '코스에 장소를 담아야 저장할 수 있어요' : '';
        }
    }
    function syncAdded() {
        const names = currentNames();
        document.querySelectorAll('.cand-card').forEach((card) => {
            const added = names.includes(card.getAttribute('data-name'));
            const addBtn = card.querySelector('.add-btn');
            const check = card.querySelector('.added-check');
            if (addBtn) addBtn.classList.toggle('hidden', added);
            if (check) check.classList.toggle('hidden', !added);
            card.classList.toggle('opacity-70', added);
        });
    }
    function toggleEmpty() {
        if (emptyState) emptyState.style.display = courseItems().length ? 'none' : 'flex';
    }

    /* ---------- 탭 전환 ---------- */
    function initTabs() {
        const btns = Array.from(document.querySelectorAll('.tab-btn'));
        const panels = Array.from(document.querySelectorAll('.tab-panel'));
        btns.forEach((btn) => btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            panels.forEach((p) => {
                const active = p.getAttribute('data-panel') === tab;
                p.classList.toggle('hidden', !active);
                p.classList.toggle('flex', active);
            });
            btns.forEach((b) => {
                const on = b === btn;
                b.classList.toggle('text-primary', on);
                b.classList.toggle('font-semibold', on);
                b.classList.toggle('border-b-2', on);
                b.classList.toggle('border-primary', on);
                b.classList.toggle('text-text-muted', !on);
            });
        }));
    }

    /* ---------- 드래그 순서 변경 ---------- */
    let dragged = null;
    function makeDraggable(item) {
        item.setAttribute('draggable', 'true');
        item.addEventListener('dragstart', () => { dragged = item; item.classList.add('opacity-50'); });
        item.addEventListener('dragend', () => { dragged = null; item.classList.remove('opacity-50'); renumber(); });
    }
    function initDnd() {
        courseItems().forEach(makeDraggable);
        timeline.addEventListener('dragover', (e) => {
            e.preventDefault();
            if (!dragged) return;
            const after = afterElement(e.clientY);
            if (after == null) timeline.insertBefore(dragged, emptyState);
            else timeline.insertBefore(dragged, after);
        });
    }
    function afterElement(y) {
        const items = courseItems().filter((i) => i !== dragged);
        let closest = null, closestOffset = Number.NEGATIVE_INFINITY;
        for (const item of items) {
            const box = item.getBoundingClientRect();
            const offset = y - box.top - box.height / 2;
            if (offset < 0 && offset > closestOffset) { closestOffset = offset; closest = item; }
        }
        return closest;
    }

    /* ---------- 저장 ---------- */
    function initSave() {
        const btn = document.getElementById('save-btn');
        const form = document.getElementById('save-form');
        if (!btn || !form) return;
        btn.addEventListener('click', () => {
            const items = courseItems();
            if (items.length === 0) return; // 버튼이 비활성이라 도달하지 않지만 방어
            document.getElementById('f-courseName').value = document.getElementById('course-name-input').value || '나의 코스';
            // 경유지를 순서대로 JSON 직렬화 → 서버가 SavedCourseStop 으로 저장
            document.getElementById('f-itemsJson').value = JSON.stringify(items.map((i) => ({
                name: i.getAttribute('data-name'),
                type: i.getAttribute('data-type'),
                sage: i.getAttribute('data-sage') === 'true'
            })));
            clearDraft();        // 서버에 저장되므로 임시 보관본은 지운다
            btn.disabled = true; // 중복 제출 방지
            form.submit();
        });
    }

    /* ---------- 관광지 상세 펼치기 ----------
       상세(설명·이용시간·주차 등)는 TourAPI 호출이 들어가므로 미리 받아두지 않고
       펼칠 때 가져온다. 서버가 첫 조회 결과를 DB에 캐시하므로 두 번째부터는 즉시. */
    /**
     * @param host      .cand-card(후보) 또는 .course-item(담긴 곳)
     * @param withAddr  주소를 함께 보여줄지. 담긴 곳은 후보 목록을 떠나 있으므로
     *                  어디였는지 확인할 수 있게 주소를 같이 띄운다.
     */
    function toggleDetail(host, withAddr) {
        const panel = host.querySelector('.item-detail');
        const btn = host.querySelector('.detail-btn');
        if (!panel || !btn) return;

        const icon = btn.querySelector('.material-symbols-outlined');
        const label = btn.querySelector('.detail-btn__label');

        // 이미 열려 있으면 닫기
        if (!panel.classList.contains('hidden')) {
            panel.classList.add('hidden');
            if (icon) icon.textContent = 'expand_more';
            if (label) label.textContent = '자세히';
            return;
        }

        panel.classList.remove('hidden');
        if (icon) icon.textContent = 'expand_less';
        if (label) label.textContent = '접기';

        if (panel.dataset.loaded === 'true') return; // 이미 받아둔 내용 재사용

        panel.innerHTML = '<p class="font-caption text-caption text-text-muted">불러오는 중…</p>';
        fetch('/api/attraction/' + host.getAttribute('data-id'))
            .then((res) => (res.ok ? res.json() : null))
            .then((d) => {
                if (!d) {
                    panel.innerHTML = '<p class="font-caption text-caption text-text-muted">정보를 불러오지 못했어요.</p>';
                    return;
                }
                panel.innerHTML = renderDetail(d, withAddr);
                panel.dataset.loaded = 'true';
            })
            .catch(() => {
                panel.innerHTML = '<p class="font-caption text-caption text-text-muted">정보를 불러오지 못했어요.</p>';
            });
    }

    function esc(s) {
        const d = document.createElement('div');
        d.textContent = s == null ? '' : String(s);
        return d.innerHTML;
    }

    function detailRow(icon, label, value) {
        if (!value) return '';
        return '<div class="flex items-start gap-2">' +
            '<span class="material-symbols-outlined text-text-muted text-[16px] mt-0.5">' + icon + '</span>' +
            '<span class="font-caption text-caption text-text-muted shrink-0">' + label + '</span>' +
            '<span class="font-caption text-caption text-text-primary whitespace-pre-line">' + esc(value) + '</span>' +
            '</div>';
    }

    function renderDetail(d, withAddr) {
        let html = '';

        // 담긴 곳은 후보 목록에서 벗어나 있어 주소를 다시 보여준다
        if (withAddr && d.addr) {
            html += '<div class="flex items-start gap-2 mb-2">' +
                '<span class="material-symbols-outlined text-text-muted text-[16px] mt-0.5">location_on</span>' +
                '<span class="font-caption text-caption text-text-primary">' + esc(d.addr) + '</span>' +
                '</div>';
        }

        if (d.pending) {
            html += '<p class="font-caption text-caption text-text-muted mb-2">' +
                '오늘 조회 한도를 다 써서 상세 설명을 가져오지 못했어요. 내일 다시 열어보면 표시됩니다.</p>';
        }

        if (d.overview) {
            html += '<p class="font-body-main text-caption text-text-primary leading-relaxed whitespace-pre-line mb-3">' +
                esc(d.overview) + '</p>';
        } else if (!d.pending) {
            html += '<p class="font-caption text-caption text-text-muted mb-2">등록된 상세 설명이 없어요.</p>';
        }

        const rows = detailRow('schedule', '이용시간', d.usetime) +
            detailRow('event_busy', '휴무일', d.restdate) +
            detailRow('local_parking', '주차', d.parking) +
            detailRow('call', '문의', d.infocenter || d.tel);
        if (rows) html += '<div class="flex flex-col gap-1.5">' + rows + '</div>';

        if (d.homepage) {
            html += '<a href="' + esc(d.homepage) + '" target="_blank" rel="noopener noreferrer" ' +
                'class="inline-flex items-center gap-1 mt-3 font-caption text-caption text-primary hover:underline">' +
                '<span class="material-symbols-outlined text-[16px]">open_in_new</span>홈페이지</a>';
        }
        return html;
    }

    /* ---------- 초기화 ---------- */
    function init() {
        timeline = document.getElementById('course-timeline');
        emptyState = document.getElementById('empty-state');
        candidateSide = document.getElementById('candidate-side');
        if (!timeline) return;

        // 후보 [+ 담기] / [자세히] — 후보 목록에는 주소가 이미 보이므로 상세엔 넣지 않는다
        if (candidateSide) candidateSide.addEventListener('click', (e) => {
            const detailBtn = e.target.closest('.detail-btn');
            if (detailBtn) {
                const card = detailBtn.closest('.cand-card');
                if (card) toggleDetail(card, false);
                return;
            }
            const btn = e.target.closest('.add-btn');
            if (!btn) return;
            const card = btn.closest('.cand-card');
            if (card) addFromCard(card);
        });
        // 코스 [자세히] / [삭제] — 담긴 곳은 상세에 주소도 함께 보여준다
        timeline.addEventListener('click', (e) => {
            const detailBtn = e.target.closest('.detail-btn');
            if (detailBtn) {
                const item = detailBtn.closest('.course-item');
                if (item) toggleDetail(item, true);
                return;
            }
            const del = e.target.closest('.del-btn');
            if (!del) return;
            const item = del.closest('.course-item');
            if (item) removeItem(item.getAttribute('data-name'));
        });

        initTabs();
        initDnd();
        initSave();
        restoreDraft(); // 로그인하러 다녀온 사이 담아둔 코스 복원
        refresh();      // 초기 담긴 항목 반영(번호/요약/왼쪽 체크)
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
