package com.quickbite.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores a single notification record for the in-app notification centre.
 * EMAIL and SMS channels are dispatched immediately and also persisted here
 * for audit and unread-count tracking.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_recipient_read", columnList = "recipientId, isRead")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    /** ID of the user (customerId / agentId / ownerId) who receives this notification */
    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    /** FK to the related entity (e.g. orderId, paymentId) */
    private Long relatedId;

    /** E.g. "ORDER", "PAYMENT" — describes what relatedId refers to */
    private String relatedType;

    /** Deep-link URL for the frontend to navigate on tap */
    private String deepLinkUrl;

    @Builder.Default
    private Boolean isRead = false;

    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) sentAt = LocalDateTime.now();
    }
}
