package com.quickbite.notification.entity;

/**
 * Notification type — determines which recipients get the alert
 * and what icon/colour is displayed in the notification centre.
 */
public enum NotificationType {
    ORDER,      // Order lifecycle events (placed, confirmed, preparing, picked-up, delivered)
    PAYMENT,    // Payment receipt, refund issued
    PROMO,      // Promotional / marketing broadcast from admin
    DELIVERY    // Delivery-agent-specific events (assigned, completed)
}
