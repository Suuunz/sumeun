package com.sunz.hidden_travel.tour;

import com.sunz.hidden_travel.config.TourApiProperties;
import com.sunz.hidden_travel.domain.ApiCallUsage;
import com.sunz.hidden_travel.repository.ApiCallUsageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * TourAPI 1일 호출 예산. 사용량을 DB 에 기록해 <b>재시작해도 유지</b>된다.
 *
 * 메모리 카운터만 쓰면 앱을 재시작할 때마다 0 으로 돌아가서, 개발 중처럼
 * 재시작이 잦은 상황에서는 가드가 무력해지고 실제 한도를 넘길 수 있다.
 */
@Component
public class TourCallBudget {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ApiCallUsageRepository repository;
    private final int dailyLimit;

    public TourCallBudget(ApiCallUsageRepository repository, TourApiProperties props) {
        this.repository = repository;
        this.dailyLimit = props.getDailyCallLimit();
    }

    public int dailyLimit() {
        return dailyLimit;
    }

    /**
     * 호출 1회를 예약한다. 한도를 넘으면 false — 호출하지 않는다.
     *
     * REQUIRES_NEW: 호출은 실제로 나갔는데 바깥 트랜잭션이 롤백되면
     * 사용량이 되돌아가 한도를 초과하게 되므로, 별도 트랜잭션으로 확정한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized boolean reserve() {
        ApiCallUsage usage = todayUsage();
        if (usage.getCount() >= dailyLimit) {
            return false;
        }
        usage.setCount(usage.getCount() + 1);
        repository.save(usage);
        return true;
    }

    @Transactional(readOnly = true)
    public int used() {
        return todayUsage().getCount();
    }

    @Transactional(readOnly = true)
    public int remaining() {
        return Math.max(0, dailyLimit - used());
    }

    /** 오늘 행이 없으면 0 으로 시작하는 새 행 (날짜가 PK 라 자정에 자연스럽게 리셋된다) */
    private ApiCallUsage todayUsage() {
        LocalDate today = LocalDate.now(KST);
        return repository.findById(today).orElseGet(() -> new ApiCallUsage(today, 0));
    }
}
