// 여행 MBTI 검사 — 한 번에 한 문항, 고르면 다음으로.
(function () {
    'use strict';

    let intro, quiz, result, questions, bar, current, backBtn;
    let answers = [];   // 문항 순서대로 1 또는 2
    let index = 0;

    /** 섹션은 flex 레이아웃이라 hidden 을 뗄 때 flex 를 되살려야 한다 */
    function show(el, on) {
        el.classList.toggle('hidden', !on);
        el.classList.toggle('flex', on);
    }

    function renderStep() {
        questions.forEach((q, i) => {
            q.classList.toggle('hidden', i !== index);
            q.classList.toggle('flex', i === index);
        });
        current.textContent = index + 1;
        bar.style.width = Math.round(((index + 1) / questions.length) * 100) + '%';
        backBtn.style.visibility = index === 0 ? 'hidden' : '';
        // 이전으로 돌아왔을 때 선택 흔적을 지운다
        questions[index].querySelectorAll('.mbti-a').forEach((b) => b.classList.remove('picked'));
    }

    function pick(qEl, value) {
        const btn = qEl.querySelector('.mbti-a[data-pick="' + value + '"]');
        if (btn) btn.classList.add('picked');
        answers[index] = value;

        // 고른 티가 나도록 잠깐 두었다가 넘어간다
        setTimeout(() => {
            if (index < questions.length - 1) {
                index++;
                renderStep();
            } else {
                submit();
            }
        }, 220);
    }

    async function submit() {
        try {
            const res = await fetch('/api/mbti/result', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ answers: answers }),
            });
            const d = await res.json();
            if (!res.ok || d.error) {
                showError(d && d.error);
                return;
            }
            document.getElementById('mbti-emoji').textContent = d.emoji || '🧳';
            document.getElementById('mbti-code').textContent = d.code || '';
            document.getElementById('mbti-label').textContent = d.label || '';
            document.getElementById('mbti-tagline').textContent = d.tagline || '';
            document.getElementById('mbti-style').textContent = d.style || '';

            // 저장되지 않았으면(비로그인) 가입 안내를 띄운다
            const note = document.getElementById('mbti-signup-note');
            if (note) note.classList.toggle('hidden', !!d.saved);

            show(quiz, false);
            show(result, true);
        } catch (e) {
            showError();
        }
    }

    /** 결과를 못 받았을 때도 화면이 멈추지 않게 안내를 띄운다 */
    function showError(message) {
        document.getElementById('mbti-emoji').textContent = '🧭';
        document.getElementById('mbti-code').textContent = '';
        document.getElementById('mbti-label').textContent = '결과를 가져오지 못했어요';
        document.getElementById('mbti-tagline').textContent = message || '잠시 후 다시 시도해 주세요.';
        document.getElementById('mbti-style').textContent = '';
        const note = document.getElementById('mbti-signup-note');
        if (note) note.classList.add('hidden');
        show(quiz, false);
        show(result, true);
    }

    function start() {
        answers = [];
        index = 0;
        show(intro, false);
        show(result, false);
        show(quiz, true);
        renderStep();
    }

    function init() {
        intro = document.getElementById('mbti-intro');
        quiz = document.getElementById('mbti-quiz');
        result = document.getElementById('mbti-result');
        bar = document.getElementById('mbti-bar');
        current = document.getElementById('mbti-current');
        backBtn = document.getElementById('mbti-back');
        if (!intro || !quiz || !result) return;

        questions = Array.from(quiz.querySelectorAll('.mbti-q'));
        if (!questions.length) return;

        document.getElementById('mbti-start').addEventListener('click', start);
        document.getElementById('mbti-retry').addEventListener('click', start);

        backBtn.addEventListener('click', () => {
            if (index > 0) {
                index--;
                renderStep();
            }
        });

        quiz.addEventListener('click', (e) => {
            const btn = e.target.closest('.mbti-a');
            if (!btn) return;
            const qEl = btn.closest('.mbti-q');
            if (!qEl || qEl.classList.contains('hidden')) return;
            pick(qEl, Number(btn.getAttribute('data-pick')));
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
