package com.musinsa.freepoint.presentation;

import com.musinsa.freepoint.application.PointCommandService;
import com.musinsa.freepoint.application.PointCommandService.CancelEarnResult;
import com.musinsa.freepoint.application.PointCommandService.CancelUseResult;
import com.musinsa.freepoint.application.PointCommandService.EarnResult;
import com.musinsa.freepoint.application.PointCommandService.UseResult;
import com.musinsa.freepoint.common.response.ApiResponse;
import com.musinsa.freepoint.presentation.request.CancelEarnPointRequest;
import com.musinsa.freepoint.presentation.request.CancelUsePointRequest;
import com.musinsa.freepoint.presentation.request.EarnPointRequest;
import com.musinsa.freepoint.presentation.request.UsePointRequest;
import com.musinsa.freepoint.presentation.response.CancelEarnPointResponse;
import com.musinsa.freepoint.presentation.response.CancelUsePointResponse;
import com.musinsa.freepoint.presentation.response.EarnPointResponse;
import com.musinsa.freepoint.presentation.response.UsePointResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointCommandService pointCommandService;

    @PostMapping("/earn")
    public ApiResponse<EarnPointResponse> earn(@Valid @RequestBody EarnPointRequest request) {
        EarnResult result = pointCommandService.earn(
                request.memberId(),
                request.amount(),
                request.earnType().toDomain(),
                request.expireDays(),
                request.reason()
        );
        return ApiResponse.success(EarnPointResponse.from(result));
    }

    @PostMapping("/earn/{pointKey}/cancel")
    public ApiResponse<CancelEarnPointResponse> cancelEarn(
            @PathVariable("pointKey") String pointKey,
            @Valid @RequestBody CancelEarnPointRequest request
    ) {
        CancelEarnResult result = pointCommandService.cancelEarn(
                pointKey,
                request.memberId(),
                request.reason()
        );
        return ApiResponse.success(CancelEarnPointResponse.from(result));
    }

    @PostMapping("/use")
    public ApiResponse<UsePointResponse> usePoint(@Valid @RequestBody UsePointRequest request) {
        UseResult result = pointCommandService.usePoint(
                request.memberId(),
                request.orderNo(),
                request.amount()
        );
        return ApiResponse.success(UsePointResponse.from(result));
    }

    @PostMapping("/use/{pointKey}/cancel")
    public ApiResponse<CancelUsePointResponse> cancelUse(
            @PathVariable("pointKey") String pointKey,
            @Valid @RequestBody CancelUsePointRequest request
    ) {
        CancelUseResult result = pointCommandService.cancelUse(
                pointKey,
                request.memberId(),
                request.amount(),
                request.reason()
        );
        return ApiResponse.success(CancelUsePointResponse.from(result));
    }
}
