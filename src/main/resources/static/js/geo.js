// 브라우저 위치정보 수집 (부가 기능 — 거부/실패해도 앱은 정상 동작)
(function () {
    'use strict';

    const CACHE_KEY = 'userGeo';

    /**
     * 현재 위치를 안전하게 가져온다.
     * @returns {Promise<{lat:number, lng:number}|null>} 실패/거부/미지원이면 null (throw 하지 않음)
     */
    window.getCurrentPositionSafe = function (options) {
        return new Promise((resolve) => {
            if (!('geolocation' in navigator)) {
                resolve(null);
                return;
            }
            navigator.geolocation.getCurrentPosition(
                (pos) => {
                    const coords = { lat: pos.coords.latitude, lng: pos.coords.longitude };
                    try {
                        sessionStorage.setItem(CACHE_KEY, JSON.stringify(coords));
                    } catch (e) { /* 저장 실패 무시 */ }
                    resolve(coords);
                },
                () => resolve(null), // 거부/실패 → null (에러 throw 안 함)
                Object.assign({ enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 }, options || {})
            );
        });
    };

    /** 세션에 캐시된 위치(없으면 null) */
    window.getCachedGeo = function () {
        try {
            const s = sessionStorage.getItem(CACHE_KEY);
            return s ? JSON.parse(s) : null;
        } catch (e) {
            return null;
        }
    };
})();
