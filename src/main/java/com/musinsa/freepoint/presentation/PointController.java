package com.musinsa.freepoint.presentation;

import com.musinsa.freepoint.application.PointCommandService;
import com.musinsa.freepoint.application.PointCommandService.EarnResult;
import com.musinsa.freepoint.common.response.ApiResponse;
import com.musinsa.freepoint.presentation.request.EarnPointRequest;
import com.musinsa.freepoint.presentation.response.EarnPointResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
