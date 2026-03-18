package com.velocity.sabziwala.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItemRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Product name is required")
    @Size(max = 200)
    private String productName;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal pricePerUnit;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotBlank(message = "Unit is required")
    @Pattern(regexp = "^(KG|GRAM|PIECE|DOZEN|BUNCH)$", message = "Unit must be KG, GRAM, PIECE, DOZEN, or BUNCH")
    private String unit;
}
