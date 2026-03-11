package com.velocity.sabziwala.entity;

import java.util.UUID;

import com.velocity.sabziwala.enums.AuthProvider;
import com.velocity.sabziwala.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name ="users",schema="iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditStamp {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "user_id")
	private UUID userId;
	
	@Column(name = "user_name")
	private String userName;
	
	@Column(name = "email")
	private String email;
	
	@Column(name = "password_hash")
	private String passwordHash;
	
	@Column(name = "full_name")
	private String fullName;
	
	@Column(name = "phone")
	private String phone;
	
	@Column(name = "locality")
	private String locality;
	
	@Column(name = "address")
	private String address;
	
	@Column(name = "is_active")
	@Builder.Default
	private boolean isActive = true;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	@Builder.Default
	private Role role = Role.CUSTOMER;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "auth_provider")
	@Builder.Default
	private AuthProvider authProvider = AuthProvider.LOCAL;
	
	@Column(name = "email_verified")
	@Builder.Default
	private Boolean emailVerified = false;
}
