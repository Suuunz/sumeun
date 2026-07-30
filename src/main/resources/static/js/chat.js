// AI 여행 상담 — 대화 전송/렌더. 추천 이동 버튼의 URL 은 서버가 만들어 준 값만 쓴다.
(function () {
    'use strict';

    let log, form, input, sendBtn, intro;
    const history = [];   // {role:'user'|'assistant', text}

    function el(tag, cls, text) {
        const n = document.createElement(tag);
        if (cls) n.className = cls;
        if (text != null) n.textContent = text;
        return n;
    }

    function scrollToEnd() {
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    /* ---------- 말풍선 ---------- */
    function addUser(text) {
        const wrap = el('div', 'flex justify-end');
        const bubble = el('div',
            'max-w-[85%] bg-primary text-on-primary rounded-xl rounded-br-sm px-4 py-3 font-body-main text-body-main whitespace-pre-line',
            text);
        wrap.appendChild(bubble);
        log.appendChild(wrap);
        return wrap;
    }

    function addBot(text) {
        const wrap = el('div', 'flex justify-start');
        const bubble = el('div',
            'max-w-[85%] bg-surface border-2 border-border rounded-xl rounded-bl-sm px-4 py-3 shadow-sm');
        const p = el('p', 'font-body-main text-body-main text-text-primary leading-relaxed whitespace-pre-line', text);
        bubble.appendChild(p);
        wrap.appendChild(bubble);
        log.appendChild(wrap);
        return bubble;
    }

    function addError(text) {
        const wrap = el('div', 'flex justify-start');
        const bubble = el('div', 'max-w-[85%] bg-surface border-2 rounded-xl px-4 py-3');
        bubble.style.borderColor = 'var(--error)';
        const p = el('p', 'font-body-main text-body-main', text);
        p.style.color = 'var(--error)';
        bubble.appendChild(p);
        wrap.appendChild(bubble);
        log.appendChild(wrap);
    }

    /** 타이핑 표시 */
    function addPending() {
        const wrap = el('div', 'flex justify-start');
        wrap.setAttribute('data-pending', 'true');
        const bubble = el('div', 'bg-surface border-2 border-border rounded-xl rounded-bl-sm px-4 py-3 flex items-center gap-2');
        bubble.appendChild(el('span', 'material-symbols-outlined text-primary text-[18px]', 'auto_awesome'));
        bubble.appendChild(el('span', 'font-caption text-caption text-text-muted', '추천을 찾고 있어요…'));
        wrap.appendChild(bubble);
        log.appendChild(wrap);
        return wrap;
    }

    /* ---------- 추천 이동 버튼 ---------- */
    function addRecommendations(bubble, recs) {
        if (!recs || recs.length === 0) return;

        const box = el('div', 'mt-3 pt-3 border-t border-border flex flex-col gap-2');
        recs.forEach((r) => {
            const a = document.createElement('a');
            a.href = r.url;   // 서버가 DB로 검증해 만든 경로
            a.className = 'flex items-center gap-3 bg-surface-alt hover:bg-accent-soft border border-border hover:border-primary rounded-lg px-3 py-2 transition-colors group';

            const icon = el('span', 'material-symbols-outlined text-primary text-[20px] shrink-0',
                r.type === 'course' ? 'route' : 'location_on');

            const body = el('div', 'flex-1 min-w-0');
            body.appendChild(el('span', 'block font-card-title text-card-title text-text-primary truncate', r.title));
            if (r.subtitle) {
                body.appendChild(el('span', 'block font-caption text-caption text-text-muted truncate', r.subtitle));
            }

            const go = el('span', 'material-symbols-outlined text-text-muted group-hover:text-primary transition-colors', 'arrow_forward');

            a.appendChild(icon);
            a.appendChild(body);
            a.appendChild(go);
            box.appendChild(a);
        });
        bubble.appendChild(box);
    }

    /* ---------- 전송 ---------- */
    async function send(text) {
        if (!text || !text.trim()) return;
        const message = text.trim();

        if (intro) intro.classList.add('hidden');
        addUser(message);
        history.push({ role: 'user', text: message });

        input.value = '';
        autoGrow();
        sendBtn.disabled = true;
        const pending = addPending();
        scrollToEnd();

        try {
            const res = await fetch('/api/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                // 직전 대화만 보낸다(방금 넣은 사용자 발화는 message 로 따로 감)
                body: JSON.stringify({ message: message, history: history.slice(0, -1) }),
            });
            pending.remove();

            if (!res.ok) {
                addError('답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.');
                return;
            }
            const data = await res.json();
            if (data.error) {
                addError(data.error);
                return;
            }
            const bubble = addBot(data.answer || '');
            addRecommendations(bubble, data.recommendations);
            history.push({ role: 'assistant', text: data.answer || '' });
        } catch (e) {
            pending.remove();
            addError('연결에 문제가 있어요. 네트워크를 확인해 주세요.');
        } finally {
            sendBtn.disabled = false;
            scrollToEnd();
        }
    }

    /* ---------- 입력창 높이 자동 조절 ---------- */
    function autoGrow() {
        input.style.height = 'auto';
        input.style.height = Math.min(input.scrollHeight, 128) + 'px';
    }

    function init() {
        log = document.getElementById('chat-log');
        form = document.getElementById('chat-form');
        input = document.getElementById('chat-input');
        sendBtn = document.getElementById('chat-send');
        intro = document.getElementById('chat-intro');
        if (!log || !form || !input) return;

        form.addEventListener('submit', (e) => {
            e.preventDefault();
            send(input.value);
        });

        input.addEventListener('input', autoGrow);
        // Enter 전송 / Shift+Enter 줄바꿈
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                send(input.value);
            }
        });

        document.querySelectorAll('.example-chip').forEach((chip) => {
            chip.addEventListener('click', () => send(chip.textContent));
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
