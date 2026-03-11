package com.velocity.sabziwala.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
	private boolean sucess;
	private String message;
	private T data;
	private String error;
	@Builder.Default
	private LocalDateTime timestamp = LocalDateTime.now();
	
	public static <T> ApiResponse<T> sucess(String message,T data){
		return ApiResponse.<T>builder()
				.sucess(true)
				.message(message)
				.data(data)
				.build();
	}
	
	public static <T> ApiResponse<T> sucess(String message){
		return ApiResponse.<T>builder()
				.sucess(true)
				.message(message)
				.build();
	}
	
	public static <T> ApiResponse<T> error(String message,String errors){
		return ApiResponse.<T>builder()
				.sucess(false)
				.message(message)
				.error(errors)
				.build();
	}
}
