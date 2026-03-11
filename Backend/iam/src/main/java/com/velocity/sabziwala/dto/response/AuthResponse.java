package com.velocity.sabziwala.dto.response;

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
public class AuthResponse {

	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private Long expiresIn; // Access Token TTL (Time to Live) in seconds
	private UserResponse user;

	/*
	 * Factory for Login/Register sucess.
	 */
	public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds, UserResponse user) {

		return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer")
				.expiresIn(expiresInSeconds).user(user).build();
	}

	/*
	 * Factory for token refresh
	 */
	public static AuthResponse refreshed(String accessToken, String refreshToken, long expiresInSeconds) {
		return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer")
				.expiresIn(expiresInSeconds).build();
	}

}
