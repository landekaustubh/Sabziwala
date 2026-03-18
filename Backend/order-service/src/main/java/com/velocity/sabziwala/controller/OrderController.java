package com.velocity.sabziwala.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.velocity.sabziwala.dto.ApiResponse;
import com.velocity.sabziwala.dto.CreateOrderRequest;
import com.velocity.sabziwala.dto.OrderResponse;
import com.velocity.sabziwala.dto.UpdateOrderStatusRequest;
import com.velocity.sabziwala.enums.OrderStatus;
import com.velocity.sabziwala.security.UserPrincipal;
import com.velocity.sabziwala.service.OrderService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // ════════════════════════════════════════════════════════════
    //  POST /orders — CREATE ORDER
    //  Allowed: CUSTOMER only
    //  ADMIN and DELIVERY → 403 Forbidden
    // ════════════════════════════════════════════════════════════

    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[CREATE ORDER] User: {} (role: {})", principal.getEmail(), principal.getRole());

        OrderResponse order = orderService.createOrder(request, principal);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully!", order));
    }

    // ════════════════════════════════════════════════════════════
    //  GET /orders/my — MY ORDERS
    //  Allowed: CUSTOMER only
    //  Returns only orders belonging to the logged-in customer
    // ════════════════════════════════════════════════════════════

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("[MY ORDERS] User: {}", principal.getEmail());

        Page<OrderResponse> orders = orderService.getMyOrders(principal, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Your orders retrieved", orders));
    }

    // ════════════════════════════════════════════════════════════
    //  GET /orders/all — ALL ORDERS (ADMIN PANEL)
    //  Allowed: ADMIN only
    //  CUSTOMER and DELIVERY → 403 Forbidden
    // ════════════════════════════════════════════════════════════

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("[ALL ORDERS] status filter: {}", status);

        Page<OrderResponse> orders = orderService.getAllOrders(status, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("All orders retrieved", orders));
    }

    // ════════════════════════════════════════════════════════════
    //  GET /orders/{id} — ORDER DETAILS
    //  Allowed: CUSTOMER + ADMIN
    //  CUSTOMER can only see OWN orders (ownership check in service)
    //  ADMIN can see ANY order
    // ════════════════════════════════════════════════════════════

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[GET ORDER] orderId: {}, user: {} ({})",
                orderId, principal.getEmail(), principal.getRole());

        OrderResponse order = orderService.getOrderById(orderId, principal);

        return ResponseEntity.ok(
                ApiResponse.success("Order details retrieved", order));
    }

    // ════════════════════════════════════════════════════════════
    //  PUT /orders/{id}/status — UPDATE STATUS
    //  Allowed: ADMIN only
    //  Used for: CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
    // ════════════════════════════════════════════════════════════

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[UPDATE STATUS] orderId: {}, newStatus: {}, admin: {}",
                orderId, request.getStatus(), principal.getEmail());

        OrderResponse order = orderService.updateOrderStatus(orderId, request, principal);

        return ResponseEntity.ok(
                ApiResponse.success("Order status updated to " + request.getStatus(), order));
    }

    // ════════════════════════════════════════════════════════════
    //  DELETE /orders/{id} — CANCEL ORDER
    //  Allowed: CUSTOMER + ADMIN (different rules)
    //  CUSTOMER: can cancel only own PENDING/CONFIRMED orders
    //  ADMIN: can cancel any cancellable order
    // ════════════════════════════════════════════════════════════

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[CANCEL ORDER] orderId: {}, user: {} ({})",
                orderId, principal.getEmail(), principal.getRole());

        OrderResponse order = orderService.cancelOrder(orderId, principal);

        return ResponseEntity.ok(
                ApiResponse.success("Order cancelled successfully", order));
    }
}
