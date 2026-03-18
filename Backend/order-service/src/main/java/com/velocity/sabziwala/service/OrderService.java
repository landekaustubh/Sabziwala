package com.velocity.sabziwala.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velocity.sabziwala.dto.CreateOrderRequest;
import com.velocity.sabziwala.dto.OrderItemRequest;
import com.velocity.sabziwala.dto.OrderResponse;
import com.velocity.sabziwala.dto.UpdateOrderStatusRequest;
import com.velocity.sabziwala.entity.Order;
import com.velocity.sabziwala.entity.OrderItem;
import com.velocity.sabziwala.enums.OrderStatus;
import com.velocity.sabziwala.enums.PaymentStatus;
import com.velocity.sabziwala.exception.InvalidOrderStateException;
import com.velocity.sabziwala.exception.OrderNotFoundException;
import com.velocity.sabziwala.exception.UnauthorizedOrderAccessException;
import com.velocity.sabziwala.repository.OrderRepository;
import com.velocity.sabziwala.security.UserPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    /** Delivery charge: free above ₹200, else ₹30 */
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("200");
    private static final BigDecimal DELIVERY_CHARGE = new BigDecimal("30");

    /** Valid status transitions */
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING,           Set.of(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED,         Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING,         Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED),
            OrderStatus.OUT_FOR_DELIVERY,  Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED,         Set.of(),
            OrderStatus.CANCELLED,         Set.of(), 
            OrderStatus.PAYMENT_FAILED,    Set.of(OrderStatus.PENDING)  // retry
    );

    // ════════════════════════════════════════════════════════════
    //  CREATE ORDER (CUSTOMER)
    // ════════════════════════════════════════════════════════════

    /**
     * Create a new order. Called by CUSTOMER.
     *
     * @param request  Order items, address, etc.
     * @param principal  The authenticated user from JWT — contains userId, email, role
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, UserPrincipal principal) {
        log.info("Creating order for user: {} ({})", principal.getEmail(), principal.getUserId());

        // Build order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(principal.getUserId())
                .userEmail(principal.getEmail())
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .deliveryAddress(request.getDeliveryAddress())
                .locality(request.getLocality())
                .build();

        // Add items and calculate totals
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            BigDecimal subtotal = itemReq.getPricePerUnit()
                    .multiply(itemReq.getQuantity());

            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .pricePerUnit(itemReq.getPricePerUnit())
                    .quantity(itemReq.getQuantity())
                    .unit(itemReq.getUnit())
                    .subtotal(subtotal)
                    .build();

            order.addItem(item);
            totalAmount = totalAmount.add(subtotal);
        }

        // Delivery charge calculation
        BigDecimal deliveryCharge = totalAmount.compareTo(FREE_DELIVERY_THRESHOLD) >= 0
                ? BigDecimal.ZERO : DELIVERY_CHARGE;

        order.setTotalAmount(totalAmount);
        order.setDeliveryCharge(deliveryCharge);
        order.setGrandTotal(totalAmount.add(deliveryCharge));
      
        Order saved = orderRepository.save(order);
        log.info("Order created: {} (total: ₹{})", saved.getOrderNumber(), saved.getGrandTotal());

        return OrderResponse.from(saved);
    }

    // ════════════════════════════════════════════════════════════
    //  GET ORDER BY ID
    // ════════════════════════════════════════════════════════════

    /**
     * Get order details with ownership check.
     * - CUSTOMER: can only view own orders
     * - ADMIN: can view any order
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, UserPrincipal principal) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with ID: " + orderId));

        // Ownership check: CUSTOMER can only see their own orders
        enforceOwnership(order, principal);

        return OrderResponse.from(order);
    }

    // ════════════════════════════════════════════════════════════
    //  LIST ORDERS (MY ORDERS for CUSTOMER, ALL ORDERS for ADMIN)
    // ════════════════════════════════════════════════════════════

    /**
     * CUSTOMER: Returns only their own orders.
     * Paginated, sorted by creation date descending.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(UserPrincipal principal, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(principal.getUserId(), pageable)
                .map(OrderResponse::summary);
    }

    /**
     * ADMIN: Returns all orders, with optional status filter.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderStatus statusFilter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (statusFilter != null) {
            return orderRepository
                    .findByStatusOrderByCreatedAtDesc(statusFilter, pageable)
                    .map(OrderResponse::summary);
        }
        return orderRepository.findAll(pageable).map(OrderResponse::summary);
    }

    // ════════════════════════════════════════════════════════════
    //  UPDATE STATUS (ADMIN only)
    // ════════════════════════════════════════════════════════════

    /**
     * Update order status with transition validation.
     * Only ADMIN can call this (enforced by @PreAuthorize on controller).
     */
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request,
                                           UserPrincipal principal) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with ID: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        // Validate transition
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidOrderStateException(
                    String.format("Cannot transition from %s to %s. Allowed: %s",
                            currentStatus, newStatus, allowed));
        }

        // Apply status change
        order.setStatus(newStatus);

        // Handle side effects
        if (newStatus == OrderStatus.CONFIRMED) {
            order.setPaymentStatus(PaymentStatus.PAID);
        } else if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        } else if (newStatus == OrderStatus.CANCELLED) {
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        } else if (newStatus == OrderStatus.PAYMENT_FAILED) {
            order.setPaymentStatus(PaymentStatus.FAILED);
        }

        Order updated = orderRepository.save(order);
        log.info("Order {} status: {} → {} (by {})",
                order.getOrderNumber(), currentStatus, newStatus, principal.getEmail());

        return OrderResponse.from(updated);
    }

    // ════════════════════════════════════════════════════════════
    //  CANCEL ORDER
    // ════════════════════════════════════════════════════════════

    /**
     * Cancel order.
     * - CUSTOMER: can cancel only PENDING or CONFIRMED orders they own
     * - ADMIN: can cancel any cancellable order
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UserPrincipal principal) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with ID: " + orderId));

        // CUSTOMER ownership check
        if ("CUSTOMER".equals(principal.getRole())) {
            enforceOwnership(order, principal);
        }

        // Can only cancel PENDING or CONFIRMED
        if (order.getStatus() != OrderStatus.PENDING &&
            order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(
                    "Cannot cancel order in " + order.getStatus() + " status. " +
                    "Only PENDING or CONFIRMED orders can be cancelled.");
        }
        order.setStatus(OrderStatus.CANCELLED);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        Order cancelled = orderRepository.save(order);
        log.info("Order {} cancelled by {} ({})",
                order.getOrderNumber(), principal.getEmail(), principal.getRole());

        return OrderResponse.from(cancelled);
    }

    // ════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════

    /**
     * Enforce ownership: CUSTOMER can only access their own orders.
     * ADMIN bypasses this check.
     */
    private void enforceOwnership(Order order, UserPrincipal principal) {
        if ("ADMIN".equals(principal.getRole())) {
            return;  // Admin can access any order
        }
        if (!order.getUserId().equals(principal.getUserId())) {
            throw new UnauthorizedOrderAccessException(
                    "You can only access your own orders");
        }
    }

    /**
     * Generate human-readable order number: ORD-2025-XXXXX
     */
    private String generateOrderNumber() {
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "ORD-" + Year.now().getValue() + "-" + random;
    }
}
