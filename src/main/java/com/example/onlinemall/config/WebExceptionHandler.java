package com.example.onlinemall.config;


import com.example.onlinemall.dto.ResponseTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseTemplate<Void> handleException(Exception e) {
        log.error(e.toString(), e);
        return ResponseTemplate.fail("服务器异常");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseTemplate<Void> handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return ResponseTemplate.fail(e.toString());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseTemplate<Void> handleIllegalArgumentException(RuntimeException e) {
        log.error(e.toString(), e);
        return ResponseTemplate.fail(e.toString());
    }
}
