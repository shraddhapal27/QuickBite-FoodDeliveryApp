package com.quickbite.notification.dto;

import com.quickbite.notification.entity.NotificationChannel;
import com.quickbite.notification.entity.NotificationType;
import lombok.Data;

import java.util.List;

/**
 * Request body for broadcasting a notification to multiple recipients at once.
 * Used by admin for platform-wide promotions or maintenance alerts.
 */
@Data
public class BulkNotificationRequestDTO {
    /** List of recipientIds to send to. If empty, the service broadcasts to all users. */
    private List<Long> recipientIds;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationChannel channel;
    private String deepLinkUrl;
}
