package com.bankflow.notification.channel;

import com.bankflow.common.enums.NotificationChannel;
import com.bankflow.notification.model.NotificationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-App Push Notification Dispatcher.
 * Holds active in-app notifications for web and mobile clients.
 */
@Component
public class InAppPushSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(InAppPushSender.class);

    private final Deque<NotificationRecord> inAppFeed = new ConcurrentLinkedDeque<>();
    private static final int MAX_FEED_SIZE = 100;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public void send(NotificationRecord record) {
        log.info("[IN-APP PUSH] Broadcasting push notification to user {}: '{}'", record.getCustomerId(), record.getSubject());
        record.setStatus("DELIVERED");
        record.setDeliveredAt(Instant.now());

        inAppFeed.addFirst(record);
        if (inAppFeed.size() > MAX_FEED_SIZE) {
            inAppFeed.removeLast();
        }
    }

    public Deque<NotificationRecord> getInAppFeed() {
        return inAppFeed;
    }
}
