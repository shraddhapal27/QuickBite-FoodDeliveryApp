package com.quickbite.notification.service.impl;

import com.quickbite.notification.dto.BulkNotificationRequestDTO;
import com.quickbite.notification.dto.NotificationRequestDTO;
import com.quickbite.notification.entity.Notification;
import com.quickbite.notification.entity.NotificationChannel;
import com.quickbite.notification.repository.NotificationRepository;
import com.quickbite.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;  // may be null if SMTP is misconfigured

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
        if (mailSender == null) {
            log.warn("JavaMailSender not available — email notifications will be skipped.");
        }
    }

    // ── Dispatch ────────────────────────────────────────────────────────────

    @Override
    public Notification send(NotificationRequestDTO dto) {
        Notification notification = Notification.builder()
                .recipientId(dto.getRecipientId())
                .type(dto.getType())
                .title(dto.getTitle())
                .message(dto.getMessage())
                .channel(dto.getChannel())
                .relatedId(dto.getRelatedId())
                .relatedType(dto.getRelatedType())
                .deepLinkUrl(dto.getDeepLinkUrl())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Dispatch to external channels asynchronously (best-effort)
        if (dto.getChannel() == NotificationChannel.EMAIL && dto.getDeepLinkUrl() != null) {
            try {
                sendEmail(dto.getDeepLinkUrl(), dto.getTitle(), dto.getMessage());
            } catch (Exception e) {
                log.warn("Email dispatch failed for recipientId={}: {}", dto.getRecipientId(), e.getMessage());
            }
        } else if (dto.getChannel() == NotificationChannel.SMS) {
            try {
                // Phone number lookup would come from auth-service in a real integration
                sendSMS("RECIPIENT_PHONE", dto.getMessage());
            } catch (Exception e) {
                log.warn("SMS dispatch failed for recipientId={}: {}", dto.getRecipientId(), e.getMessage());
            }
        }

        return saved;
    }

    @Override
    public List<Notification> sendBulk(BulkNotificationRequestDTO dto) {
        if (dto.getRecipientIds() == null || dto.getRecipientIds().isEmpty()) {
            throw new IllegalArgumentException("recipientIds must not be empty for bulk send.");
        }

        List<Notification> saved = new ArrayList<>();
        for (Long recipientId : dto.getRecipientIds()) {
            NotificationRequestDTO single = new NotificationRequestDTO();
            single.setRecipientId(recipientId);
            single.setType(dto.getType());
            single.setTitle(dto.getTitle());
            single.setMessage(dto.getMessage());
            single.setChannel(dto.getChannel());
            single.setDeepLinkUrl(dto.getDeepLinkUrl());
            saved.add(send(single));
        }
        return saved;
    }

    // ── Retrieval ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderBySentAtDesc(recipientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnread(Long recipientId) {
        return notificationRepository.findByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }

    // ── Read-State Management ───────────────────────────────────────────────

    @Override
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    public void markAllRead(Long recipientId) {
        notificationRepository.markAllAsRead(recipientId);
    }

    // ── Deletion ────────────────────────────────────────────────────────────

    @Override
    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new IllegalArgumentException("Notification not found: " + notificationId);
        }
        notificationRepository.deleteByNotificationId(notificationId);
    }

    // ── Channel Dispatch Helpers ────────────────────────────────────────────

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        if (mailSender == null) {
            log.warn("[EMAIL SKIP] No mail sender configured. Would send to {}: {}", toEmail, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendSMS(String toPhone, String message) {
        // Stub — replace with Twilio SDK or AWS SNS client in production
        log.info("[SMS STUB] To: {} | Message: {}", toPhone, message);
    }
}
