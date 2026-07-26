package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.goodprice.GoodPriceSyncService;
import com.sunz.hidden_travel.tour.TourSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 관리자용 데이터 적재 트리거. 사용자 요청 경로가 아니며, 배치를 수동 실행한다.
 * (사용자 화면은 항상 DB만 읽는다 — 이 엔드포인트에서만 TourAPI를 호출)
 *
 *  - POST /admin/sync/tour?sigCd=47170  → 단일 지역(안동시) 적재
 *  - POST /admin/sync/tour?areaCode=35  → 시도(경북) 전체 적재
 *  - POST /admin/sync/tour              → 전국 배치
 */
@RestController
@RequestMapping("/admin/sync")
public class AdminSyncController {

    private final TourSyncService sync;
    private final GoodPriceSyncService goodPriceSync;

    public AdminSyncController(TourSyncService sync, GoodPriceSyncService goodPriceSync) {
        this.sync = sync;
        this.goodPriceSync = goodPriceSync;
    }

    @PostMapping("/tour")
    public Map<String, Object> tour(@RequestParam(required = false) String sigCd,
                                    @RequestParam(required = false) Integer areaCode) {
        if (sigCd != null) {
            return sync.syncRegion(sigCd);
        }
        if (areaCode != null) {
            return sync.syncSido(areaCode);
        }
        return sync.syncAll();
    }

    /**
     * 착한가격업소 CSV 적재. (URL 한글 회피 위해 시도 2자리 코드 사용)
     *  - POST /admin/sync/goodprice?sidoCode=47 → 경북만
     *  - POST /admin/sync/goodprice             → 전국
     */
    @PostMapping("/goodprice")
    public Map<String, Object> goodPrice(@RequestParam(required = false) String sidoCode) {
        return goodPriceSync.sync(sidoCode);
    }
}
