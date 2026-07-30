// 코스 동선 지도 — 담은 순서대로 번호 마커·연결선·방향 화살표를 그린다.
//
// 경로 탐색 API 는 쓰지 않는다(직선 연결). 실제 도로를 따라가진 않지만
// "어느 순서로 어느 방향으로 도는지"는 그대로 드러나, 동선이 지그재그인지
// 한 방향으로 흐르는지 판단할 수 있다.
(function () {
    'use strict';

    let map = null;
    let overlays = [];      // 마커·선·화살표 — 다시 그릴 때 전부 제거
    let emptyEl = null;

    /* ---------- 코스에서 좌표가 있는 경유지만 뽑는다 ---------- */
    function stops() {
        return Array.from(document.querySelectorAll('#course-timeline .course-item'))
            .map((el) => ({
                name: el.getAttribute('data-name'),
                lat: parseFloat(el.getAttribute('data-lat')),
                lng: parseFloat(el.getAttribute('data-lng')),
            }))
            .filter((s) => !isNaN(s.lat) && !isNaN(s.lng));
    }

    function clearOverlays() {
        overlays.forEach((o) => o.setMap(null));
        overlays = [];
    }

    /** 번호가 들어간 마커 */
    function numberMarker(pos, index) {
        return new kakao.maps.CustomOverlay({
            position: pos,
            yAnchor: 0.5,
            zIndex: 3,
            content:
                '<div style="display:flex;align-items:center;justify-content:center;' +
                'width:26px;height:26px;border-radius:50%;background:var(--accent);color:#fff;' +
                'font-weight:700;font-size:13px;border:2px solid #fff;' +
                'box-shadow:0 1px 4px rgba(0,0,0,.3)">' + index + '</div>',
        });
    }

    /** 두 지점 사이 중간에 진행 방향 화살표를 놓는다 */
    function arrowOverlay(from, to) {
        const midLat = (from.getLat() + to.getLat()) / 2;
        const midLng = (from.getLng() + to.getLng()) / 2;
        // 화면상 각도 — y(위도)는 위로 갈수록 커지므로 부호를 뒤집는다
        const deg = Math.atan2(to.getLat() - from.getLat(), to.getLng() - from.getLng()) * 180 / Math.PI;
        return new kakao.maps.CustomOverlay({
            position: new kakao.maps.LatLng(midLat, midLng),
            yAnchor: 0.5,
            zIndex: 2,
            content:
                '<div style="transform:rotate(' + (-deg) + 'deg);color:var(--accent-hover);' +
                'font-size:18px;line-height:1;text-shadow:0 0 3px #fff,0 0 3px #fff">➤</div>',
        });
    }

    function render() {
        if (!map) return;
        clearOverlays();

        const list = stops();
        if (emptyEl) emptyEl.style.display = list.length ? 'none' : '';
        if (!list.length) return;

        const positions = list.map((s) => new kakao.maps.LatLng(s.lat, s.lng));

        // 순서대로 잇는 선
        if (positions.length > 1) {
            const line = new kakao.maps.Polyline({
                path: positions,
                strokeWeight: 3,
                strokeColor: '#D08A5D',       // --accent
                strokeOpacity: 0.9,
                strokeStyle: 'solid',
            });
            line.setMap(map);
            overlays.push(line);

            for (let i = 0; i < positions.length - 1; i++) {
                const arrow = arrowOverlay(positions[i], positions[i + 1]);
                arrow.setMap(map);
                overlays.push(arrow);
            }
        }

        positions.forEach((pos, i) => {
            const m = numberMarker(pos, i + 1);
            m.setMap(map);
            overlays.push(m);
        });

        // 전체가 보이도록 맞춘다
        const bounds = new kakao.maps.LatLngBounds();
        positions.forEach((p) => bounds.extend(p));
        if (positions.length === 1) {
            map.setCenter(positions[0]);
            map.setLevel(5);
        } else {
            map.setBounds(bounds, 40, 40, 40, 40);
        }
    }

    function init() {
        const el = document.getElementById('course-map');
        emptyEl = document.getElementById('course-map-empty');
        // 키가 없으면 SDK 자체가 로드되지 않는다 → 안내만 남기고 조용히 끝낸다
        if (!el || typeof kakao === 'undefined' || !kakao.maps) return;

        kakao.maps.load(() => {
            map = new kakao.maps.Map(el, {
                center: new kakao.maps.LatLng(36.5, 127.9),
                level: 12,
            });
            render();
            // course.js 가 담기/삭제/순서변경 후 알려준다
            document.addEventListener('course:changed', render);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
