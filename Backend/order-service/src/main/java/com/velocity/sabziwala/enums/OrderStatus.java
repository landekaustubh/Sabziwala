package com.velocity.sabziwala.enums;

public enum OrderStatus {
    PENDING,              // Order created, awaiting payment
    CONFIRMED,            // Payment successful, order confirmed
    PREPARING,            // Vendor is preparing the order
    OUT_FOR_DELIVERY,     // Delivery partner picked up
    DELIVERED,            // Successfully delivered to customer
    CANCELLED,            // Cancelled by customer or admin
    PAYMENT_FAILED        // Payment was unsuccessful
}
