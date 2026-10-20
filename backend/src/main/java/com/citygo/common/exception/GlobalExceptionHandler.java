package com.citygo.common.exception;

import com.citygo.common.api.ApiResponse;
import com.citygo.common.api.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException e) {
        int status = switch (e.getErrorCode()) {
            case UNAUTHORIZED, USER_DISABLED, BAD_CREDENTIALS, SESSION_EXPIRED -> HttpStatus.UNAUTHORIZED.value();
            case FORBIDDEN -> HttpStatus.FORBIDDEN.value();
            case NOT_FOUND -> HttpStatus.NOT_FOUND.value();
            case CONFLICT, CATEGORY_HAS_CHILDREN, ACTIVITY_HAS_VALID_ORDERS, SESSION_SOLD_OUT,
                 SESSION_OFF_SALE, PACKAGE_OFF_SALE, PLATFORM_PAUSED, ORDER_NOT_PAYABLE,
                 ORDER_NOT_CANCELLABLE, ORDER_NOT_REFUNDABLE, ORDER_NOT_CONFIRMABLE,
                 ORDER_NOT_COMPLETABLE, INVALID_STATE_TRANSITION, IDEMPOTENCY_KEY_CONFLICT,
                 PAYMENT_AMOUNT_MISMATCH, CART_ITEM_INVALID, REFUND_AMOUNT_MISMATCH,
                 REFUND_EXISTS, REFUND_RETRYABLE -> HttpStatus.CONFLICT.value();
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS.value();
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE.value();
            default -> HttpStatus.BAD_REQUEST.value();
        };
        return ResponseEntity.status(status)
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? ErrorCode.BAD_REQUEST.getDefaultMessage()
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), message));
    }

    @ExceptionHandler({MissingRequestHeaderException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            jakarta.validation.ConstraintViolationException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.multipart.MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), ErrorCode.BAD_REQUEST.getDefaultMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getDefaultMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getDefaultMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("duplicate key: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorCode.CONFLICT.getCode(), "数据已存在或请求重复"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        log.error("unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }
}
