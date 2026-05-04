package com.musinsa.freepoint.presentation.request;

import com.musinsa.freepoint.domain.earning.PointEarnType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EarnPointRequest(
        @NotBlank(message = "memberId는 필수입니다.")
        String memberId,

        @NotNull(message = "amount는 필수입니다.")
        @Min(value = 1, message = "amount는 1 이상이어야 합니다.")
        Long amount,

        @NotNull(message = "earnType은 필수입니다.")
        EarnType earnType,

        @Min(value = 1, message = "expireDays는 1 이상이어야 합니다.")
        Integer expireDays,

        String reason
) {

    /**
     * 외부 API에서 허용하는 적립 유형. EXPIRED_RESTORE는 사용취소 내부 로직 전용으로 노출하지 않는다.
     */
    public enum EarnType {
        NORMAL,
        MANUAL;

        public PointEarnType toDomain() {
            return PointEarnType.valueOf(this.name());
        }
    }
}
