// AI 추천 모달 — 취향/기분/자유서술 수집 → POST /api/recommend → 추천 지역을 지도에서 선택
(function () {
    'use strict';

    let modal, submitBtn, submitText, spinner, errorEl;

    function open() {
        if (!modal) return;
        errorEl.classList.add('hidden');
        modal.classList.remove('hidden');
    }
    function close() {
        if (modal) modal.classList.add('hidden');
    }

    function selectedValues(group, multi) {
        const chips = Array.from(modal.querySelectorAll('.rec-chip[data-group="' + group + '"].selected'));
        const vals = chips.map((c) => c.getAttribute('data-value'));
        return multi ? vals : (vals[0] || '');
    }

    function initChips() {
        modal.querySelectorAll('.rec-chip').forEach((chip) => {
            chip.addEventListener('click', () => {
                const group = chip.getAttribute('data-group');
                if (group === 'mood') {
                    // 단일 선택
                    modal.querySelectorAll('.rec-chip[data-group="mood"]').forEach((c) => {
                        if (c !== chip) c.classList.remove('selected');
                    });
                    chip.classList.toggle('selected');
                } else {
                    // 다중 선택(styles)
                    chip.classList.toggle('selected');
                }
            });
        });
    }

    function setLoading(on) {
        submitBtn.disabled = on;
        submitBtn.classList.toggle('opacity-80', on);
        submitText.classList.toggle('hidden', on);
        spinner.classList.toggle('hidden', !on);
    }

    async function submit() {
        errorEl.classList.add('hidden');
        const payload = {
            styles: selectedValues('styles', true),
            mood: selectedValues('mood', false),
            freeText: (document.getElementById('rec-freetext').value || '').trim()
        };
        setLoading(true);
        try {
            const res = await fetch('/api/recommend', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error('HTTP ' + res.status);
            const data = await res.json();
            if (!data || !data.sigCd) {
                errorEl.classList.remove('hidden');
                setLoading(false);
                return;
            }
            close();
            setLoading(false);
            // 3단계 지도 선택 흐름 재사용: 하이라이트 + 줌 + 패널 오픈
            if (typeof window.selectRegion === 'function') {
                window.selectRegion(data.sigCd);
            }
        } catch (err) {
            console.error('[recommend] 실패:', err);
            errorEl.classList.remove('hidden');
            setLoading(false);
        }
    }

    function init() {
        modal = document.getElementById('recommend-modal');
        const openBtn = document.getElementById('ai-recommend-btn');
        if (!modal || !openBtn) return;

        submitBtn = document.getElementById('rec-submit');
        submitText = document.getElementById('rec-submit-text');
        spinner = document.getElementById('rec-spinner');
        errorEl = document.getElementById('rec-error');

        openBtn.addEventListener('click', open);
        document.getElementById('rec-close').addEventListener('click', close);
        document.getElementById('rec-cancel').addEventListener('click', close);
        document.getElementById('rec-overlay').addEventListener('click', close);
        submitBtn.addEventListener('click', submit);

        initChips();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
