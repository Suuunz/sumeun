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
    // 실제 도로 경로 [[lng,lat], ...]. 있으면 직선 대신 이걸로 선을 그린다.
    let roadPath = null;

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
            zIndex: 4,
            content:
                '<div style="display:flex;align-items:center;justify-content:center;' +
                'width:26px;height:26px;border-radius:50%;background:var(--accent);color:#fff;' +
                'font-weight:700;font-size:13px;border:2px solid #fff;' +
                'box-shadow:0 1px 4px rgba(0,0,0,.3)">' + index + '</div>',
        });
    }

    /**
     * 두 지점 사이 중간에 진행 방향 화살표를 놓는다.
     * 선 위에 얹히므로 흰 원판을 깔아 선과 겹쳐도 방향이 읽히게 한다.
     */
    function arrowOverlay(from, to) {
        const midLat = (from.getLat() + to.getLat()) / 2;
        const midLng = (from.getLng() + to.getLng()) / 2;
        // 화면상 각도 — y(위도)는 위로 갈수록 커지므로 부호를 뒤집는다
        const deg = Math.atan2(to.getLat() - from.getLat(), to.getLng() - from.getLng()) * 180 / Math.PI;
        return new kakao.maps.CustomOverlay({
            position: new kakao.maps.LatLng(midLat, midLng),
            yAnchor: 0.5,
            zIndex: 3,
            content:
                '<div style="display:flex;align-items:center;justify-content:center;' +
                'width:20px;height:20px;border-radius:50%;background:#fff;' +
                'border:2px solid #D08A5D;box-shadow:0 1px 3px rgba(0,0,0,.25)">' +
                '<div style="transform:rotate(' + (-deg) + 'deg);color:#B9764C;' +
                'font-size:11px;line-height:1">➤</div></div>',
        });
    }

    function render() {
        if (!map) return;
        clearOverlays();

        const list = stops();
        if (emptyEl) emptyEl.style.display = list.length ? 'none' : '';
        if (!list.length) return;

        const positions = list.map((s) => new kakao.maps.LatLng(s.lat, s.lng));

        // 순서대로 잇는 선.
        // 지도 배경(도로·녹지)에 묻히지 않도록 흰 테두리선을 깔고 그 위에 색선을 얹는다.
        // 지도에서 경로를 강조할 때 쓰는 방식으로, 어떤 배경 위에서도 선이 살아난다.
        if (positions.length > 1) {
            // 도로 경로를 받아왔으면 그 좌표열로, 아니면 경유지를 직선으로 잇는다
            const linePath = roadPath && roadPath.length > 1
                ? roadPath.map((p) => new kakao.maps.LatLng(p[1], p[0]))
                : positions;

            const casing = new kakao.maps.Polyline({
                path: linePath,
                strokeWeight: 9,
                strokeColor: '#FFFFFF',
                strokeOpacity: 0.95,
                strokeStyle: 'solid',
                zIndex: 1,
            });
            casing.setMap(map);
            overlays.push(casing);

            const line = new kakao.maps.Polyline({
                path: linePath,
                strokeWeight: 5,
                strokeColor: '#D08A5D',       // --accent
                strokeOpacity: 1,
                strokeStyle: 'solid',
                zIndex: 2,
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

    /**
     * 저장된 코스의 실제 도로 경로를 가져와 선을 바꾸고, 소요시간·거리를 채운다.
     * 서버가 첫 호출에서만 길찾기를 부르고 결과를 저장하므로 두 번째부터는 호출이 없다.
     */
    function loadRoad(courseId) {
        if (!courseId) return;
        const distEl = document.getElementById('route-distance');
        const durEl = document.getElementById('route-duration');
        const noteEl = document.getElementById('route-note');

        fetch('/api/course/' + courseId + '/route')
            .then((res) => (res.ok ? res.json() : null))
            .then((d) => {
                if (!d || !d.available) {
                    // 경로를 못 구해도 직선 동선은 그대로 남는다
                    if (noteEl && d && d.message) noteEl.textContent = d.message;
                    return;
                }
                if (distEl && d.distanceText) distEl.textContent = d.distanceText;
                if (durEl && d.durationText) durEl.textContent = d.durationText;
                if (noteEl) noteEl.textContent = '실제 도로 기준 경로예요.';
                if (d.path && d.path.length > 1) {
                    roadPath = d.path;
                    render();
                }
            })
            .catch(() => { /* 실패하면 직선 동선을 유지한다 */ });
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
            // 저장된 코스라면 실제 도로 경로를 이어서 가져온다
            loadRoad(el.getAttribute('data-course-id'));
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
