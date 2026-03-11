package com.velocity.sabziwala.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class RegisterRequest {
	@NotBlank(message = "Username is Required")
	private String userName;

	@NotBlank(message = "email is Required")
	@Email(message = "Please provide a Valid Email address")
	private String email;

	@NotBlank(message = "password is Required")
	@Size(min = 8, max = 8, message = "Password should be of 8 Charaters")
	@Pattern(regexp = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$", message = "Password must contain one uppercase,one lowercase,one digit,one special character")
	private String password;

	@NotBlank(message = "Full Name is Required")
	private String fullName;

	@NotBlank(message = "Phone Number is Required")
	@Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$", message = "Pls provide the valid Mobile Number")
	private String phone;

	@NotBlank(message = "Locality is Required")
	private String locality;
}
