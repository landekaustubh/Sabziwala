package com.velocity.sabziwala.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String deliveryAddress;

    @Size(max = 100)
    private String locality;       // Pune locality: Kothrud, Baner, etc.
}
