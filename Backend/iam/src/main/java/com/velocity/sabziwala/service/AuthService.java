package com.velocity.sabziwala.service;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velocity.sabziwala.dto.request.LoginRequest;
import com.velocity.sabziwala.dto.request.RefreshTokenRequest;
import com.velocity.sabziwala.dto.request.RegisterRequest;
import com.velocity.sabziwala.dto.response.AuthResponse;
import com.velocity.sabziwala.dto.response.UserResponse;
import com.velocity.sabziwala.entity.RefreshToken;
import com.velocity.sabziwala.entity.User;
import com.velocity.sabziwala.exception.DuplicateException;
import com.velocity.sabziwala.exception.ResourceNotFoundException;
import com.velocity.sabziwala.exception.TokenException;
import com.velocity.sabziwala.repository.RefreshTokenRepository;
import com.velocity.sabziwala.repository.UserRepository;
import com.velocity.sabziwala.security.JwtService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final RedisTokenService redisTokenService;

	@Value("${jwt.refresh-token.expiration-ms}")
	private long refreshTokenExpirationMs;

	// Register Customer
	public AuthResponse register(@Valid RegisterRequest request) {

		// Step 1 : Check for same email Id
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateException("Email already exists for another customer");
		}

		// Step 2 : Check for same User Name
		if (userRepository.existsByUserName(request.getUserName())) {
			throw new DuplicateException("Username already exists for another customer");
		}

		// Step 3 :Create the user entity object using Builder
		// Note : Plain password should be encrypted before its persisted.
		User user = User.builder().userName(request.getUserName()).email(request.getEmail())
				.fullName(request.getFullName()).passwordHash(passwordEncoder.encode(request.getPassword()))
				.phone(request.getPhone()).locality(request.getLocality()).build();

		// Step 4 : Save the user to DB
		User savedUser = userRepository.save(user);

		// Step 5 : Generate the Refresh token for this user
		return generateAuthResponse(savedUser);
	}

	@Transactional
	public AuthResponse login(@Valid LoginRequest request) {

		// Step 1 : Spring Security authentication (throws BadCredentialsException on
		// failure)
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
				request.getEmail().toLowerCase().trim(), request.getPassword()));

		// Step 2 : Load user
		User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
				.orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

		// Step 3 : Check if account is active
		if (!user.isActive()) {
			throw new BadCredentialsException("Account is deactivated");
		}

		// Step 4 : Revoke old refresh tokens (enforce single active session)
		refreshTokenRepository.revokeAllByUser(user);
		redisTokenService.removeCachedRefreshToken(user.getUserId().toString());

		// Step 5 : Update last login
		userRepository.updateLastLoginTime(user.getUserId(), LocalDateTime.now());

		// Step 6 : Generate tokens
		AuthResponse response = generateAuthResponse(user);

		return response;
	}

	@Transactional
	public AuthResponse refreshToken(@Valid RefreshTokenRequest request) {
		// Find refresh token in DB
		RefreshToken storedToken = refreshTokenRepository.findByTokenAndIsRevokedFalse(request.getRefreshToken())
				.orElseThrow(() -> new TokenException("Invalid refresh token. Please login again."));

		// Check expiry
		if (storedToken.isExpired()) {
			storedToken.setIsRevoked(true);
			refreshTokenRepository.save(storedToken);
			redisTokenService.removeCachedRefreshToken(storedToken.getUser().getUserId().toString());
			throw new TokenException("Refresh token expired. Please login again.");
		}

		User user = storedToken.getUser();

		// Rotate: revoke old token
		storedToken.setIsRevoked(true);

		// Generate new token pair
		String newAccessToken = jwtService.generateAccessToken(user.getUserId(), user.getEmail(), user.getRole().name(),
				user.getUserName());

		String newRefreshTokenStr = jwtService.generateRefreshToken();

		refreshTokenRepository.save(storedToken);

		// Save new refresh token

		RefreshToken newRefreshToken = new RefreshToken();
		newRefreshToken.setUser(user);
		newRefreshToken.setToken(newRefreshTokenStr);
		newRefreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
		newRefreshToken.setIsRevoked(false);

		refreshTokenRepository.save(newRefreshToken);

		// Update Redis cache
		redisTokenService.cacheRefreshToken(user.getUserId().toString(), newRefreshTokenStr, refreshTokenExpirationMs);

		log.info("Token refreshed for user: {}", user.getUserId());

		return AuthResponse.refreshed(newAccessToken, newRefreshTokenStr, jwtService.getAccessTokenExpirationSeconds());
	}

	@Transactional
	public void logout(String accessToken, String username) {
		User user = userRepository.findByEmail(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// Blacklist the access token for its remaining TTL
		long remainingTtl = jwtService.getRemainingTtlMs(accessToken);
		if (remainingTtl > 0) {
			redisTokenService.blacklistAccessToken(accessToken, remainingTtl);
		}

		// Revoke all refresh tokens in DB
		refreshTokenRepository.revokeAllByUser(user);

		// Clean up Redis
		redisTokenService.removeCachedRefreshToken(user.getUserId().toString());
		redisTokenService.destroySession(user.getUserId().toString());
	}

	private AuthResponse generateAuthResponse(User user) {
		// Step 1 : Generate Access Token
		String accessToken = jwtService.generateAccessToken(user.getUserId(), user.getEmail(), user.getRole().name(),
				user.getUserName());

		// Step 2 : Refresh token : Its a Randomly generated UUID
		String refreshToken = jwtService.generateRefreshToken();

		// Step 3 : Persist refresh token to DB with its USER Mapping
		RefreshToken rToken = new RefreshToken();
		rToken.setUser(user);
		rToken.setToken(refreshToken);
		rToken.setExpiryDate(Instant.now().plusMillis(85000));
		rToken.setIsRevoked(false);

		// Step 4 : Save the Token Entity
		refreshTokenRepository.save(rToken);

		// Step 5 : Share the Response
		return AuthResponse.of(accessToken, refreshToken, jwtService.getAccessTokenExpirationSeconds(),
				UserResponse.from(user));
	}

	public UserResponse getCurrentUser(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
		return UserResponse.from(user);
	}

}
