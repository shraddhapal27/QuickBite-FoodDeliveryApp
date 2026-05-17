package com.quickbite.notification.listener;

import com.quickbite.notification.config.RabbitMQConfig;
import com.quickbite.notification.dto.NotificationRequestDTO;
import com.quickbite.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void receiveNotificationMessage(NotificationRequestDTO requestDTO) {
        log.info("Received notification message from RabbitMQ: {}", requestDTO);
        try {
            notificationService.send(requestDTO);
            log.info("Notification sent successfully via RabbitMQ listener.");
        } catch (Exception e) {
            log.error("Error processing notification message: {}", e.getMessage(), e);
        }
    }
}
