package com.quickbite.notification.entity;

/**
 * Delivery channel for a notification.
 */
public enum NotificationChannel {
    APP,    // In-app notification centre (stored in DB, surfaced via REST)
    EMAIL,  // Sent via JavaMailSender (SMTP)
    SMS     // Sent via Twilio / AWS SNS (stub implementation)
}
