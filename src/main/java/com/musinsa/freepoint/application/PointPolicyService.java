package com.musinsa.freepoint.application;

import com.musinsa.freepoint.common.exception.BusinessException;
import com.musinsa.freepoint.common.exception.ErrorCode;
import com.musinsa.freepoint.domain.policy.PointPolicy;
import com.musinsa.freepoint.domain.policy.PointPolicyKey;
import com.musinsa.freepoint.domain.policy.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PointPolicyService {

    private final PointPolicyRepository pointPolicyRepository;

    public long getLongValue(PointPolicyKey key) {
        String value = getRawValue(key);
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(
                    ErrorCode.POLICY_NOT_FOUND,
                    "정책 값을 숫자로 변환할 수 없습니다. key=" + key.name() + ", value=" + value
            );
        }
    }

    public int getIntValue(PointPolicyKey key) {
        long value = getLongValue(key);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new BusinessException(
                    ErrorCode.POLICY_NOT_FOUND,
                    "정책 값이 int 범위를 벗어났습니다. key=" + key.name() + ", value=" + value
            );
        }
        return (int) value;
    }

    public long getMaxEarnAmountPerOnce() {
        return getLongValue(PointPolicyKey.MAX_EARN_AMOUNT_PER_ONCE);
    }

    public long getMaxWalletBalance() {
        return getLongValue(PointPolicyKey.MAX_WALLET_BALANCE);
    }

    public int getDefaultExpireDays() {
        return getIntValue(PointPolicyKey.DEFAULT_EXPIRE_DAYS);
    }

    public int getMinExpireDays() {
        return getIntValue(PointPolicyKey.MIN_EXPIRE_DAYS);
    }

    public int getMaxExpireDays() {
        return getIntValue(PointPolicyKey.MAX_EXPIRE_DAYS);
    }

    private String getRawValue(PointPolicyKey key) {
        PointPolicy policy = pointPolicyRepository.findByPolicyKeyAndEnabledTrue(key)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.POLICY_NOT_FOUND,
                        "활성화된 정책이 존재하지 않습니다. key=" + key.name()
                ));
        return policy.getPolicyValue();
    }
}
