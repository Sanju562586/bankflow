package com.bankflow.notification.service;

import com.bankflow.common.enums.NotificationChannel;
import com.bankflow.notification.channel.NotificationChannelSender;
import com.bankflow.notification.exception.DeliveryException;
import com.bankflow.notification.model.NotificationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final Map<NotificationChannel, NotificationChannelSender> senders = new EnumMap<>(NotificationChannel.class);
    private final Deque<NotificationRecord> history = new ConcurrentLinkedDeque<>();
    private static final int MAX_HISTORY = 200;

    public NotificationDispatcher(List<NotificationChannelSender> channelSenders) {
        for (NotificationChannelSender sender : channelSenders) {
            senders.put(sender.getChannel(), sender);
        }
    }

    /**
     * Dispatch notification with automated exponential backoff retry on delivery failure.
     * Backoff: initial 1000ms, multiplier 2.0, maxDelay 8000ms, maxAttempts 3.
     */
    @Retryable(
        retryFor = { DeliveryException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 8000)
    )
    public void dispatch(NotificationRecord record) {
        record.setAttemptCount(record.getAttemptCount() + 1);
        log.info("Attempting dispatch for notification id={}, channel={}, attempt={}", 
                 record.getId(), record.getChannel(), record.getAttemptCount());

        NotificationChannelSender sender = senders.get(record.getChannel());
        if (sender == null) {
            log.warn("No sender registered for channel {}", record.getChannel());
            record.setStatus("UNSUPPORTED_CHANNEL");
            record.setFailureReason("No sender found for channel: " + record.getChannel());
            recordHistory(record);
            return;
        }

        try {
            sender.send(record);
            recordHistory(record);
        } catch (DeliveryException ex) {
            record.setStatus("RETRYING");
            record.setFailureReason(ex.getMessage());
            log.warn("Delivery attempt {} failed for notification {}: {}. Triggering backoff retry...", 
                     record.getAttemptCount(), record.getId(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * Fallback recovery method executed if all retry attempts are exhausted.
     */
    @Recover
    public void recover(DeliveryException ex, NotificationRecord record) {
        log.error("All delivery retry attempts exhausted for notification id={}. Final status: FAILED. Error: {}", 
                  record.getId(), ex.getMessage());
        record.setStatus("FAILED");
        record.setFailureReason("Retry limit reached. " + ex.getMessage());
        recordHistory(record);
    }

    private void recordHistory(NotificationRecord record) {
        history.removeIf(r -> r.getId().equals(record.getId()));
        history.addFirst(record);
        if (history.size() > MAX_HISTORY) {
            history.removeLast();
        }
    }

    public List<NotificationRecord> getHistory() {
        return new ArrayList<>(history);
    }
}
