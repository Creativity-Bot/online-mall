package com.example.onlinemall.exception;

/**
 * Checkout Workflow Update/Signal 失败，映射为可读的业务错误类型。
 */
public class CheckoutWorkflowException extends RuntimeException {

    private final String errorType;

    public CheckoutWorkflowException(String errorType, String message) {
        super(message);
        this.errorType = errorType == null ? "CheckoutFailed" : errorType;
    }

    public CheckoutWorkflowException(String errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType == null ? "CheckoutFailed" : errorType;
    }

    public String getErrorType() {
        return errorType;
    }
}
