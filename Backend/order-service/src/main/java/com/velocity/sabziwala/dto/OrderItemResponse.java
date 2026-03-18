package com.velocity.sabziwala.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

import com.velocity.sabziwala.entity.OrderItem;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItemResponse {

    private UUID orderItemId;
    private String productId;
    private String productName;
    private BigDecimal pricePerUnit;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal subtotal;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .pricePerUnit(item.getPricePerUnit())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .subtotal(item.getSubtotal())
                .build();
    }
}
