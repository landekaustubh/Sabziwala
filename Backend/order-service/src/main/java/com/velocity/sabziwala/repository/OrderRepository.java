package com.velocity.sabziwala.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.velocity.sabziwala.entity.Order;
import com.velocity.sabziwala.enums.OrderStatus;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** Find orders by userId — CUSTOMER sees only their own orders */
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Find all orders with optional status filter — ADMIN view */
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    /** Find order with items eagerly loaded */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderId = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    /** Find order with items + history eagerly loaded */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.items " +
           "WHERE o.orderId = :id")
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    /** Check if order belongs to a user — for ownership validation */
    boolean existsByOrderIdAndUserId(UUID orderId, UUID userId);
}
