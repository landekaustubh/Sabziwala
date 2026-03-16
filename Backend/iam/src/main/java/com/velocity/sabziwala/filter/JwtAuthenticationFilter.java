package com.velocity.sabziwala.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.velocity.sabziwala.security.CustomUserDetailsService;
import com.velocity.sabziwala.security.JwtService;
import com.velocity.sabziwala.service.RedisTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;
	private final RedisTokenService redisTokenService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Step 1: Extract token from Authorization header
		final String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		final String jwt = authHeader.substring(7);

		try {
			// Step 2: Check if token is blacklisted (user logged out)
			if (redisTokenService.isAccessTokenBlacklisted(jwt)) {
				log.debug("Token is blacklisted (user logged out)");
				filterChain.doFilter(request, response);
				return;
			}

			// Step 3: Extract email from JWT
			final String userEmail = jwtService.extractEmail(jwt);

			// Step 4: If email extracted and no existing auth in context
			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

				// Step 5: Load user details
				UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

				// Step 6: Validate token
				if (jwtService.isTokenValid(jwt, userDetails)) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
							null, userDetails.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

					// Step 7: Set authentication in context
					SecurityContextHolder.getContext().setAuthentication(authToken);
					log.debug("Authenticated user: {}", userEmail);
				}
			}
		} catch (Exception e) {
			log.warn("JWT authentication failed: {}", e.getMessage());
			// Don't throw — just don't authenticate. Spring Security will handle 401.
		}

		filterChain.doFilter(request, response);

	}

	/**
	 * Skip filter for public endpoints to avoid unnecessary processing.
	 */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		return path.startsWith("/auth/register") ||
				path.startsWith("/auth/login") || 
				path.startsWith("/auth/refresh") || 
				path.startsWith("/api-docs") ||
				path.startsWith("/swagger-ui") ||
				path.startsWith("/v3/api-docs");
	}

}
