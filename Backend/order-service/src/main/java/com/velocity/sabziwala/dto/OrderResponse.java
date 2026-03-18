package com.velocity.sabziwala.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.velocity.sabziwala.entity.Order;
import com.velocity.sabziwala.enums.OrderStatus;
import com.velocity.sabziwala.enums.PaymentStatus;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private UUID orderId;
    private String orderNumber;
    private UUID userId;
    private String userEmail;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal deliveryCharge;
    private BigDecimal grandTotal;
    private String deliveryAddress;
    private String locality;
    private String deliverySlot;
    private String notes;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deliveredAt;

    /**
     * Map entity to response DTO.
     */
    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .deliveryCharge(order.getDeliveryCharge())
                .grandTotal(order.getGrandTotal())
                .deliveryAddress(order.getDeliveryAddress())
                .locality(order.getLocality())
                .items(order.getItems().stream()
                        .map(OrderItemResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .deliveredAt(order.getDeliveredAt())
                .build();
    }

    /** Lightweight version without items/history (for list endpoints). */
    public static OrderResponse summary(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .grandTotal(order.getGrandTotal())
                .locality(order.getLocality())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
