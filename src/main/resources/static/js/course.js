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

    /* ---------- 코스 항목 DOM 생성 ---------- */
    function makeCourseItem(name, type, category, sage) {
        const row = el('div', 'course-item flex items-center gap-3 bg-surface border border-border rounded-lg p-3 mb-3');
        row.setAttribute('data-name', name);
        row.setAttribute('data-type', type);
        row.setAttribute('data-category', category || '');
        row.setAttribute('data-sage', sage ? 'true' : 'false');

        const handle = el('span', 'material-symbols-outlined text-text-muted cursor-grab drag-handle text-[20px]', 'drag_indicator');
        const badge = el('div', 'w-7 h-7 rounded-full bg-accent-soft text-primary flex items-center justify-center font-bold text-sm shrink-0 order-num', '0');

        const body = el('div', 'flex-1 min-w-0');
        const head = el('div', 'flex items-center gap-2 flex-wrap');
        head.appendChild(el('span', 'font-semibold text-text-primary truncate', name));
        head.appendChild(el('span', 'bg-surface-alt text-text-muted font-caption text-caption px-2 py-0.5 rounded text-[11px]', category || type));
        if (sage) head.appendChild(el('span', 'badge-sage', '착한가격업소'));
        body.appendChild(head);

        const del = el('button', 'del-btn text-text-muted hover:text-error transition-colors p-1');
        del.type = 'button';
        del.setAttribute('aria-label', '삭제');
        del.appendChild(el('span', 'material-symbols-outlined', 'close'));

        row.appendChild(handle);
        row.appendChild(badge);
        row.appendChild(body);
        row.appendChild(del);
        makeDraggable(row);
        return row;
    }

    /* ---------- 추가 / 삭제 ---------- */
    function addFromCard(card) {
        const name = card.getAttribute('data-name');
        if (!name || inCourse(name)) return;
        const type = card.getAttribute('data-type');
        const category = card.getAttribute('data-category');
        const sage = card.getAttribute('data-sage') === 'true';
        timeline.insertBefore(makeCourseItem(name, type, category, sage), emptyState);
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
    }
    function renumber() {
        courseItems().forEach((it, i) => {
            const n = it.querySelector('.order-num');
            if (n) n.textContent = String(i + 1);
        });
    }
    function updateSummary() {
        const t = document.getElementById('total-places');
        if (t) t.textContent = String(courseItems().length);
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
            if (items.length === 0) return; // 빈 코스는 저장하지 않는다(버튼도 비활성 상태)
            document.getElementById('f-courseName').value = document.getElementById('course-name-input').value || '나의 코스';
            // 경유지를 순서대로 JSON 직렬화 → 서버가 SavedCourseStop 으로 저장
            document.getElementById('f-itemsJson').value = JSON.stringify(items.map((i) => ({
                name: i.getAttribute('data-name'),
                type: i.getAttribute('data-type'),
                sage: i.getAttribute('data-sage') === 'true'
            })));
            btn.disabled = true; // 중복 제출 방지
            form.submit();
        });
    }

    /* ---------- 초기화 ---------- */
    function init() {
        timeline = document.getElementById('course-timeline');
        emptyState = document.getElementById('empty-state');
        candidateSide = document.getElementById('candidate-side');
        if (!timeline) return;

        // 후보 [+ 담기]
        if (candidateSide) candidateSide.addEventListener('click', (e) => {
            const btn = e.target.closest('.add-btn');
            if (!btn) return;
            const card = btn.closest('.cand-card');
            if (card) addFromCard(card);
        });
        // 코스 [삭제]
        timeline.addEventListener('click', (e) => {
            const del = e.target.closest('.del-btn');
            if (!del) return;
            const item = del.closest('.course-item');
            if (item) removeItem(item.getAttribute('data-name'));
        });

        initTabs();
        initDnd();
        initSave();
        refresh(); // 초기 담긴 항목 반영(번호/요약/왼쪽 체크)
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
