package com.quickbite.notification.resource;

import com.quickbite.notification.dto.BulkNotificationRequestDTO;
import com.quickbite.notification.dto.NotificationRequestDTO;
import com.quickbite.notification.entity.Notification;
import com.quickbite.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Resource for the Notification-Service.
 * Base path: /notifications
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app, email, and SMS notification management")
public class NotificationResource {

    private final NotificationService notificationService;

    @Operation(summary = "Send a notification")
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody NotificationRequestDTO dto) {
        try {
            return new ResponseEntity<>(notificationService.send(dto), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Send bulk notifications (Admin)")
    @PostMapping("/send/bulk")
    public ResponseEntity<?> sendBulk(@RequestBody BulkNotificationRequestDTO dto) {
        try {
            return new ResponseEntity<>(notificationService.sendBulk(dto), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get notifications by recipient")
    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Notification>> getByRecipient(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    @Operation(summary = "Get unread notifications")
    @GetMapping("/recipient/{recipientId}/unread")
    public ResponseEntity<List<Notification>> getUnread(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getUnread(recipientId));
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/recipient/{recipientId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(recipientId));
    }

    @Operation(summary = "Get all notifications (Admin)")
    @GetMapping
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @Operation(summary = "Mark notification as read")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        try {
            return ResponseEntity.ok(notificationService.markAsRead(notificationId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Mark all notifications as read")
    @PutMapping("/recipient/{recipientId}/read-all")
    public ResponseEntity<String> markAllRead(@PathVariable Long recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok("All notifications marked as read.");
    }

    @Operation(summary = "Delete a notification")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String> deleteNotification(@PathVariable Long notificationId) {
        try {
            notificationService.deleteNotification(notificationId);
            return ResponseEntity.ok("Notification deleted.");
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
