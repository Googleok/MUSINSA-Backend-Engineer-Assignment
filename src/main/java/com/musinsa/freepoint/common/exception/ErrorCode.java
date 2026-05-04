package com.musinsa.freepoint.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "내부 서버 오류가 발생했습니다."),

    // 정책
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "포인트 정책을 찾을 수 없습니다."),

    // 적립
    INVALID_EARN_AMOUNT(HttpStatus.BAD_REQUEST, "E001", "적립 가능한 포인트 금액이 아닙니다."),
    INVALID_EXPIRE_DAYS(HttpStatus.BAD_REQUEST, "E002", "만료일은 최소 1일 이상, 최대 5년 미만이어야 합니다."),
    BALANCE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "E003", "보유 가능한 최대 포인트를 초과했습니다."),
    EARNING_NOT_FOUND(HttpStatus.NOT_FOUND, "E004", "적립 내역을 찾을 수 없습니다."),
    EARNING_ALREADY_USED(HttpStatus.CONFLICT, "E005", "일부라도 사용된 적립은 취소할 수 없습니다."),
    EARNING_ALREADY_CANCELED(HttpStatus.CONFLICT, "E006", "이미 취소된 적립입니다."),

    // 사용
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "U001", "보유 포인트가 부족합니다."),
    ORDER_NO_REQUIRED(HttpStatus.BAD_REQUEST, "U002", "포인트 사용 시 주문번호는 필수입니다."),
    USE_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "U003", "사용 내역을 찾을 수 없습니다."),
    INVALID_USE_AMOUNT(HttpStatus.BAD_REQUEST, "U004", "사용 금액은 1포인트 이상이어야 합니다."),

    // 사용취소
    INVALID_CANCEL_AMOUNT(HttpStatus.BAD_REQUEST, "X001", "취소 금액이 잘못되었습니다."),
    USE_CANCEL_AMOUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "X002", "취소 가능한 금액을 초과했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
