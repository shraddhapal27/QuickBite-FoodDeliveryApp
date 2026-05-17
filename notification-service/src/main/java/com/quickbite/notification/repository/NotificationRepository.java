package com.quickbite.notification.repository;

import com.quickbite.notification.entity.Notification;
import com.quickbite.notification.entity.NotificationChannel;
import com.quickbite.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** All notifications for a recipient, newest first */
    List<Notification> findByRecipientIdOrderBySentAtDesc(Long recipientId);

    /** Unread-only notifications for a recipient */
    List<Notification> findByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    /** Unread badge count for the nav-bar */
    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    /** All notifications of a specific type (e.g. ORDER) */
    List<Notification> findByType(NotificationType type);

    /** All notifications linked to a specific entity (e.g. all alerts for orderId=5) */
    List<Notification> findByRelatedId(Long relatedId);

    /** All notifications sent via a specific channel */
    List<Notification> findByChannel(NotificationChannel channel);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    void markAllAsRead(@org.springframework.data.repository.query.Param("recipientId") Long recipientId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByNotificationId(Long notificationId);
}
