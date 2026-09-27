package com.bankflow.notification.controller;

import com.bankflow.common.enums.NotificationChannel;
import com.bankflow.notification.channel.InAppPushSender;
import com.bankflow.notification.model.NotificationRecord;
import com.bankflow.notification.service.NotificationDispatcher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationDispatcher dispatcher;
    private final InAppPushSender pushSender;

    public NotificationController(NotificationDispatcher dispatcher, InAppPushSender pushSender) {
        this.dispatcher = dispatcher;
        this.pushSender = pushSender;
    }

    /**
     * Get all dispatched notifications and delivery statuses.
     */
    @GetMapping
    public ResponseEntity<List<NotificationRecord>> getNotificationHistory() {
        return ResponseEntity.ok(dispatcher.getHistory());
    }

    /**
     * Get active in-app notifications stream.
     */
    @GetMapping("/feed")
    public ResponseEntity<List<NotificationRecord>> getInAppFeed() {
        return ResponseEntity.ok(new ArrayList<>(pushSender.getInAppFeed()));
    }

    /**
     * Get notifications for a specific customer or account.
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<NotificationRecord>> getCustomerNotifications(@PathVariable("customerId") String customerId) {
        List<NotificationRecord> list = dispatcher.getHistory().stream()
                .filter(n -> customerId.equalsIgnoreCase(n.getCustomerId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /**
     * Test endpoint to trigger a notification and observe exponential backoff retries.
     */
    @PostMapping("/test")
    public ResponseEntity<NotificationRecord> sendTestNotification(@RequestBody Map<String, String> payload) {
        String channelStr = payload.getOrDefault("channel", "SMS").toUpperCase();
        NotificationChannel channel = NotificationChannel.valueOf(channelStr);
        String recipient = payload.getOrDefault("recipient", "+919876543210");
        String subject = payload.getOrDefault("subject", "Test Notification");
        String body = payload.getOrDefault("body", "This is an automated test message from Bankflow notification service.");

        NotificationRecord record = new NotificationRecord(
            "test-" + UUID.randomUUID().toString().substring(0, 8),
            recipient,
            "cust-test",
            channel,
            subject,
            body
        );

        try {
            dispatcher.dispatch(record);
        } catch (Exception ex) {
            // Handled by recover
        }

        return ResponseEntity.ok(record);
    }
}
