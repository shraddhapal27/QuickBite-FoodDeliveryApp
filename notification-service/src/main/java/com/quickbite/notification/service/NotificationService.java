package com.quickbite.notification.service;

import com.quickbite.notification.dto.BulkNotificationRequestDTO;
import com.quickbite.notification.dto.NotificationRequestDTO;
import com.quickbite.notification.entity.Notification;

import java.util.List;

/**
 * Service contract for the Notification-Service.
 * Handles single/bulk dispatch, read-state management, retrieval, and deletion.
 */
public interface NotificationService {

    // ── Dispatch ────────────────────────────────────────────────────────────

    /**
     * Send a single notification to one recipient.
     * Persists the record and dispatches to the appropriate channel (APP / EMAIL / SMS).
     */
    Notification send(NotificationRequestDTO dto);

    /**
     * Bulk-send the same notification to a list of recipients (admin broadcast).
     * Returns the list of saved Notification records.
     */
    List<Notification> sendBulk(BulkNotificationRequestDTO dto);

    // ── Retrieval ───────────────────────────────────────────────────────────

    /** All notifications for a recipient, ordered newest-first. */
    List<Notification> getByRecipient(Long recipientId);

    /** Unread notifications only for a recipient. */
    List<Notification> getUnread(Long recipientId);

    /** Unread badge count — used by the nav-bar real-time counter. */
    long getUnreadCount(Long recipientId);

    /** Admin: get all notifications on the platform. */
    List<Notification> getAll();

    // ── Read-State Management ───────────────────────────────────────────────

    /** Mark a single notification as read. */
    Notification markAsRead(Long notificationId);

    /** Mark all notifications for a recipient as read (e.g. "Mark all read" button). */
    void markAllRead(Long recipientId);

    // ── Deletion ────────────────────────────────────────────────────────────

    /** Delete a single notification record. */
    void deleteNotification(Long notificationId);

    // ── Channel Dispatch Helpers ────────────────────────────────────────────

    /** Send an email via JavaMailSender. */
    void sendEmail(String toEmail, String subject, String body);

    /** Send an SMS via Twilio / AWS SNS stub. */
    void sendSMS(String toPhone, String message);
}
