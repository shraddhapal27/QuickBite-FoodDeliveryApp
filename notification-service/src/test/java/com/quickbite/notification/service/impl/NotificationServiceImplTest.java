package com.quickbite.notification.service.impl;


import com.quickbite.notification.dto.BulkNotificationRequestDTO;
import com.quickbite.notification.dto.NotificationRequestDTO;
import com.quickbite.notification.entity.Notification;
import com.quickbite.notification.entity.NotificationChannel;
import com.quickbite.notification.entity.NotificationType;
import com.quickbite.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private JavaMailSender mailSender;
    @InjectMocks private NotificationServiceImpl notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .notificationId(1L).recipientId(10L)
                .type(NotificationType.ORDER).title("Order Placed!")
                .message("Your order #100 has been placed.")
                .channel(NotificationChannel.APP)
                .relatedId(100L).relatedType("Order")
                .deepLinkUrl("/orders").isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
    }

    // ── Send Notification ──

    @Nested
    @DisplayName("Send Notification")
    class SendTests {

        @Test
        @DisplayName("send – APP channel notification persisted")
        void send_appChannel() {
            NotificationRequestDTO dto = new NotificationRequestDTO();
            dto.setRecipientId(10L); dto.setType(NotificationType.ORDER);
            dto.setTitle("Order Placed!"); dto.setMessage("Your order #100 has been placed.");
            dto.setChannel(NotificationChannel.APP);
            dto.setRelatedId(100L); dto.setRelatedType("Order"); dto.setDeepLinkUrl("/orders");

            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

            Notification result = notificationService.send(dto);
            assertThat(result.getRecipientId()).isEqualTo(10L);
            assertThat(result.getIsRead()).isFalse();
            verify(notificationRepository).save(any(Notification.class));
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("send – EMAIL channel dispatches email")
        void send_emailChannel() {
            NotificationRequestDTO dto = new NotificationRequestDTO();
            dto.setRecipientId(10L); dto.setType(NotificationType.PAYMENT);
            dto.setTitle("Payment Received"); dto.setMessage("₹500 paid.");
            dto.setChannel(NotificationChannel.EMAIL);
            dto.setDeepLinkUrl("user@test.com");

            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

            notificationService.send(dto);
            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("send – EMAIL channel without deepLinkUrl skips email")
        void send_emailNoUrl() {
            NotificationRequestDTO dto = new NotificationRequestDTO();
            dto.setRecipientId(10L); dto.setType(NotificationType.PROMO);
            dto.setTitle("Sale!"); dto.setMessage("50% off!");
            dto.setChannel(NotificationChannel.EMAIL);
            dto.setDeepLinkUrl(null);

            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

            notificationService.send(dto);
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("send – EMAIL failure does not throw (best-effort)")
        void send_emailFailure() {
            NotificationRequestDTO dto = new NotificationRequestDTO();
            dto.setRecipientId(10L); dto.setType(NotificationType.ORDER);
            dto.setTitle("Test"); dto.setMessage("msg");
            dto.setChannel(NotificationChannel.EMAIL);
            dto.setDeepLinkUrl("bad@email.com");

            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);
            doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

            // Should not throw — email failure is best-effort
            assertThatCode(() -> notificationService.send(dto)).doesNotThrowAnyException();
        }
    }

    // ── Bulk Send ──

    @Nested
    @DisplayName("Bulk Send")
    class BulkTests {

        @Test
        @DisplayName("sendBulk – sends to all recipients")
        void sendBulk_success() {
            BulkNotificationRequestDTO dto = new BulkNotificationRequestDTO();
            dto.setRecipientIds(List.of(10L, 20L, 30L));
            dto.setType(NotificationType.PROMO);
            dto.setTitle("Sale!"); dto.setMessage("50% off!");
            dto.setChannel(NotificationChannel.APP);

            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

            List<Notification> results = notificationService.sendBulk(dto);
            assertThat(results).hasSize(3);
            verify(notificationRepository, times(3)).save(any(Notification.class));
        }

        @Test
        @DisplayName("sendBulk – empty recipients throws")
        void sendBulk_emptyRecipients() {
            BulkNotificationRequestDTO dto = new BulkNotificationRequestDTO();
            dto.setRecipientIds(new ArrayList<>());

            assertThatThrownBy(() -> notificationService.sendBulk(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be empty");
        }

        @Test
        @DisplayName("sendBulk – null recipients throws")
        void sendBulk_nullRecipients() {
            BulkNotificationRequestDTO dto = new BulkNotificationRequestDTO();
            dto.setRecipientIds(null);

            assertThatThrownBy(() -> notificationService.sendBulk(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Retrieval ──

    @Test
    @DisplayName("getByRecipient – returns ordered notifications")
    void getByRecipient() {
        when(notificationRepository.findByRecipientIdOrderBySentAtDesc(10L))
                .thenReturn(List.of(sampleNotification));
        assertThat(notificationService.getByRecipient(10L)).hasSize(1);
    }

    @Test
    @DisplayName("getUnread – returns unread only")
    void getUnread() {
        when(notificationRepository.findByRecipientIdAndIsRead(10L, false))
                .thenReturn(List.of(sampleNotification));
        assertThat(notificationService.getUnread(10L)).hasSize(1);
    }

    @Test
    @DisplayName("getUnreadCount – returns count")
    void getUnreadCount() {
        when(notificationRepository.countByRecipientIdAndIsRead(10L, false)).thenReturn(5L);
        assertThat(notificationService.getUnreadCount(10L)).isEqualTo(5L);
    }

    @Test
    @DisplayName("getAll – returns all notifications")
    void getAll() {
        when(notificationRepository.findAll()).thenReturn(List.of(sampleNotification));
        assertThat(notificationService.getAll()).hasSize(1);
    }

    // ── Read-State Management ──

    @Test
    @DisplayName("markAsRead – sets isRead to true")
    void markAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.markAsRead(1L);
        assertThat(result.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("markAsRead – not found throws")
    void markAsRead_notFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.markAsRead(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("markAllRead – marks all unread as read")
    void markAllRead() {
        Notification n1 = Notification.builder().notificationId(1L).recipientId(10L)
                .type(NotificationType.ORDER).title("t1").message("m1")
                .channel(NotificationChannel.APP).isRead(false).build();
        Notification n2 = Notification.builder().notificationId(2L).recipientId(10L)
                .type(NotificationType.ORDER).title("t2").message("m2")
                .channel(NotificationChannel.APP).isRead(false).build();

        when(notificationRepository.findByRecipientIdAndIsRead(10L, false))
                .thenReturn(List.of(n1, n2));

        notificationService.markAllRead(10L);
        assertThat(n1.getIsRead()).isTrue();
        assertThat(n2.getIsRead()).isTrue();
        verify(notificationRepository).saveAll(anyList());
    }

    // ── Deletion ──

    @Test
    @DisplayName("deleteNotification – deletes existing")
    void deleteNotification_success() {
        when(notificationRepository.existsById(1L)).thenReturn(true);
        notificationService.deleteNotification(1L);
        verify(notificationRepository).deleteByNotificationId(1L);
    }

    @Test
    @DisplayName("deleteNotification – not found throws")
    void deleteNotification_notFound() {
        when(notificationRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> notificationService.deleteNotification(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
