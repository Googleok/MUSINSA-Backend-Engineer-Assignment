package com.musinsa.freepoint.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record CancelEarnPointRequest(
        @NotBlank(message = "memberId는 필수입니다.")
        String memberId,

        String reason
) {
}
