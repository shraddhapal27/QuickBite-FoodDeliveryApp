package com.quickbite.notification.dto;

import com.quickbite.notification.entity.NotificationChannel;
import com.quickbite.notification.entity.NotificationType;
import lombok.Data;

/**
 * Request body for sending a single notification.
 */
@Data
public class NotificationRequestDTO {
    private Long recipientId;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationChannel channel;
    /** FK to related entity (orderId, paymentId, etc.) */
    private Long relatedId;
    private String relatedType;
    private String deepLinkUrl;
}
