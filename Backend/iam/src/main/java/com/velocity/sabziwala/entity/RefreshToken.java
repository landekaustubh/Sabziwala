package com.velocity.sabziwala.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens",schema = "iam")
public class RefreshToken extends AuditStamp {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "token_id")
	private UUID tokenId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false,foreignKey = @ForeignKey(name ="fk_rt_user"))
	private User user;
	
	@Column(name = "token")
	private String token;
	
	@Column(name = "expiry_date")
	private Instant expiryDate;
	
	@Column(name = "is_revoked")
	private Boolean isRevoked = false;
	
	/*
	 * Check if this refresh token has expired
	 */
	public boolean isExpired() {
		return Instant.now().isAfter(expiryDate);
	}
}
