package com.velocity.sabziwala.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_item_id", updatable = false, nullable = false)
    private UUID orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_oi_order"))
    private Order order;

    /** Product ID from Product Service (MongoDB ObjectId as string) */
    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    /** Denormalized — avoids calling Product Service for display */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "price_per_unit", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerUnit;

    @Column(name = "quantity", nullable = false, precision = 8, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;   // KG, PIECE, DOZEN, BUNCH

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
