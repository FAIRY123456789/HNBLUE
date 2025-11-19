package com.example.jpaspringboot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 这将应用于所有的控制器
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<?> handleUserAlreadyExists(UserAlreadyExistsException e) {
        // 用户名已存在
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    // 其他异常处理方法...
}

