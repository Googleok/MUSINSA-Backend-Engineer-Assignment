package com.musinsa.freepoint.config;

import com.musinsa.freepoint.domain.policy.PointPolicy;
import com.musinsa.freepoint.domain.policy.PointPolicyKey;
import com.musinsa.freepoint.domain.policy.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Map<PointPolicyKey, PolicyDefault> DEFAULTS = buildDefaults();

    private final PointPolicyRepository pointPolicyRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initDefaultPolicies();
    }

    @Transactional
    public void initDefaultPolicies() {
        DEFAULTS.forEach((key, def) -> {
            if (pointPolicyRepository.existsByPolicyKey(key)) {
                return;
            }
            pointPolicyRepository.save(PointPolicy.create(key, def.value(), def.description()));
            log.info("기본 정책 시드 등록: key={}, value={}", key, def.value());
        });
    }

    private static Map<PointPolicyKey, PolicyDefault> buildDefaults() {
        Map<PointPolicyKey, PolicyDefault> map = new LinkedHashMap<>();
        map.put(PointPolicyKey.MAX_EARN_AMOUNT_PER_ONCE,
                new PolicyDefault("100000", "1회 최대 적립 가능 포인트"));
        map.put(PointPolicyKey.MAX_WALLET_BALANCE,
                new PolicyDefault("1000000", "회원당 최대 보유 가능 포인트"));
        map.put(PointPolicyKey.DEFAULT_EXPIRE_DAYS,
                new PolicyDefault("365", "기본 만료일(일)"));
        map.put(PointPolicyKey.MIN_EXPIRE_DAYS,
                new PolicyDefault("1", "최소 만료일(일)"));
        map.put(PointPolicyKey.MAX_EXPIRE_DAYS,
                new PolicyDefault("1824", "최대 만료일(일, 5년 미만)"));
        return map;
    }

    private record PolicyDefault(String value, String description) {
    }
}
