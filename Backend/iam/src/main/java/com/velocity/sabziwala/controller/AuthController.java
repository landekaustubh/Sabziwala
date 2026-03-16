package com.velocity.sabziwala.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.velocity.sabziwala.dto.request.LoginRequest;
import com.velocity.sabziwala.dto.request.RefreshTokenRequest;
import com.velocity.sabziwala.dto.request.RegisterRequest;
import com.velocity.sabziwala.dto.response.ApiResponse;
import com.velocity.sabziwala.dto.response.AuthResponse;
import com.velocity.sabziwala.dto.response.UserResponse;
import com.velocity.sabziwala.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody @Valid RegisterRequest request) {
		AuthResponse response = authService.register(request);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.sucess("Registeration is Successful ! Welcome", response));

	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
		AuthResponse response = authService.login(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.sucess("Login Successful!", response));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		AuthResponse response = authService.refreshToken(request);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.sucess("Token refreshed successfully", response));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request,
			@AuthenticationPrincipal UserDetails userDetails) {
		String authHeader = request.getHeader("Authorization");
		String accessToken = authHeader.substring(7);
		authService.logout(accessToken, userDetails.getUsername());

		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.sucess("Logged Out successfully !"));
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {

		UserResponse user = authService.getCurrentUser(userDetails.getUsername());

		return ResponseEntity.ok(ApiResponse.sucess("User profile retrieved", user));
	}

}
