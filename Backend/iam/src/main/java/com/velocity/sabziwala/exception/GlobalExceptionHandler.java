package com.velocity.sabziwala.exception;

import java.io.ObjectInputStream.GetField;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.velocity.sabziwala.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/* Centralized Exception Handling for all Controllers */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex){
		
		Map<String, String> errorData = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(fe -> fe.getField(), fe -> fe.getDefaultMessage()));
		
		return ResponseEntity.badRequest()
				.body(ApiResponse.<Map<String, String>>builder()
						.sucess(false)
						.message("Validation Failed")
						.data(errorData)
						.build());
		
	}
	
	
	@ExceptionHandler(TokenException.class)
	public ResponseEntity<ApiResponse<Void>> handleTokenError(TokenException ex){
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication Failed", ex.getMessage()));
	}
	
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex){
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Internal Server Error"," An unexpected error has occurred. Please try again after some time."));
	}

}
