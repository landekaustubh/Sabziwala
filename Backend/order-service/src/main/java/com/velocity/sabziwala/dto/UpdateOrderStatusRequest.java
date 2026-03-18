package com.velocity.sabziwala.dto;

import com.velocity.sabziwala.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request to update order status — ADMIN only via @PreAuthorize.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    @NotNull(message = "New status is required")
    private OrderStatus status;

    @Size(max = 500)
    private String remarks;   // Reason for status change
}
