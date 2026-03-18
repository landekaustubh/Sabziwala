package com.velocity.sabziwala.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.velocity.sabziwala.security.JwtService;
import com.velocity.sabziwala.security.UserPrincipal;

import java.io.IOException;
import java.util.UUID;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║  JWT AUTHENTICATION FILTER — Order Service                        ║
 * ║                                                                   ║
 * ║  This filter runs ONCE per request (OncePerRequestFilter).        ║
 * ║  It does NOT authenticate users — that's IAM Service's job.       ║
 * ║  It PARSES the JWT that IAM already issued and sets up            ║
 * ║  Spring Security context so @PreAuthorize works.                  ║
 * ║                                                                   ║
 * ║  STEP-BY-STEP FLOW:                                               ║
 * ║  ─────────────────                                                ║
 * ║  ① Angular sends:  Authorization: Bearer eyJhbGci...              ║
 * ║  ② Filter extracts "eyJhbGci..." from the header                  ║
 * ║  ③ JwtService.isTokenValid() → verifies signature + expiry        ║
 * ║  ④ JwtService.extractEmail() → "customer@example.com"             ║
 * ║  ⑤ JwtService.extractUserId() → "550e8400-e29b-..."               ║
 * ║  ⑥ JwtService.extractRole() → "CUSTOMER"                          ║
 * ║  ⑦ Build UserPrincipal with role="CUSTOMER"                       ║
 * ║  ⑧ Create Authentication with authority="ROLE_CUSTOMER"           ║
 * ║  ⑨ Set in SecurityContext                                         ║
 * ║  ⑩ Now @PreAuthorize("hasRole('CUSTOMER')") can check it          ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // ① Extract Authorization header
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token → skip (public endpoint or will get 401 from Spring Security)
            filterChain.doFilter(request, response);
            return;
        }

        // ② Extract raw JWT string
        final String jwt = authHeader.substring(7);

        try {
            // ③ Validate: checks signature (using shared secret) + expiry
            if (!jwtService.isTokenValid(jwt)) {
                log.debug("Invalid JWT token received");
                filterChain.doFilter(request, response);
                return;
            }

            // ④⑤⑥ Extract claims from the JWT that IAM Service created
            String email   = jwtService.extractEmail(jwt);      // from "sub" claim
            String userId  = jwtService.extractUserId(jwt);     // from "userId" claim
            String role    = jwtService.extractRole(jwt);       // from "role" claim — "CUSTOMER" or "ADMIN"
            String username = jwtService.extractUsername(jwt);  // from "username" claim

            // ⑦ Build our UserPrincipal (implements UserDetails)
            UserPrincipal principal = UserPrincipal.builder()
                    .userId(UUID.fromString(userId))
                    .email(email)
                    .role(role)                    // Raw: "CUSTOMER"
                    .displayName(username)
                    .build();
            // principal.getAuthorities() returns → [ROLE_CUSTOMER]

            // ⑧ Create Spring Security Authentication token
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                principal,       // Principal (our UserPrincipal)
                                null,            // Credentials (not needed — already authenticated by IAM)
                                principal.getAuthorities()  // [ROLE_CUSTOMER] or [ROLE_ADMIN]
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // ⑨ Set in SecurityContext — now this request is "authenticated"
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated: email={}, role={}, userId={}", email, role, userId);
            }

        } catch (Exception e) {
            log.warn("JWT processing failed: {}", e.getMessage());
            // Don't throw — just don't set authentication.
            // Spring Security will return 401 for protected endpoints.
        }

        // ⑩ Continue filter chain → reach the controller
        filterChain.doFilter(request, response);
    }

    /**
     * Skip JWT processing for public endpoints (Swagger, health).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator");
    }
}
