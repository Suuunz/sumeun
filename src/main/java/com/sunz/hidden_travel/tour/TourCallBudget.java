package com.sunz.hidden_travel.tour;

import com.sunz.hidden_travel.config.TourApiProperties;
import com.sunz.hidden_travel.external.DailyCallBudget;
import org.springframework.stereotype.Component;

/**
 * TourAPI 1일 호출 예산. 사용량 관리는 {@link DailyCallBudget} 에 위임한다.
 */
@Component
public class TourCallBudget {

    /** 사용량 기록에 쓰는 서비스 이름 */
    private static final String SERVICE = "tour";

    private final DailyCallBudget budget;
    private final int dailyLimit;

    public TourCallBudget(DailyCallBudget budget, TourApiProperties props) {
        this.budget = budget;
        this.dailyLimit = props.getDailyCallLimit();
    }

    public int dailyLimit() {
        return dailyLimit;
    }

    public boolean reserve() {
        return budget.reserve(SERVICE, dailyLimit);
    }

    public int used() {
        return budget.used(SERVICE);
    }

    public int remaining() {
        return budget.remaining(SERVICE, dailyLimit);
    }
}
