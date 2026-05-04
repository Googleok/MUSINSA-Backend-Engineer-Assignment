package com.musinsa.freepoint.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsePointRequest(
        @NotBlank(message = "memberId는 필수입니다.")
        String memberId,

        @NotBlank(message = "orderNo는 필수입니다.")
        String orderNo,

        @NotNull(message = "amount는 필수입니다.")
        @Min(value = 1, message = "amount는 1 이상이어야 합니다.")
        Long amount
) {
}
