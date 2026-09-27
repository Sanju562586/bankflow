package com.bankflow.notification.channel;

import com.bankflow.common.enums.NotificationChannel;
import com.bankflow.notification.exception.DeliveryException;
import com.bankflow.notification.model.NotificationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * SMS Dispatcher via Twilio Sandbox API or high-fidelity simulation.
 */
@Component
public class TwilioSmsSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    @Value("${bankflow.twilio.account-sid:AC_MOCK_TWILIO_ACCOUNT_SID}")
    private String accountSid;

    @Value("${bankflow.twilio.from-number:+14155238886}")
    private String fromNumber;

    @Value("${bankflow.twilio.mock:true}")
    private boolean mockMode;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(NotificationRecord record) {
        log.info("[TWILIO SMS SANDBOX] Dispatching SMS to {}: '{}'", record.getRecipient(), record.getBody());

        // Simulated transient network glitch check for testing retry
        if (record.getRecipient() != null && record.getRecipient().endsWith("9999") && record.getAttemptCount() < 2) {
            log.warn("[TWILIO SMS SANDBOX] Simulated carrier timeout for number ending in 9999");
            throw new DeliveryException("Twilio Sandbox carrier network timeout for " + record.getRecipient());
        }

        // Mock delivery success
        record.setStatus("DELIVERED");
        record.setDeliveredAt(Instant.now());
        log.info("[TWILIO SMS SANDBOX] SMS successfully delivered to {} via fromNumber={}", record.getRecipient(), fromNumber);
    }
}
